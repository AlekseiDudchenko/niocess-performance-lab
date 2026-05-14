const chartDefaults = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  plugins: {
    legend: {
      labels: { color: '#dbeafe', usePointStyle: true, boxWidth: 8 },
    },
    tooltip: {
      backgroundColor: 'rgba(7, 17, 31, 0.94)',
      borderColor: 'rgba(255,255,255,0.18)',
      borderWidth: 1,
      titleColor: '#ffffff',
      bodyColor: '#dbeafe',
      padding: 12,
    },
  },
  scales: {
    x: {
      grid: { color: 'rgba(255,255,255,0.08)' },
      ticks: { color: '#9db0cc' },
      title: { display: true, text: 'Concurrent users per scenario', color: '#9db0cc' },
    },
    y: {
      beginAtZero: true,
      grid: { color: 'rgba(255,255,255,0.08)' },
      ticks: { color: '#9db0cc' },
    },
  },
};

const variantStyles = {
  jdbc: {
    borderColor: '#fca5a5',
    backgroundColor: 'rgba(252, 165, 165, 0.16)',
  },
  pgasync: {
    borderColor: '#7dd3fc',
    backgroundColor: 'rgba(125, 211, 252, 0.16)',
  },
};

const tailMetricStyles = {
  p95: { borderDash: [], pointStyle: 'circle' },
  p99: { borderDash: [7, 5], pointStyle: 'triangle' },
  max: { borderDash: [2, 4], pointStyle: 'rectRot' },
};

let tailChart;
let benchmarkData;

async function loadData() {
  const response = await fetch('./data/benchmark-results.json');
  if (!response.ok) {
    throw new Error(`Could not load benchmark data: ${response.status}`);
  }
  return response.json();
}

function labelsFrom(data) {
  return data.results.map((point) => point.concurrentUsers);
}

function datasetFor(data, variant, metric, label, options = {}) {
  return {
    label,
    data: data.results.map((point) => point.variants[variant][metric]),
    borderWidth: 3,
    tension: 0.28,
    spanGaps: false,
    pointRadius: 4,
    pointHoverRadius: 7,
    ...variantStyles[variant],
    ...options,
  };
}

function createLineChart(canvasId, data, metric, title) {
  const ctx = document.getElementById(canvasId);
  return new Chart(ctx, {
    type: 'line',
    data: {
      labels: labelsFrom(data),
      datasets: [
        datasetFor(data, 'jdbc', metric, 'JDBC baseline'),
        datasetFor(data, 'pgasync', metric, 'pgasync non-blocking'),
      ],
    },
    options: {
      ...chartDefaults,
      scales: {
        ...chartDefaults.scales,
        y: {
          ...chartDefaults.scales.y,
          title: { display: true, text: title, color: '#9db0cc' },
        },
      },
    },
  });
}

function buildTailDatasets(data) {
  const checkedMetrics = [...document.querySelectorAll('[data-series]:checked')]
    .map((input) => input.dataset.series);

  return checkedMetrics.flatMap((metric) => [
    datasetFor(data, 'jdbc', metric, `JDBC ${metric}`, tailMetricStyles[metric]),
    datasetFor(data, 'pgasync', metric, `pgasync ${metric}`, tailMetricStyles[metric]),
  ]);
}

function createTailLatencyChart(data) {
  const ctx = document.getElementById('tailLatencyChart');
  return new Chart(ctx, {
    type: 'line',
    data: {
      labels: labelsFrom(data),
      datasets: buildTailDatasets(data),
    },
    options: {
      ...chartDefaults,
      scales: {
        ...chartDefaults.scales,
        y: {
          ...chartDefaults.scales.y,
          title: { display: true, text: 'Latency, ms', color: '#9db0cc' },
        },
      },
    },
  });
}

function updateTailChart() {
  tailChart.data.datasets = buildTailDatasets(benchmarkData);
  tailChart.update();
}

function formatNumber(value) {
  if (value === null || value === undefined) return '—';
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 1 }).format(value);
}

function formatMetric(value, unit) {
  if (value === null || value === undefined) return '—';
  return `${formatNumber(value)} ${unit}`;
}

function appendCell(row, text) {
  const cell = document.createElement('td');
  cell.textContent = text;
  row.appendChild(cell);
}

function fillTable(data) {
  const tbody = document.getElementById('dataTableBody');
  tbody.replaceChildren();

  data.results.forEach((point) => {
    Object.values(point.variants).forEach((variant) => {
      const row = document.createElement('tr');
      appendCell(row, String(point.concurrentUsers));
      appendCell(row, variant.label);
      appendCell(row, formatMetric(variant.throughput, 'req/s'));
      appendCell(row, formatMetric(variant.avgLatency, 'ms'));
      appendCell(row, formatMetric(variant.p95, 'ms'));
      appendCell(row, formatMetric(variant.p99, 'ms'));
      appendCell(row, formatMetric(variant.max, 'ms'));
      tbody.appendChild(row);
    });
  });
}

function fillSummary(data) {
  const last = data.results[data.results.length - 1];
  const jdbc = last.variants.jdbc;
  const pgasync = last.variants.pgasync;

  const p95Reduction = Math.round(((jdbc.p95 - pgasync.p95) / jdbc.p95) * 100);
  const throughputGain = Math.round(((pgasync.throughput - jdbc.throughput) / jdbc.throughput) * 1000) / 10;

  document.getElementById('bestP95').textContent = `${p95Reduction}% lower p95`;
  document.getElementById('bestThroughput').textContent = `${throughputGain}% higher TPS`;
}

function setupPanelToggles() {
  document.querySelectorAll('.metric-toggle').forEach((button) => {
    button.addEventListener('click', () => {
      const target = document.getElementById(button.dataset.target);
      const isActive = button.classList.toggle('active');
      button.setAttribute('aria-pressed', String(isActive));
      target.classList.toggle('hidden', !isActive);
    });
  });
}

function setupSeriesToggles() {
  document.querySelectorAll('[data-series]').forEach((checkbox) => {
    checkbox.addEventListener('change', updateTailChart);
  });
}

function showDashboardError(error) {
  const dashboard = document.getElementById('dashboard');
  const section = document.createElement('section');
  section.className = 'panel wide';

  const heading = document.createElement('h2');
  heading.textContent = 'Could not load dashboard';

  const message = document.createElement('p');
  message.textContent = error.message;

  section.append(heading, message);
  dashboard.replaceChildren(section);
}

async function init() {
  try {
    if (!window.Chart) {
      throw new Error('Chart.js could not be loaded. Check your internet connection or CDN access.');
    }

    benchmarkData = await loadData();
    createLineChart('avgLatencyChart', benchmarkData, 'avgLatency', 'Average latency, ms');
    createLineChart('throughputChart', benchmarkData, 'throughput', 'Requests per second');
    tailChart = createTailLatencyChart(benchmarkData);
    fillTable(benchmarkData);
    fillSummary(benchmarkData);
    setupPanelToggles();
    setupSeriesToggles();
  } catch (error) {
    console.error(error);
    showDashboardError(error);
  }
}

init();
