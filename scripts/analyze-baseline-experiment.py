#!/usr/bin/env python3
import argparse
import csv
import json
import math
from collections import defaultdict
from pathlib import Path
from statistics import mean, median


def percentile(values, p):
    values = sorted(values)
    if not values:
        return 0.0
    k = (len(values) - 1) * (p / 100.0)
    floor = math.floor(k)
    ceil = math.ceil(k)
    if floor == ceil:
        return float(values[floor])
    return values[floor] + (values[ceil] - values[floor]) * (k - floor)


def read_csv(path):
    with path.open(newline="", encoding="utf-8-sig") as fh:
        return list(csv.DictReader(fh))


def read_jtl(path):
    rows = read_csv(path)
    values = [int(row["elapsed"]) for row in rows]
    errors = sum(1 for row in rows if row.get("success") != "true")
    timestamps = [float(row["timeStamp"]) for row in rows]
    ends = [float(row["timeStamp"]) + float(row["elapsed"]) for row in rows]
    duration = ((max(ends) - min(timestamps)) / 1000.0) if rows else 0.0
    return {
        "samples": len(rows),
        "errors": errors,
        "error_rate_percent": (errors / len(rows) * 100.0) if rows else 0.0,
        "avg_ms": mean(values) if values else 0.0,
        "median_ms": median(values) if values else 0.0,
        "p95_ms": percentile(values, 95),
        "p99_ms": percentile(values, 99),
        "throughput": (len(rows) / duration) if duration > 0 else 0.0,
        "duration_seconds": duration,
        "elapsed_values": values,
    }


def load_run_metadata(experiment_dir):
    summary_path = experiment_dir / "summaries" / "run-summary.csv"
    if summary_path.exists():
        rows = read_csv(summary_path)
        return rows

    rows = []
    for metadata_path in sorted((experiment_dir / "runs").glob("*/metadata.json")):
        rows.append(json.loads(metadata_path.read_text(encoding="utf-8-sig")))
    return rows


def aggregate(rows):
    grouped = defaultdict(list)
    for row in rows:
        grouped[(int(row["threads"]), int(row["delayMs"]))].append(row)

    output = []
    for (threads, delay_ms), items in sorted(grouped.items()):
        def values(field):
            return [float(item[field]) for item in items]

        output.append({
            "threads": threads,
            "delayMs": delay_ms,
            "runs": len(items),
            "samples": sum(int(float(item["samples"])) for item in items),
            "errors": sum(int(float(item["errors"])) for item in items),
            "error_rate_percent_avg": mean(values("errorRatePercent")),
            "avg_ms_median": median(values("avgMs")),
            "p95_ms_median": median(values("p95Ms")),
            "p99_ms_median": median(values("p99Ms")),
            "throughput_median": median(values("throughput")),
            "simulator_max_concurrent_median": median(values("simulatorMaxConcurrentRequests")),
            "simulator_total_requests_median": median(values("simulatorTotalRequests")),
        })
    return output


def write_csv(path, rows, fieldnames):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def svg_line_chart(path, title, x_values, series, y_label):
    width, height = 1040, 640
    left, right, top, bottom = 95, 210, 70, 90
    plot_w = width - left - right
    plot_h = height - top - bottom
    all_y = [value for item in series for value in item["values"] if value is not None]
    y_max = max(all_y) * 1.15 if all_y else 1.0
    y_min = 0.0
    if y_max <= y_min:
        y_max = y_min + 1.0

    def x_pos(x):
        if len(x_values) == 1:
            return left + plot_w / 2
        return left + (x_values.index(x) * (plot_w / (len(x_values) - 1)))

    def y_pos(y):
        return top + plot_h - ((y - y_min) / (y_max - y_min) * plot_h)

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
        '<rect width="100%" height="100%" fill="#ffffff"/>',
        f'<text x="{width/2}" y="34" text-anchor="middle" font-family="Arial" font-size="24" font-weight="700" fill="#222">{title}</text>',
        f'<text x="24" y="{height/2}" transform="rotate(-90 24 {height/2})" text-anchor="middle" font-family="Arial" font-size="14" fill="#444">{y_label}</text>',
    ]

    for t in range(6):
        value = y_min + (y_max - y_min) * t / 5
        y = y_pos(value)
        parts.append(f'<line x1="{left}" y1="{y:.1f}" x2="{width-right}" y2="{y:.1f}" stroke="#e5e7eb"/>')
        parts.append(f'<text x="{left-12}" y="{y+5:.1f}" text-anchor="end" font-family="Arial" font-size="13" fill="#555">{value:.0f}</text>')

    parts.append(f'<line x1="{left}" y1="{top}" x2="{left}" y2="{height-bottom}" stroke="#333"/>')
    parts.append(f'<line x1="{left}" y1="{height-bottom}" x2="{width-right}" y2="{height-bottom}" stroke="#333"/>')

    for x in x_values:
        parts.append(f'<text x="{x_pos(x):.1f}" y="{height-bottom+32}" text-anchor="middle" font-family="Arial" font-size="13" fill="#333">{x}</text>')
    parts.append(f'<text x="{left + plot_w/2}" y="{height-20}" text-anchor="middle" font-family="Arial" font-size="14" fill="#444">Threads</text>')

    for idx, item in enumerate(series):
        color = item["color"]
        points = " ".join(
            f'{x_pos(x):.1f},{y_pos(value):.1f}'
            for x, value in zip(x_values, item["values"])
            if value is not None
        )
        parts.append(f'<polyline points="{points}" fill="none" stroke="{color}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>')
        for x, value in zip(x_values, item["values"]):
            if value is None:
                continue
            parts.append(f'<circle cx="{x_pos(x):.1f}" cy="{y_pos(value):.1f}" r="5" fill="{color}"/>')
        legend_y = top + idx * 24
        parts.append(f'<line x1="{width-right+28}" y1="{legend_y}" x2="{width-right+58}" y2="{legend_y}" stroke="{color}" stroke-width="3"/>')
        parts.append(f'<text x="{width-right+68}" y="{legend_y+5}" font-family="Arial" font-size="13" fill="#333">{item["name"]}</text>')

    parts.append("</svg>")
    path.write_text("\n".join(parts), encoding="utf-8")


