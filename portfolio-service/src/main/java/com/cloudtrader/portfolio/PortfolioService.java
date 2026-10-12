package com.cloudtrader.portfolio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** The business rules: checking and placing trades, and building the portfolio view. */
@Service
public class PortfolioService {

    private final TradeRepository trades;
    private final MarketClient market;

    public PortfolioService(TradeRepository trades, MarketClient market) {
        this.trades = trades;
        this.market = market;
    }

    /** "Trade execution": check the request, get the live price, save the trade. */
    public Trade placeTrade(TradeRequest request) {
        if (request.symbol() == null || request.side() == null) {
            throw badRequest("symbol and side are required");
        }
        String symbol = request.symbol().toUpperCase();
        String side = request.side().toUpperCase();
        if (!side.equals("BUY") && !side.equals("SELL")) {
            throw badRequest("side must be BUY or SELL");
        }
        if (request.quantity() <= 0) {
            throw badRequest("quantity must be more than 0");
        }
        if (side.equals("SELL") && trades.sharesHeld(symbol) < request.quantity()) {
            throw badRequest("You can't sell more " + symbol + " shares than you own");
        }

        double price = market.latestPrice(symbol);   // HTTP call to market-service
        trades.save(symbol, side, request.quantity(), price);
        return new Trade(symbol, side, request.quantity(), price, LocalDateTime.now());
    }

    public List<Trade> recentTrades(int limit) {
        return trades.recent(Math.max(1, Math.min(limit, 500)));
    }

    /**
     * "Portfolio valuation and P&L".
     * method = "sql"  -> MySQL adds up the trades (fast, the default)
     * method = "java" -> load every trade into Java and add up in a loop (the slower "before" version,
     *                    kept so the speed difference can be measured)
     */
    public PortfolioView portfolio(String method) {
        boolean useJava = "java".equalsIgnoreCase(method);

        long start = System.nanoTime();
        List<Position> positions = useJava
                ? PortfolioCalculator.aggregateInJava(trades.allTrades())
                : trades.positionsInSql();
        double calculationMs = (System.nanoTime() - start) / 1_000_000.0;

        Map<String, Double> prices = market.latestPrices();
        List<Holding> holdings = PortfolioCalculator.withPrices(positions, prices);

        double totalValue = holdings.stream().mapToDouble(Holding::marketValue).sum();
        double totalPnl = holdings.stream().mapToDouble(Holding::pnl).sum();
        return new PortfolioView(useJava ? "java" : "sql",
                PortfolioCalculator.round(calculationMs),
                holdings,
                PortfolioCalculator.round(totalValue),
                PortfolioCalculator.round(totalPnl));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
