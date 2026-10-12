package com.cloudtrader.portfolio;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks the money maths without needing a database or a running server. */
class PortfolioCalculatorTest {

    private static Trade trade(String symbol, String side, int qty, double price) {
        return new Trade(symbol, side, qty, price, null);
    }

    @Test
    void buysAndSellsAreAddedUpPerStock() {
        List<Position> positions = PortfolioCalculator.aggregateInJava(List.of(
                trade("AAPL", "BUY", 10, 100.0),    // spend 1000
                trade("AAPL", "SELL", 4, 110.0),    // get back 440
                trade("MSFT", "BUY", 2, 400.0)));   // spend 800

        assertEquals(List.of(
                new Position("AAPL", 6, 560.0),
                new Position("MSFT", 2, 800.0)), positions);
    }

    @Test
    void profitIncludesSoldAndStillHeldShares() {
        // Bought 10 @100 (1000), sold 4 @110 (440): net invested 560, still holding 6.
        // Now the price is 120: holding is worth 720, so P&L = 720 - 560 = +160.
        // Check: sold 4 shares for +40 profit, 6 held shares are up +120. 40 + 120 = 160.
        List<Holding> holdings = PortfolioCalculator.withPrices(
                List.of(new Position("AAPL", 6, 560.0)), Map.of("AAPL", 120.0));

        Holding aapl = holdings.get(0);
        assertEquals(720.0, aapl.marketValue());
        assertEquals(160.0, aapl.pnl());
    }

    @Test
    void fullySoldStockShowsRealisedProfit() {
        // Bought 5 @50 (250), sold 5 @60 (300): own 0 shares, net invested -50 -> profit 50
        List<Position> positions = PortfolioCalculator.aggregateInJava(List.of(
                trade("TSLA", "BUY", 5, 50.0),
                trade("TSLA", "SELL", 5, 60.0)));
        Holding tsla = PortfolioCalculator.withPrices(positions, Map.of("TSLA", 999.0)).get(0);

        assertEquals(0, tsla.quantity());
        assertEquals(50.0, tsla.pnl());
    }
}
