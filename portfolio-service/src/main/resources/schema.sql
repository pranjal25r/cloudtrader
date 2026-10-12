-- Runs every time portfolio-service starts. "IF NOT EXISTS" makes it safe to re-run.
-- One row = one buy or sell. Holdings are never stored; they are always calculated from these rows.
CREATE TABLE IF NOT EXISTS trades (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol    VARCHAR(10)   NOT NULL,
    side      VARCHAR(4)    NOT NULL,   -- 'BUY' or 'SELL'
    quantity  INT           NOT NULL,
    price     DECIMAL(12,2) NOT NULL,   -- price per share at the moment of the trade
    traded_at TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_symbol (symbol)
);
