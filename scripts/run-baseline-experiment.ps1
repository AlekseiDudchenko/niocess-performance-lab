param(
    [int[]]$Threads = @(100, 200, 500, 1000, 2000, 3000),
    [int[]]$DelaysMs = @(100, 250, 500, 1000, 2000, 3000),
    [int]$Repeats = 3,
    [int]$Loops = 1,
    [int]$CooldownSeconds = 15,
    [string]$JmxPath = ".\client-app-100-threads.jmx",
    [string]$ClientBaseUrl = "http://127.0.0.1:8080",
    [string]$SimulatorBaseUrl = "http://127.0.0.1:8090",
    [string]$OutputRoot = ".\experiments",
    [string]$JMeterPath = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingPath([string]$PathValue) {
    $resolved = Resolve-Path -LiteralPath $PathValue -ErrorAction Stop
    return $resolved.Path
}

function Resolve-JMeter([string]$ExplicitPath) {
    if ($ExplicitPath -and (Test-Path -LiteralPath $ExplicitPath)) {
        return (Resolve-Path -LiteralPath $ExplicitPath).Path
    }

    $command = Get-Command jmeter -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $default = "C:\Users\Admin\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat"
    if (Test-Path -LiteralPath $default) {
        return $default
    }

    throw "JMeter was not found. Pass -JMeterPath or add jmeter to PATH."
}

function Get-RampUp([int]$ThreadCount) {
    return [Math]::Max(1, [int][Math]::Ceiling($ThreadCount * 0.03))
}

function Get-Percentile([double[]]$Values, [double]$Percentile) {
    if (-not $Values -or $Values.Count -eq 0) {
        return 0
    }

    $sorted = $Values | Sort-Object
    $k = ($sorted.Count - 1) * ($Percentile / 100.0)
    $floor = [int][Math]::Floor($k)
    $ceil = [int][Math]::Ceiling($k)
    if ($floor -eq $ceil) {
        return [double]$sorted[$floor]
    }

    return [double]$sorted[$floor] + ([double]$sorted[$ceil] - [double]$sorted[$floor]) * ($k - $floor)
}

function Read-JtlSummary([string]$JtlPath) {
    $rows = Import-Csv -LiteralPath $JtlPath
    $elapsed = @($rows | ForEach-Object { [double]$_.elapsed })
    $errors = @($rows | Where-Object { $_.success -ne "true" }).Count
    $samples = $rows.Count
    $start = ($rows | ForEach-Object { [double]$_.timeStamp } | Measure-Object -Minimum).Minimum
    $end = ($rows | ForEach-Object { [double]$_.timeStamp + [double]$_.elapsed } | Measure-Object -Maximum).Maximum
    $durationSeconds = if ($end -gt $start) { ($end - $start) / 1000.0 } else { 0 }
    $average = if ($samples -gt 0) { ($elapsed | Measure-Object -Average).Average } else { 0 }
    $throughput = if ($durationSeconds -gt 0) { $samples / $durationSeconds } else { 0 }

    return [pscustomobject]@{
        samples = $samples
        errors = $errors
        errorRatePercent = if ($samples -gt 0) { ($errors / $samples) * 100.0 } else { 0 }
        avgMs = $average
        medianMs = Get-Percentile $elapsed 50
        p95Ms = Get-Percentile $elapsed 95
        p99Ms = Get-Percentile $elapsed 99
        throughput = $throughput
        durationSeconds = $durationSeconds
    }
}

function Invoke-JsonPost([string]$Uri) {
    try {
        Invoke-RestMethod -Method Post -Uri $Uri -TimeoutSec 30 | Out-Null
    } catch {
        throw "POST $Uri failed: $($_.Exception.Message)"
    }
}

function Invoke-JsonGet([string]$Uri) {
    try {
        return Invoke-RestMethod -Method Get -Uri $Uri -TimeoutSec 30
    } catch {
        throw "GET $Uri failed: $($_.Exception.Message)"
    }
}

$root = (Resolve-Path -LiteralPath ".").Path
$jmx = Resolve-ExistingPath $JmxPath
$jmeter = Resolve-JMeter $JMeterPath
$startedAt = Get-Date
$experimentId = "baseline-" + $startedAt.ToString("yyyyMMdd-HHmmss")
$outputRootPath = if (Test-Path -LiteralPath $OutputRoot) {
    (Resolve-Path -LiteralPath $OutputRoot).Path
} else {
    Join-Path $root $OutputRoot
}
if (-not $DryRun -and -not (Test-Path -LiteralPath $OutputRoot)) {
    New-Item -ItemType Directory -Path $OutputRoot | Out-Null
    $outputRootPath = (Resolve-Path -LiteralPath $OutputRoot).Path
}
$experimentDir = Join-Path $outputRootPath $experimentId
$runsDir = Join-Path $experimentDir "runs"
$summariesDir = Join-Path $experimentDir "summaries"
$graphsDir = Join-Path $experimentDir "graphs"

if ($DryRun) {
    Write-Host "Dry run. No files will be written and JMeter will not be started."
} else {
    New-Item -ItemType Directory -Path $runsDir, $summariesDir, $graphsDir | Out-Null
}

$plannedRuns = foreach ($delay in $DelaysMs) {
    foreach ($thread in $Threads) {
        foreach ($repeat in 1..$Repeats) {
            [pscustomobject]@{
                threads = $thread
                delayMs = $delay
                rampup = Get-RampUp $thread
                repeat = $repeat
            }
        }
    }
}

$plan = [pscustomobject]@{
    experimentId = $experimentId
    startedAt = $startedAt.ToString("o")
    root = $root
    jmxPath = $jmx
    jmeterPath = $jmeter
    clientBaseUrl = $ClientBaseUrl
    simulatorBaseUrl = $SimulatorBaseUrl
    threads = $Threads
    delaysMs = $DelaysMs
    repeats = $Repeats
    loops = $Loops
    cooldownSeconds = $CooldownSeconds
    plannedRuns = $plannedRuns.Count
}

if (-not $DryRun) {
    $plan | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $experimentDir "experiment-plan.json") -Encoding UTF8
}

