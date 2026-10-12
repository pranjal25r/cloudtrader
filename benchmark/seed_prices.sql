-- Inserts 1,000,000 fake price rows into price_history (200,000 per stock).
-- Used by Experiment A to make the table big, so we can see how slow /prices/stats gets.
-- Run with:  mysql -u trader -ptrader123 cloudtrader < seed_prices.sql

-- A recursive CTE is a query that calls itself. MySQL stops it at 1,000 loops by default,
-- so we raise the limit to 1,000,000 for this session only.
SET SESSION cte_max_recursion_depth = 1000000;

INSERT INTO price_history (symbol, price)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 1000000
)
SELECT
    -- Rotate through the 5 stocks: row 1 = AAPL, row 2 = GOOGL, ... row 6 = AAPL again
    ELT((n % 5) + 1, 'AAPL', 'GOOGL', 'MSFT', 'TSLA', 'AMZN'),
    -- A price somewhere between 100.00 and 500.00
    ROUND(100 + RAND() * 400, 2)
FROM numbers;

-- A big bulk insert makes MySQL reserve more ids than it uses (the next id would be about 1,048,561,
-- not 1,000,001). The retention job keeps "the newest 50,000 ids", so that gap of unused ids would
-- make it keep far fewer than 50,000 rows. This puts the id counter back to "highest id + 1".
ALTER TABLE price_history AUTO_INCREMENT = 1;

SELECT COUNT(*) AS price_rows FROM price_history;
