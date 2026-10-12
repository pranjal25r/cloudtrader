-- Inserts 100,000 fake trades into the trades table (20,000 per stock).
-- Used by Experiment B to compare adding up trades in Java vs in SQL.
-- Run with:  mysql -u trader -ptrader123 cloudtrader < seed_trades.sql

-- Raise the recursion limit (default 1,000) for this session only.
SET SESSION cte_max_recursion_depth = 100000;

INSERT INTO trades (symbol, side, quantity, price)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 100000
)
SELECT
    -- Rotate through the 5 stocks
    ELT((n % 5) + 1, 'AAPL', 'GOOGL', 'MSFT', 'TSLA', 'AMZN'),
    -- 3 out of every 4 trades are BUYs, so we always own more than we sold
    IF(n % 4 = 0, 'SELL', 'BUY'),
    -- Between 1 and 10 shares
    1 + FLOOR(RAND() * 10),
    -- A price somewhere between 100.00 and 500.00
    ROUND(100 + RAND() * 400, 2)
FROM numbers;

SELECT COUNT(*) AS trade_rows FROM trades;
