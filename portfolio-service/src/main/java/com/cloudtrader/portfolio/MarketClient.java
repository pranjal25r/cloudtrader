package com.cloudtrader.portfolio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * portfolio-service never touches the price table directly.
 * When it needs a price, it asks market-service over HTTP, like calling another shop on the phone.
 */
@Component
public class MarketClient {

    private final RestClient http;

    public MarketClient(RestClient.Builder builder, @Value("${market.url}") String marketUrl) {
        this.http = builder.baseUrl(marketUrl).build();
    }

    /** Latest price of one stock. Throws 400 if market-service doesn't know the symbol. */
    public double latestPrice(String symbol) {
        try {
            MarketPrice price = http.get().uri("/prices/{symbol}", symbol)
                    .retrieve().body(MarketPrice.class);
            return price.price();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown symbol: " + symbol);
        } catch (RestClientException e) {
            throw marketDown(e);
        }
    }

    /** Latest price of every stock, as a map like {AAPL=191.2, MSFT=418.9, ...}. */
    public Map<String, Double> latestPrices() {
        try {
            MarketPrice[] prices = http.get().uri("/prices").retrieve().body(MarketPrice[].class);
            Map<String, Double> map = new LinkedHashMap<>();
            if (prices != null) {
                for (MarketPrice p : prices) {
                    map.put(p.symbol(), p.price());
                }
            }
            return map;
        } catch (RestClientException e) {
            throw marketDown(e);
        }
    }

    private ResponseStatusException marketDown(Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "market-service is not reachable", cause);
    }
}
