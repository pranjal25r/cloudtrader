package com.cloudtrader.market;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the price rule without needing a database or a running server. */
class PriceGeneratorTest {

    private final PriceGenerator generator = new PriceGenerator(new Random(42)); // fixed seed = repeatable

    @Test
    void nextPriceMovesAtMostOnePercent() {
        double price = 100.00;
        for (int i = 0; i < 10_000; i++) {
            double next = generator.next(price);
            assertTrue(Math.abs(next - price) <= price * 0.01 + 0.005,
                    "moved too far: " + price + " -> " + next);
            price = next;
        }
    }

    @Test
    void priceHasAtMostTwoDecimals() {
        double next = generator.next(123.45);
        assertEquals(next, Math.round(next * 100.0) / 100.0, 1e-9);
    }

    @Test
    void priceNeverDropsBelowOne() {
        assertTrue(generator.next(1.00) >= 1.00);
        assertTrue(generator.next(0.50) >= 1.00);
    }
}
