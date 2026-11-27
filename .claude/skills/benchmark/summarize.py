import json
import math
import sys
from collections import defaultdict


def load(path):
    with open(path) as file:
        results = json.load(file)
    rows = {}
    jdks = {result["vmVersion"] for result in results}
    for result in results:
        suite, method = result["benchmark"].split(".")[-2:]
        params = ",".join(f"{k}={v}" for k, v in result.get("params", {}).items())
        metric = result["primaryMetric"]
        rows[(suite, method, params)] = (
            float(metric["score"]),
            float(metric["scoreError"]),
            metric["scoreUnit"],
        )
    return rows, jdks


def measured(score, error):
    if math.isnan(error):
        return f"{score:.1f}"
    return f"{score:.1f} ± {error:.1f}"


def cell(current, baseline):
    score, error, _ = current
    text = measured(score, error)
    if baseline is None:
        return text
    base, base_error, _ = baseline
    mark = "*" if abs(score - base) > error + base_error else ""
    return f"{measured(base, base_error)} → {text} ({score / base:.2f}{mark})"


def main(paths):
    current, jdks = load(paths[-1])
    baseline = None
    if len(paths) == 2:
        baseline, base_jdks = load(paths[0])
        if base_jdks != jdks:
            print(f"JDK differs: baseline {', '.join(sorted(base_jdks))}\n")
    print(f"JDK: {', '.join(sorted(jdks))}\n")
    suites = defaultdict(lambda: ({}, {}))
    for suite, method, params in current:
        methods, columns = suites[suite]
        methods[method] = None
        columns[params] = None
    for suite, (methods, columns) in suites.items():
        unit = current[(suite, next(iter(methods)), next(iter(columns)))][2]
        print(f"### {suite} ({unit})\n")
        print("| Method | " + " | ".join(columns) + " |")
        print("|---" * (len(columns) + 1) + "|")
        for method in methods:
            cells = []
            for params in columns:
                key = (suite, method, params)
                if key not in current:
                    cells.append("-")
                    continue
                cells.append(cell(current[key], baseline.get(key) if baseline else None))
            print(f"| {method} | " + " | ".join(cells) + " |")
        print()


if len(sys.argv) not in (2, 3):
    sys.exit("usage: summarize.py [baseline.json] current.json")
main(sys.argv[1:])
