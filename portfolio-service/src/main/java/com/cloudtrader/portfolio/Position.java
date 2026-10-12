package com.cloudtrader.portfolio;

/**
 * The result of adding up all trades for one stock.
 *   quantity    = shares bought - shares sold
 *   netInvested = money spent on buys - money received from sells
 */
public record Position(String symbol, long quantity, double netInvested) {
}