def svg_boxplot(path, title, boxes):
    width, height = 1040, 650
    left, right, top, bottom = 105, 50, 75, 115
    plot_w = width - left - right
    plot_h = height - top - bottom
    all_y = []
    for box in boxes:
        all_y.extend([box["min"], box["max"], box["q1"], box["median"], box["q3"]])
    y_min = max(0, min(all_y) - 100) if all_y else 0
    y_max = max(all_y) + 250 if all_y else 1

    def y_pos(v):
        return top + plot_h - ((v - y_min) / (y_max - y_min) * plot_h)

    def x_pos(i):
        return left + (i + 0.5) * (plot_w / len(boxes))

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
        '<rect width="100%" height="100%" fill="#ffffff"/>',
        f'<text x="{width/2}" y="38" text-anchor="middle" font-family="Arial" font-size="24" font-weight="700" fill="#222">{title}</text>',
        f'<text x="26" y="{height/2}" transform="rotate(-90 26 {height/2})" text-anchor="middle" font-family="Arial" font-size="14" fill="#444">Elapsed time, ms</text>',
    ]
    for t in range(6):
        value = y_min + (y_max - y_min) * t / 5
        y = y_pos(value)
        parts.append(f'<line x1="{left}" y1="{y:.1f}" x2="{width-right}" y2="{y:.1f}" stroke="#e5e7eb"/>')
        parts.append(f'<text x="{left-12}" y="{y+5:.1f}" text-anchor="end" font-family="Arial" font-size="13" fill="#555">{value:.0f}</text>')
    parts.append(f'<line x1="{left}" y1="{top}" x2="{left}" y2="{height-bottom}" stroke="#333"/>')
    parts.append(f'<line x1="{left}" y1="{height-bottom}" x2="{width-right}" y2="{height-bottom}" stroke="#333"/>')

    colors = ["#1f77b4", "#2ca02c", "#d62728", "#9467bd"]
    box_w = min(115, plot_w / len(boxes) * 0.55)
    for i, box in enumerate(boxes):
        x = x_pos(i)
        color = colors[i % len(colors)]
        q1_y, q3_y, med_y = y_pos(box["q1"]), y_pos(box["q3"]), y_pos(box["median"])
        low_y, high_y = y_pos(box["whisker_low"]), y_pos(box["whisker_high"])
        box_top, box_h = min(q1_y, q3_y), max(abs(q1_y - q3_y), 2)
        parts.append(f'<line x1="{x:.1f}" y1="{high_y:.1f}" x2="{x:.1f}" y2="{box_top:.1f}" stroke="{color}" stroke-width="3"/>')
        parts.append(f'<line x1="{x:.1f}" y1="{q1_y:.1f}" x2="{x:.1f}" y2="{low_y:.1f}" stroke="{color}" stroke-width="3"/>')
        parts.append(f'<line x1="{x-box_w/3:.1f}" y1="{high_y:.1f}" x2="{x+box_w/3:.1f}" y2="{high_y:.1f}" stroke="{color}" stroke-width="3"/>')
        parts.append(f'<line x1="{x-box_w/3:.1f}" y1="{low_y:.1f}" x2="{x+box_w/3:.1f}" y2="{low_y:.1f}" stroke="{color}" stroke-width="3"/>')
        parts.append(f'<rect x="{x-box_w/2:.1f}" y="{box_top:.1f}" width="{box_w:.1f}" height="{box_h:.1f}" fill="{color}" fill-opacity="0.18" stroke="{color}" stroke-width="3"/>')
        parts.append(f'<line x1="{x-box_w/2:.1f}" y1="{med_y:.1f}" x2="{x+box_w/2:.1f}" y2="{med_y:.1f}" stroke="{color}" stroke-width="4"/>')
        for j, value in enumerate(box["outliers"][:120]):
            jitter = ((j % 9) - 4) * 3
            parts.append(f'<circle cx="{x+jitter:.1f}" cy="{y_pos(value):.1f}" r="2.5" fill="{color}" fill-opacity="0.45"/>')
        parts.append(f'<text x="{x:.1f}" y="{height-bottom+32}" text-anchor="middle" font-family="Arial" font-size="12" fill="#333">{box["label"]}</text>')
        parts.append(f'<text x="{x:.1f}" y="{height-bottom+52}" text-anchor="middle" font-family="Arial" font-size="11" fill="#666">n={box["samples"]}</text>')
    parts.append("</svg>")
    path.write_text("\n".join(parts), encoding="utf-8")


