package com.cloudtrader.market;

/** Summary of all stored history for one stock: lowest, highest, average price and number of ticks. */
public record PriceStats(String symbol, double low, double high, double average, long ticks) {
}
