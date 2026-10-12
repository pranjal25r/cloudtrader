package com.cloudtrader.market;

import java.util.Random;

/**
 * Makes the next fake price from the current one.
 * Rule: move up or down by a random amount between -1% and +1%,
 * round to 2 decimals, and never drop below 1.00.
 *
 * Plain Java with no Spring and no database, so it is easy to unit test.
 */
public class PriceGenerator {

    static final double MAX_MOVE = 0.01; // 1%
    static final double MIN_PRICE = 1.00;

    private final Random random;

    public PriceGenerator(Random random) {
        this.random = random;
    }

    public double next(double currentPrice) {
        double change = (random.nextDouble() * 2 - 1) * MAX_MOVE; // between -0.01 and +0.01
        double next = currentPrice * (1 + change);
        next = Math.round(next * 100.0) / 100.0;                   // 2 decimal places
        return Math.max(next, MIN_PRICE);
    }
}
