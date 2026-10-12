package com.cloudtrader.portfolio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The HTTP "front desk" of portfolio-service.
 *   POST /trades                 -> buy or sell, body: {"symbol":"AAPL","side":"BUY","quantity":10}
 *   GET  /trades?limit=20        -> most recent trades
 *   GET  /portfolio              -> holdings, value and profit/loss (adds up in SQL)
 *   GET  /portfolio?method=java  -> same answer, but adds up in a Java loop (for the speed comparison)
 */
@RestController
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @PostMapping("/trades")
    @ResponseStatus(HttpStatus.CREATED)
    public Trade placeTrade(@RequestBody TradeRequest request) {
        return service.placeTrade(request);
    }

    @GetMapping("/trades")
    public List<Trade> recentTrades(@RequestParam(defaultValue = "20") int limit) {
        return service.recentTrades(limit);
    }

    @GetMapping("/portfolio")
    public PortfolioView portfolio(@RequestParam(defaultValue = "sql") String method) {
        return service.portfolio(method);
    }
}
