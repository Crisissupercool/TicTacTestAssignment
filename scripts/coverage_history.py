#!/usr/bin/env python3
"""Haengt die aktuelle Coverage an die Historie an und rendert die Trend-Seite.

    python3 scripts/coverage_history.py REPORT.xml HISTORY.csv OUT.html COMMIT
"""

import csv
import datetime as dt
import os
import sys
import xml.etree.ElementTree as ET


def read_coverage(path):
    """Instruction-Coverage in Prozent aus den Gesamtwerten des JaCoCo-Reports."""
    root = ET.parse(path).getroot()
    for counter in root.findall("counter"):
        if counter.get("type") == "INSTRUCTION":
            covered = int(counter.get("covered"))
            total = covered + int(counter.get("missed"))
            return round(100.0 * covered / total, 1) if total else 0.0
    raise SystemExit("Kein INSTRUCTION-Counter im Report gefunden")


def load(path):
    if not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8") as handle:
        return [r for r in csv.DictReader(handle) if r.get("commit")]


def save(path, rows):
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, ["date", "commit", "coverage"], lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def chart(rows):
    """Liniendiagramm als Inline-SVG - kein JavaScript, kein CDN."""
    width, height, pad = 760, 300, 45
    plot_w, plot_h = width - 2 * pad, height - 2 * pad
    values = [float(r["coverage"]) for r in rows]
    low = max(0, int(min(values) / 10) * 10 - 10)
    span = 100 - low

    def x(i):
        return pad + (plot_w / 2 if len(rows) == 1 else plot_w * i / (len(rows) - 1))

    def y(v):
        return pad + plot_h * (1 - (v - low) / span)

    svg = [f'<svg viewBox="0 0 {width} {height}" class="chart">']
    for tick in range(low, 101, 10):
        svg.append(f'<line class="grid" x1="{pad}" y1="{y(tick):.0f}" x2="{pad + plot_w}" y2="{y(tick):.0f}"/>')
        svg.append(f'<text class="ax" x="{pad - 8}" y="{y(tick) + 4:.0f}" text-anchor="end">{tick}%</text>')

    points = [(x(i), y(v)) for i, v in enumerate(values)]
    if len(points) > 1:
        svg.append('<polyline class="line" points="%s"/>'
                   % " ".join(f"{px:.0f},{py:.0f}" for px, py in points))
    for px, py in points:
        svg.append(f'<circle class="dot" cx="{px:.0f}" cy="{py:.0f}" r="3"/>')

    every = max(1, len(rows) // 6)
    for i, row in enumerate(rows):
        if i % every == 0 or i == len(rows) - 1:
            svg.append(f'<text class="ax" x="{x(i):.0f}" y="{height - 15}" '
                       f'text-anchor="middle">{row["date"][:10]}</text>')
    svg.append("</svg>")
    return "".join(svg)


def page(rows):
    table = "".join(
        f'<tr><td>{r["date"]}</td><td><code>{r["commit"][:7]}</code></td>'
        f'<td>{r["coverage"]} %</td></tr>' for r in reversed(rows[-15:]))
    return f"""<!DOCTYPE html>
<html lang="de"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Coverage Trend</title>
<style>
body {{ font-family: system-ui, sans-serif; max-width: 820px; margin: 2rem auto; padding: 0 1rem; }}
.chart {{ width: 100%; height: auto; }}
.grid {{ stroke: #ddd; }}
.ax {{ fill: #666; font-size: 12px; }}
.line {{ fill: none; stroke: #1f6feb; stroke-width: 2; }}
.dot {{ fill: #fff; stroke: #1f6feb; stroke-width: 2; }}
.now {{ font-size: 2rem; font-weight: 600; }}
table {{ border-collapse: collapse; width: 100%; }}
td, th {{ text-align: left; padding: 6px 10px; border-bottom: 1px solid #ddd; }}
</style></head>
<body>
<h1>Test Coverage auf main</h1>
<p class="now">{rows[-1]["coverage"]} %</p>
{chart(rows)}
<p><a href="coverage/index.html">JaCoCo-Report</a> &middot;
   <a href="coverage-history.csv">Rohdaten (CSV)</a></p>
<h2>Messwerte</h2>
<table><tr><th>Zeitpunkt</th><th>Commit</th><th>Coverage</th></tr>{table}</table>
</body></html>
"""


def main():
    if len(sys.argv) != 5:
        raise SystemExit(__doc__)
    report, history, out, commit = sys.argv[1:]

    entry = {
        "date": dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d %H:%M"),
        "commit": commit,
        "coverage": f"{read_coverage(report):.1f}",
    }

    rows = [r for r in load(history) if r["commit"] != commit]  # Re-Run: nicht doppeln
    rows.append(entry)
    save(history, rows)

    with open(out, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(page(rows))

    print(f"{commit[:7]}: {entry['coverage']} % - Historie hat {len(rows)} Eintraege")


if __name__ == "__main__":
    main()
