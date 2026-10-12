package com.cloudtrader.market;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The HTTP "front desk" of market-service.
 *   GET /prices                    -> latest price of every stock
 *   GET /prices/AAPL               -> latest price of one stock
 *   GET /prices/AAPL/history?limit=50 -> recent prices of one stock
 *   GET /prices/stats              -> low / high / average over all stored history
 */
@RestController
public class PriceController {

    private final PriceRepository repository;

    public PriceController(PriceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/prices")
    public List<PricePoint> latestForAll() {
        return repository.latestForAll();
    }

    @GetMapping("/prices/stats")
    public List<PriceStats> stats() {
        return repository.stats();
    }

    @GetMapping("/prices/{symbol}")
    public ResponseEntity<PricePoint> latest(@PathVariable String symbol) {
        return repository.latest(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prices/{symbol}/history")
    public List<PricePoint> history(@PathVariable String symbol,
                                    @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return repository.history(symbol.toUpperCase(), safeLimit);
    }
}
