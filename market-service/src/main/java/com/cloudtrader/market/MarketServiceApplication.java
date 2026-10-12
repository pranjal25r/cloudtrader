package com.cloudtrader.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. @EnableScheduling switches on the timed jobs in MarketScheduler
 * (a new price every second, a clean-up every 10 seconds).
 */
@SpringBootApplication
@EnableScheduling
public class MarketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketServiceApplication.class, args);
    }
}