Write-Host "Experiment: $experimentId"
Write-Host "Planned runs: $($plannedRuns.Count)"
Write-Host "JMX: $jmx"
Write-Host "JMeter: $jmeter"

if ($DryRun) {
    $plannedRuns | Format-Table -AutoSize
    return
}

Invoke-JsonGet "$ClientBaseUrl/actuator/health" | Out-Null
Invoke-JsonGet "$SimulatorBaseUrl/health" | Out-Null

$allSummaries = @()
$failureStreakByZone = @{}

foreach ($run in $plannedRuns) {
    $zone = "t$($run.threads)-d$($run.delayMs)"
    if ($failureStreakByZone.ContainsKey($zone) -and $failureStreakByZone[$zone] -ge 2) {
        Write-Host "Skipping $zone repeat $($run.repeat): previous two repeats exceeded stop criteria."
        continue
    }

    $runId = "$zone-r$($run.repeat)"
    $runDir = Join-Path $runsDir $runId
    $reportDir = Join-Path $runDir "report"
    $jtlPath = Join-Path $runDir "results.jtl"
    $metadataPath = Join-Path $runDir "metadata.json"
    $metricsPath = Join-Path $runDir "simulator-metrics.json"

    New-Item -ItemType Directory -Path $runDir | Out-Null

    $runStartedAt = Get-Date
    Write-Host "Running $runId threads=$($run.threads) delayMs=$($run.delayMs) rampup=$($run.rampup)"
    Invoke-JsonPost "$SimulatorBaseUrl/metrics/reset"

    $args = @(
        "-n",
        "-t", $jmx,
        "-Jthreads=$($run.threads)",
        "-JdelayMs=$($run.delayMs)",
        "-Jrampup=$($run.rampup)",
        "-Jloops=$Loops",
        "-l", $jtlPath,
        "-e",
        "-o", $reportDir
    )

    & $jmeter @args
    $exitCode = $LASTEXITCODE
    $runFinishedAt = Get-Date

    $simulatorMetrics = Invoke-JsonGet "$SimulatorBaseUrl/metrics"
    $simulatorMetrics | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $metricsPath -Encoding UTF8

    $summary = Read-JtlSummary $jtlPath
    $summaryRow = [pscustomobject]@{
        experimentId = $experimentId
        runId = $runId
        threads = $run.threads
        delayMs = $run.delayMs
        rampup = $run.rampup
        repeat = $run.repeat
        loops = $Loops
        startedAt = $runStartedAt.ToString("o")
        finishedAt = $runFinishedAt.ToString("o")
        exitCode = $exitCode
        samples = $summary.samples
        errors = $summary.errors
        errorRatePercent = [Math]::Round($summary.errorRatePercent, 4)
        avgMs = [Math]::Round($summary.avgMs, 4)
        medianMs = [Math]::Round($summary.medianMs, 4)
        p95Ms = [Math]::Round($summary.p95Ms, 4)
        p99Ms = [Math]::Round($summary.p99Ms, 4)
        throughput = [Math]::Round($summary.throughput, 4)
        durationSeconds = [Math]::Round($summary.durationSeconds, 4)
        simulatorTotalRequests = $simulatorMetrics.totalRequests
        simulatorMaxConcurrentRequests = $simulatorMetrics.maxConcurrentRequests
        simulatorErrorCount = $simulatorMetrics.errorCount
        jtlPath = $jtlPath
        reportPath = $reportDir
        simulatorMetricsPath = $metricsPath
    }

    $summaryRow | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $metadataPath -Encoding UTF8
    $allSummaries += $summaryRow
    $allSummaries | Export-Csv -LiteralPath (Join-Path $summariesDir "run-summary.csv") -NoTypeInformation -Encoding UTF8

    $failedStopCriteria = ($summaryRow.errorRatePercent -gt 10.0) -or ($summaryRow.avgMs -gt 60000.0) -or ($exitCode -ne 0)
    if ($failedStopCriteria) {
        $failureStreakByZone[$zone] = if ($failureStreakByZone.ContainsKey($zone)) { $failureStreakByZone[$zone] + 1 } else { 1 }
    } else {
        $failureStreakByZone[$zone] = 0
    }

    Write-Host ("Done {0}: samples={1} errors={2} errorRate={3}% avg={4}ms p95={5}ms p99={6}ms" -f `
        $runId, $summaryRow.samples, $summaryRow.errors, $summaryRow.errorRatePercent, $summaryRow.avgMs, $summaryRow.p95Ms, $summaryRow.p99Ms)

    if ($CooldownSeconds -gt 0) {
        Start-Sleep -Seconds $CooldownSeconds
    }
}

Write-Host "Experiment complete: $experimentDir"
Write-Host "Analyze with: python .\scripts\analyze-baseline-experiment.py --experiment-dir `"$experimentDir`""
