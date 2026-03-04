package com.stocktrading.dto;

public class TrendMetricsDTO {
    private double sharpeRatio;
    private double maxDrawdown;
    private double volatility;
    private double winRate;
    private int numberOfTrades;
    private double profitFactor;
    private double avgTimeInMarket;
    private int stocksInTrend;
    
    public TrendMetricsDTO() {}
    
    public double getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(double sharpeRatio) { this.sharpeRatio = sharpeRatio; }
    
    public double getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(double maxDrawdown) { this.maxDrawdown = maxDrawdown; }
    
    public double getVolatility() { return volatility; }
    public void setVolatility(double volatility) { this.volatility = volatility; }
    
    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    
    public int getNumberOfTrades() { return numberOfTrades; }
    public void setNumberOfTrades(int numberOfTrades) { this.numberOfTrades = numberOfTrades; }
    
    public double getProfitFactor() { return profitFactor; }
    public void setProfitFactor(double profitFactor) { this.profitFactor = profitFactor; }
    
    public double getAvgTimeInMarket() { return avgTimeInMarket; }
    public void setAvgTimeInMarket(double avgTimeInMarket) { this.avgTimeInMarket = avgTimeInMarket; }
    
    public int getStocksInTrend() { return stocksInTrend; }
    public void setStocksInTrend(int stocksInTrend) { this.stocksInTrend = stocksInTrend; }
}
