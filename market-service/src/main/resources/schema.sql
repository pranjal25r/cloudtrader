-- Runs every time market-service starts. "IF NOT EXISTS" makes it safe to re-run.
-- One row = one price tick for one stock.
CREATE TABLE IF NOT EXISTS price_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol      VARCHAR(10)   NOT NULL,
    price       DECIMAL(12,2) NOT NULL,
    recorded_at TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    -- Lets MySQL jump straight to "latest rows for symbol X" instead of scanning the table
    INDEX idx_symbol_id (symbol, id)
);
