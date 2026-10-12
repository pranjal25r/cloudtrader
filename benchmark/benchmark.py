"""
Tiny latency benchmark. Standard library only.

Usage:
    python3 benchmark.py http://localhost:8081/prices/stats
    python3 benchmark.py "http://localhost:8082/portfolio?method=java"

It calls the URL 20 times to warm up (results thrown away), then 200 more times,
and prints the median and p95 response time in milliseconds.

If the answer contains a "calculationMs" field (GET /portfolio does), it also prints
the median and p95 of that, which is the time the server spent adding up the trades.
"""
import json
import statistics
import sys
import time
import urllib.request

WARMUP_CALLS = 20
MEASURED_CALLS = 200


def call(url):
    """Call the URL once. Returns (how long it took in ms, the response body)."""
    start = time.perf_counter()
    with urllib.request.urlopen(url) as response:
        body = response.read()
    elapsed_ms = (time.perf_counter() - start) * 1000
    return elapsed_ms, body


def p95(values):
    """The value that 95% of the calls were faster than."""
    ordered = sorted(values)
    index = int(len(ordered) * 0.95) - 1
    return ordered[index]


def main():
    if len(sys.argv) != 2:
        print("Usage: python3 benchmark.py <url>")
        sys.exit(1)
    url = sys.argv[1]

    for _ in range(WARMUP_CALLS):
        call(url)

    latencies = []       # end-to-end time seen by the caller
    calculations = []    # "calculationMs" reported by the server, if present
    for _ in range(MEASURED_CALLS):
        elapsed_ms, body = call(url)
        latencies.append(elapsed_ms)
        try:
            data = json.loads(body)
            if isinstance(data, dict) and "calculationMs" in data:
                calculations.append(data["calculationMs"])
        except ValueError:
            pass  # not JSON, nothing extra to record

    print(f"URL: {url}")
    print(f"Calls: {MEASURED_CALLS} (after {WARMUP_CALLS} warm-up calls)")
    print(f"End-to-end latency: median {statistics.median(latencies):.2f} ms, p95 {p95(latencies):.2f} ms")
    if calculations:
        print(f"calculationMs:      median {statistics.median(calculations):.2f} ms, p95 {p95(calculations):.2f} ms")


if __name__ == "__main__":
    main()
