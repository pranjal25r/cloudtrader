package com.cloudtrader.market;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Every SQL query for the price_history table lives here.
 * JdbcTemplate = "send this SQL to the database and turn the rows into Java objects".
 */
@Repository
public class PriceRepository {

    private final JdbcTemplate jdbc;

    // Turns one database row into one PricePoint object
    private static final RowMapper<PricePoint> PRICE_ROW = (rs, rowNum) -> new PricePoint(
            rs.getString("symbol"),
            rs.getDouble("price"),
            rs.getTimestamp("recorded_at").toLocalDateTime());

    public PriceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(String symbol, double price) {
        jdbc.update("INSERT INTO price_history (symbol, price) VALUES (?, ?)", symbol, price);
    }

    /** Latest price of every stock: find each stock's newest row id, then fetch those rows. */
    public List<PricePoint> latestForAll() {
        return jdbc.query("""
                SELECT p.symbol, p.price, p.recorded_at
                FROM price_history p
                JOIN (SELECT symbol, MAX(id) AS id FROM price_history GROUP BY symbol) newest
                  ON p.id = newest.id
                ORDER BY p.symbol
                """, PRICE_ROW);
    }

    public Optional<PricePoint> latest(String symbol) {
        return jdbc.query("""
                SELECT symbol, price, recorded_at FROM price_history
                WHERE symbol = ? ORDER BY id DESC LIMIT 1
                """, PRICE_ROW, symbol).stream().findFirst();
    }

    public List<PricePoint> history(String symbol, int limit) {
        return jdbc.query("""
                SELECT symbol, price, recorded_at FROM price_history
                WHERE symbol = ? ORDER BY id DESC LIMIT ?
                """, PRICE_ROW, symbol, limit);
    }

    /**
     * Low / high / average over ALL stored history.
     * This query has to read every row, so it gets slower as the table grows.
     * That is exactly why we cap the table size (see deleteAllButNewest).
     */
    public List<PriceStats> stats() {
        return jdbc.query("""
                SELECT symbol, MIN(price) AS low, MAX(price) AS high,
                       ROUND(AVG(price), 2) AS average, COUNT(*) AS ticks
                FROM price_history
                GROUP BY symbol
                ORDER BY symbol
                """, (rs, rowNum) -> new PriceStats(
                rs.getString("symbol"),
                rs.getDouble("low"),
                rs.getDouble("high"),
                rs.getDouble("average"),
                rs.getLong("ticks")));
    }

    /**
     * Rolling retention: keep only the newest maxRows rows, delete everything older.
     * Finds the id of the maxRows-th newest row, then deletes every row with a smaller id.
     * Returns how many rows were deleted.
     */
    public int deleteAllButNewest(long maxRows) {
        // Find the id of the maxRows-th newest row. Everything older than it is deleted.
        List<Long> cutoff = jdbc.queryForList(
                "SELECT id FROM price_history ORDER BY id DESC LIMIT 1 OFFSET ?", Long.class, maxRows - 1);
        if (cutoff.isEmpty()) {
            return 0; // fewer than maxRows rows, nothing to delete
        }
        return jdbc.update("DELETE FROM price_history WHERE id < ?", cutoff.get(0));
    }
}
