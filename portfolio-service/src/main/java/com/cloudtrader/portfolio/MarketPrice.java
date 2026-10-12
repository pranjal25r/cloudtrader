package com.cloudtrader.portfolio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The part of market-service's answer that we need. Extra fields (like recordedAt) are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketPrice(String symbol, double price) {
}