def build_series(aggregates, field):
    threads = sorted({row["threads"] for row in aggregates})
    delays = sorted({row["delayMs"] for row in aggregates})
    palette = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b"]
    series = []
    by_key = {(row["threads"], row["delayMs"]): row for row in aggregates}
    for idx, delay in enumerate(delays):
        series.append({
            "name": f"delay {delay} ms",
            "color": palette[idx % len(palette)],
            "values": [by_key.get((thread, delay), {}).get(field) for thread in threads],
        })
    return threads, series


def collect_box_values(experiment_dir, summary_rows, selected_delays):
    selected_threads = [200, 2000, 1000, 3000]
    boxes = []
    for delay in selected_delays:
        for thread in selected_threads:
            candidates = [
                row for row in summary_rows
                if int(row["threads"]) == thread and int(row["delayMs"]) == delay
            ]
            if not candidates:
                continue
            values = []
            for row in candidates:
                jtl_path = Path(row["jtlPath"])
                if not jtl_path.is_absolute():
                    jtl_path = experiment_dir / jtl_path
                values.extend(read_jtl(jtl_path)["elapsed_values"])
            if not values:
                continue
            sorted_values = sorted(values)
            q1 = percentile(sorted_values, 25)
            q3 = percentile(sorted_values, 75)
            iqr = q3 - q1
            low_fence = q1 - 1.5 * iqr
            high_fence = q3 + 1.5 * iqr
            non_outliers = [v for v in sorted_values if low_fence <= v <= high_fence]
            boxes.append({
                "label": f"t{thread} d{delay}",
                "samples": len(sorted_values),
                "min": sorted_values[0],
                "max": sorted_values[-1],
                "q1": q1,
                "median": percentile(sorted_values, 50),
                "q3": q3,
                "whisker_low": min(non_outliers) if non_outliers else sorted_values[0],
                "whisker_high": max(non_outliers) if non_outliers else sorted_values[-1],
                "outliers": [v for v in sorted_values if v < low_fence or v > high_fence],
            })
    return boxes


def main():
    parser = argparse.ArgumentParser(description="Analyze Niocess baseline experiment JMeter outputs.")
    parser.add_argument("--experiment-dir", required=True, type=Path)
    args = parser.parse_args()

    experiment_dir = args.experiment_dir.resolve()
    graphs_dir = experiment_dir / "graphs"
    summaries_dir = experiment_dir / "summaries"
    graphs_dir.mkdir(parents=True, exist_ok=True)
    summaries_dir.mkdir(parents=True, exist_ok=True)

    rows = load_run_metadata(experiment_dir)
    if not rows:
        raise SystemExit(f"No run metadata found in {experiment_dir}")

    aggregates = aggregate(rows)
    write_csv(
        summaries_dir / "aggregate-summary.csv",
        aggregates,
        [
            "threads", "delayMs", "runs", "samples", "errors",
            "error_rate_percent_avg", "avg_ms_median", "p95_ms_median",
            "p99_ms_median", "throughput_median",
            "simulator_max_concurrent_median", "simulator_total_requests_median",
        ],
    )

    chart_specs = [
        ("avg_ms_median", "Average response time vs threads", "Median average response time, ms", "avg-response-time.svg"),
        ("p95_ms_median", "p95 response time vs threads", "Median p95 response time, ms", "p95-response-time.svg"),
        ("p99_ms_median", "p99 response time vs threads", "Median p99 response time, ms", "p99-response-time.svg"),
        ("error_rate_percent_avg", "Error rate vs threads", "Average error rate, %", "error-rate.svg"),
        ("throughput_median", "Throughput vs threads", "Median throughput, req/s", "throughput.svg"),
        ("simulator_max_concurrent_median", "Simulator max concurrency vs threads", "Median max concurrent requests", "simulator-max-concurrency.svg"),
    ]
    for field, title, y_label, filename in chart_specs:
        threads, series = build_series(aggregates, field)
        svg_line_chart(graphs_dir / filename, title, threads, series, y_label)

    delays = sorted({int(row["delayMs"]) for row in rows})
    selected_delays = [delay for delay in [1000, 3000] if delay in delays]
    boxes = collect_box_values(experiment_dir, rows, selected_delays)
    if boxes:
        svg_boxplot(graphs_dir / "selected-boxplots.svg", "Selected response time box plots", boxes)

    print(f"Run summary: {summaries_dir / 'run-summary.csv'}")
    print(f"Aggregate summary: {summaries_dir / 'aggregate-summary.csv'}")
    print(f"Graphs: {graphs_dir}")


if __name__ == "__main__":
    main()
