package com.cloudtrader.portfolio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The money maths. Plain Java with no Spring and no database, so it is easy to unit test.
 *
 * Profit/loss rule (covers both sold and still-held shares in one line):
 *     P&L = (shares still held x current price) - (money spent on buys - money received from sells)
 */
final class PortfolioCalculator {

    private PortfolioCalculator() {
    }

    /**
     * The "before" approach: every trade row is already loaded into Java, and we add them up in a loop.
     * The "after" approach does the same adding-up inside MySQL (see TradeRepository.positionsInSql).
     */
    static List<Position> aggregateInJava(List<Trade> trades) {
        Map<String, long[]> quantities = new TreeMap<>();   // TreeMap keeps symbols sorted A-Z
        Map<String, double[]> invested = new TreeMap<>();
        for (Trade t : trades) {
            int sign = "BUY".equals(t.side()) ? 1 : -1;
            quantities.computeIfAbsent(t.symbol(), s -> new long[1])[0] += (long) sign * t.quantity();
            invested.computeIfAbsent(t.symbol(), s -> new double[1])[0] += sign * t.quantity() * t.price();
        }
        List<Position> positions = new ArrayList<>();
        for (String symbol : quantities.keySet()) {
            positions.add(new Position(symbol, quantities.get(symbol)[0], round(invested.get(symbol)[0])));
        }
        return positions;
    }

    /** Combine positions with live prices to get market value and profit/loss for each stock. */
    static List<Holding> withPrices(List<Position> positions, Map<String, Double> prices) {
        List<Holding> holdings = new ArrayList<>();
        for (Position p : positions) {
            double price = prices.getOrDefault(p.symbol(), 0.0);
            double marketValue = p.quantity() * price;
            double pnl = marketValue - p.netInvested();
            holdings.add(new Holding(p.symbol(), p.quantity(), round(p.netInvested()),
                    price, round(marketValue), round(pnl)));
        }
        return holdings;
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
