package com.stocktrading.model;

public enum MarketTrend {

    BULLISH("Bullish"),
    BEARISH("Bearish"),
    SIDEWAYS("Sideways");

    private final String displayName;

    MarketTrend(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}