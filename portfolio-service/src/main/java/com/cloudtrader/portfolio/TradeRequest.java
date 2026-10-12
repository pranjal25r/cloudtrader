package com.cloudtrader.portfolio;

/** What the user sends to place a trade, e.g. {"symbol":"AAPL","side":"BUY","quantity":10} */
public record TradeRequest(String symbol, String side, int quantity) {
}
