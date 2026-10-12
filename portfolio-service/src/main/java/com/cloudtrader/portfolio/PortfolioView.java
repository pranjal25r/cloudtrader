package com.cloudtrader.portfolio;

import java.util.List;

/** What GET /portfolio returns. calculationMs = time spent adding up the trades. */
public record PortfolioView(String method, double calculationMs, List<Holding> holdings,
                            double totalMarketValue, double totalPnl) {
}
