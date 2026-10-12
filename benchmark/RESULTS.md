# Benchmark results

Every number here came from a real run on 12 October 2026, using the scripts in this folder.
Nothing is estimated. Re-run the steps in the README to get numbers for your own machine.

## Machine and setup

| | |
|---|---|
| Laptop | Apple M5, 10 CPU cores, 16 GB RAM, macOS 27.0.1 |
| Java | OpenJDK 21.0.11 (Homebrew). Both services ran directly on the laptop with `java -jar` |
| MySQL | 8.4.11, running in a Docker container (`mysql:8.4`), published on port 3307 |
| Docker | Docker Desktop, engine 29.6.2. Its Linux VM had 10 CPUs and about 7.75 GB RAM |
| Python | 3.9 (standard library only) |
| Method | `benchmark.py`: 20 warm-up calls, then 200 measured calls, one at a time |

MySQL ran inside Docker Desktop's VM, so each query crosses from the laptop into the VM.
A MySQL installed directly on the laptop would probably give different numbers.

## Experiment A: rolling retention and `GET /prices/stats`

`/prices/stats` reads every row of `price_history`, so it slows down as the table grows.

| Retention | Rows in `price_history` during the run | Median | p95 |
|---|---|---|---|
| Off (`RETENTION_ENABLED=false`) | 1,000,007 to 1,000,460 | 405.78 ms | 424.71 ms |
| On (keep newest 50,000) | 50,015 to 50,045 | 20.87 ms | 46.73 ms |

- Median latency went down by **94.9%** (405.78 ms to 20.87 ms, about 19 times faster).
- p95 latency went down by 89.0% (424.71 ms to 46.73 ms).
- The retention job removed 950,460 rows in one pass, as logged by market-service.

The row counts move a little during each run because market-service keeps adding 5 rows per second.

## Experiment B: P&L calculation, Java loop vs SQL `GROUP BY`

100,000 rows in `trades` (75,000 BUY, 25,000 SELL, 20,000 per stock).
`calculationMs` is the time the server reports for adding up the trades.
End-to-end is the full HTTP call, which also includes asking market-service for prices.

| Run | Method | calculationMs median | calculationMs p95 | End-to-end median | End-to-end p95 |
|---|---|---|---|---|---|
| 1 | `java` | 62.18 ms | 66.40 ms | 65.36 ms | 69.44 ms |
| 1 | `sql` | 41.02 ms | 43.03 ms | 46.68 ms | 48.96 ms |
| 2 | `java` | 63.15 ms | 65.77 ms | 65.90 ms | 68.78 ms |
| 2 | `sql` | 41.13 ms | 41.90 ms | 45.62 ms | 47.17 ms |

Improvement of `sql` over `java`, on the medians:

| Run | calculationMs | End-to-end |
|---|---|---|
| 1 | **34.0%** faster (62.18 ms to 41.02 ms) | **28.6%** faster (65.36 ms to 46.68 ms) |
| 2 | 34.9% faster (63.15 ms to 41.13 ms) | 30.8% faster (65.90 ms to 45.62 ms) |

Both methods returned exactly the same holdings and the same total P&L.

## One thing to know about the seed script

The retention job keeps "the newest 50,000 ids", not "the newest 50,000 rows".
A bulk insert makes MySQL reserve more ids than it uses: after inserting 1,000,000 rows the next id was 1,048,561,
not 1,000,001. In a first attempt, that gap made retention keep only about 1,460 rows instead of 50,000.
`seed_prices.sql` now ends with `ALTER TABLE price_history AUTO_INCREMENT = 1`, which puts the counter back to
"highest id + 1". The numbers above are from the runs after that fix. The numbers from the first attempt were thrown away.
