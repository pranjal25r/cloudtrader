package com.cloudtrader.market;

import java.time.LocalDateTime;

/** One price tick, as returned by the API. */
public record PricePoint(String symbol, double price, LocalDateTime recordedAt) {
}
