package com.cloudtrader.portfolio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Every SQL query for the trades table lives here. */
@Repository
public class TradeRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Trade> TRADE_ROW = (rs, rowNum) -> new Trade(
            rs.getString("symbol"),
            rs.getString("side"),
            rs.getInt("quantity"),
            rs.getDouble("price"),
            rs.getTimestamp("traded_at").toLocalDateTime());

    public TradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(String symbol, String side, int quantity, double price) {
        jdbc.update("INSERT INTO trades (symbol, side, quantity, price) VALUES (?, ?, ?, ?)",
                symbol, side, quantity, price);
    }

    /** How many shares of this stock we currently own (buys minus sells). */
    public long sharesHeld(String symbol) {
        Long held = jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN side = 'BUY' THEN quantity ELSE -quantity END), 0)
                FROM trades WHERE symbol = ?
                """, Long.class, symbol);
        return held == null ? 0 : held;
    }

    public List<Trade> recent(int limit) {
        return jdbc.query("""
                SELECT symbol, side, quantity, price, traded_at FROM trades
                ORDER BY id DESC LIMIT ?
                """, TRADE_ROW, limit);
    }

    /** "Before" approach: pull EVERY trade row out of the database so Java can add them up. */
    public List<Trade> allTrades() {
        return jdbc.query("SELECT symbol, side, quantity, price, traded_at FROM trades", TRADE_ROW);
    }

    /**
     * "After" approach: let MySQL add everything up and send back just one row per stock.
     * BUY counts as +, SELL counts as -.
     */
    public List<Position> positionsInSql() {
        return jdbc.query("""
                SELECT symbol,
                       SUM(CASE WHEN side = 'BUY' THEN quantity         ELSE -quantity         END) AS quantity,
                       SUM(CASE WHEN side = 'BUY' THEN quantity * price ELSE -quantity * price END) AS net_invested
                FROM trades
                GROUP BY symbol
                ORDER BY symbol
                """, (rs, rowNum) -> new Position(
                rs.getString("symbol"),
                rs.getLong("quantity"),
                rs.getDouble("net_invested")));
    }
}
