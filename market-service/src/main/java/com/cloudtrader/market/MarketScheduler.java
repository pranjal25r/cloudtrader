package com.cloudtrader.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * The two background jobs of this service:
 *  1. tick()    - every second, make a new price for each stock and save it  ("market data ingestion")
 *  2. cleanUp() - every 10 seconds, delete old rows beyond the cap            ("data retention")
 */
@Component
public class MarketScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketScheduler.class);

    private final PriceRepository repository;
    private final PriceGenerator generator = new PriceGenerator(new Random());
    private final String[] symbols;
    private final String[] startPrices;
    private final boolean retentionEnabled;
    private final long maxRows;

    // The current price of each stock, kept in memory so we don't have to re-read it every second
    private final Map<String, Double> currentPrices = new LinkedHashMap<>();

    public MarketScheduler(PriceRepository repository,
                           @Value("${market.symbols}") String[] symbols,
                           @Value("${market.start-prices}") String[] startPrices,
                           @Value("${market.retention.enabled}") boolean retentionEnabled,
                           @Value("${market.retention.max-rows}") long maxRows) {
        this.repository = repository;
        this.symbols = symbols;
        this.startPrices = startPrices;
        this.retentionEnabled = retentionEnabled;
        this.maxRows = maxRows;
    }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (currentPrices.isEmpty()) {
            loadStartingPrices();
        }
        for (String symbol : symbols) {
            double next = generator.next(currentPrices.get(symbol));
            currentPrices.put(symbol, next);
            repository.save(symbol, next);
        }
    }

    @Scheduled(fixedRate = 10_000)
    public void cleanUp() {
        if (!retentionEnabled) {
            return;
        }
        int deleted = repository.deleteAllButNewest(maxRows);
        if (deleted > 0) {
            log.info("Retention: deleted {} old price rows (keeping newest {})", deleted, maxRows);
        }
    }

    /** Continue from the last saved price if there is one (e.g. after a restart), else use the configured start price. */
    private void loadStartingPrices() {
        for (int i = 0; i < symbols.length; i++) {
            String symbol = symbols[i];
            double start = Double.parseDouble(startPrices[i]);
            double price = repository.latest(symbol).map(PricePoint::price).orElse(start);
            currentPrices.put(symbol, price);
        }
        log.info("Starting prices: {}", currentPrices);
    }
}
