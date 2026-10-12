package com.cloudtrader.portfolio;

/** A Position plus the live price, so we can show market value and profit/loss. */
public record Holding(String symbol, long quantity, double netInvested,
                      double currentPrice, double marketValue, double pnl) {
}
