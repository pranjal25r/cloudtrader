package com.cloudtrader.portfolio;

import java.time.LocalDateTime;

/** One buy or sell, as stored in the trades table. */
public record Trade(String symbol, String side, int quantity, double price, LocalDateTime tradedAt) {
}
