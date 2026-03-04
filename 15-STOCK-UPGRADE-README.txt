╔═══════════════════════════════════════════════════════════════════╗
║        15-STOCK EXPERIMENT WITH TREND ANALYSIS - COMPLETE          ║
╚═══════════════════════════════════════════════════════════════════╝

🎯 MAJOR UPGRADE: 10 → 15 STOCKS WITH MARKET TREND CLASSIFICATION

═══════════════════════════════════════════════════════════════════
📊 STOCK DISTRIBUTION (5-5-5 BALANCED SPLIT)
═══════════════════════════════════════════════════════════════════

BULLISH STOCKS (5):
  • stock_1  (test_stock_1.csv)   - Return: +6.67%
  • stock_2  (test_stock_2.csv)   - Return: +16.93%
  • stock_3  (test_stock_3.csv)   - Return: +19.22%
  • stock_4  (test_stock_4.csv)   - Return: +9.55%
  • stock_5  (test_stock_5.csv)   - Return: +30.88%

BEARISH STOCKS (5):
  • stock_6  (test_stock_6.csv)   - Return: -26.19%
  • stock_7  (test_stock_7.csv)   - Return: -33.77%
  • stock_8  (test_stock_8.csv)   - Return: -28.48%
  • stock_9  (test_stock_9.csv)   - Return: -11.64%
  • stock_10 (test_stock_10.csv)  - Return: -14.15%

SIDEWAYS STOCKS (5):
  • stock_11 (test_stock_11.csv)  - Return: +3.39%
  • stock_12 (test_stock_12.csv)  - Return: -3.16%
  • stock_13 (test_stock_13.csv)  - Return: +2.60%
  • stock_14 (test_stock_14.csv)  - Return: -3.86%
  • stock_15 (test_stock_15.csv)  - Return: +3.30%

═══════════════════════════════════════════════════════════════════
✅ WHAT'S NEW
═══════════════════════════════════════════════════════════════════

1. EXPERIMENT LENGTH
   Before: 10 stocks × 10 days = 100 decisions
   Now:    15 stocks × 10 days = 150 decisions

2. MARKET TREND CLASSIFICATION
   ✓ Each stock tagged as Bullish/Bearish/Sideways
   ✓ Displayed during trading
   ✓ Used for performance analysis

3. TREND-SPECIFIC METRICS
   For EACH market trend type, calculate:
   ✓ Sharpe Ratio
   ✓ Maximum Drawdown
   ✓ Volatility
   ✓ Win Rate
   ✓ Number of Trades
   ✓ Profit Factor
   ✓ Time in Market (avg holding period)

4. ENHANCED DATA EXPORT
   ✓ New export button in admin dashboard
   ✓ CSV includes all trend-specific metrics
   ✓ Separate columns for Bullish/Bearish/Sideways performance

═══════════════════════════════════════════════════════════════════
📁 FILES CHANGED
═══════════════════════════════════════════════════════════════════

UPDATED FILES:
✓ ExperimentService.java
  - TOTAL_STOCKS = 10 → 15
  - Added marketTrend to current state

✓ ExperimentStock.java (model)
  - Added marketTrend field
  - Added getMarketTrend() and setMarketTrend()

✓ ExperimentDataLoader.java
  - Loads 15 stocks instead of 10
  - Assigns market trend to each stock
  - Maps stock_1 to stock_15

✓ AdminController.java
  - Added TrendMetricsService
  - Added /admin/export-enhanced endpoint
  - Generates CSV with trend breakdown

✓ admin/dashboard.html
  - Added "Export Enhanced Data" button
  - Styled as green download button

NEW FILES:
✓ TrendMetricsService.java
  - Calculates metrics separately for each trend
  - Groups decisions by stock trend
  - Computes Sharpe, volatility, win rate, etc.

═══════════════════════════════════════════════════════════════════
📊 ENHANCED CSV EXPORT FORMAT
═══════════════════════════════════════════════════════════════════

COLUMNS (31 total):

Basic Info (6):
  1. Username
  2. Full Name
  3. Email
  4. Total Stocks Completed
  5. Final Capital
  6. Total P/L

Bullish Metrics (7):
  7.  Bullish Sharpe Ratio
  8.  Bullish Max Drawdown
  9.  Bullish Volatility
  10. Bullish Win Rate
  11. Bullish Number of Trades
  12. Bullish Profit Factor
  13. Bullish Time in Market

Bearish Metrics (7):
  14. Bearish Sharpe Ratio
  15. Bearish Max Drawdown
  16. Bearish Volatility
  17. Bearish Win Rate
  18. Bearish Number of Trades
  19. Bearish Profit Factor
  20. Bearish Time in Market

Sideways Metrics (7):
  21. Sideways Sharpe Ratio
  22. Sideways Max Drawdown
  23. Sideways Volatility
  24. Sideways Win Rate
  25. Sideways Number of Trades
  26. Sideways Profit Factor
  27. Sideways Time in Market

Session Info (3):
  28. Completed (Yes/No)
  29. Start Time
  30. End Time

═══════════════════════════════════════════════════════════════════
🚀 DEPLOYMENT STEPS
═══════════════════════════════════════════════════════════════════

STEP 1: Extract and Replace
────────────────────────────
cd "D:\NycAd Stock App"
Extract ZIP and replace all files

STEP 2: Make Sure You Have 15 Stock CSV Files
────────────────────────────────────────────
Ensure you have files in src/main/resources/data/:
  - stock_1.csv  (Bullish)
  - stock_2.csv  (Bullish)
  - stock_3.csv  (Bullish)
  - stock_4.csv  (Bullish)
  - stock_5.csv  (Bullish)
  - stock_6.csv  (Bearish)
  - stock_7.csv  (Bearish)
  - stock_8.csv  (Bearish)
  - stock_9.csv  (Bearish)
  - stock_10.csv (Bearish)
  - stock_11.csv (Sideways)
  - stock_12.csv (Sideways)
  - stock_13.csv (Sideways)
  - stock_14.csv (Sideways)
  - stock_15.csv (Sideways)

NOTE: Rename your test_stock_X.csv files to stock_X.csv

STEP 3: Test Locally
────────────────────
mvn clean install
mvn spring-boot:run

Check console for:
✓ "SETTING UP 15 EXPERIMENT STOCKS"
✓ "Stock 1: stock_1 (Bullish)"
✓ ...
✓ "Stock 15: stock_15 (Sideways)"
✓ "Loaded 15 experiment stocks"

STEP 4: Test in Browser
────────────────────────
http://localhost:8080

Login as admin1 / admin123
- Dashboard should show "Export Enhanced Data" button
- Click it to download CSV with trend metrics

Login as pranav / pranav123
- Start experiment
- Should see 15 stocks instead of 10

STEP 5: Deploy to Railway
─────────────────────────
git add .
git commit -m "Upgrade to 15 stocks with trend analysis"
git push origin main

Wait 3-5 minutes for Railway build

═══════════════════════════════════════════════════════════════════
📈 HOW TO ANALYZE RESULTS
═══════════════════════════════════════════════════════════════════

After users complete experiments:

1. Login to admin panel: /admin/dashboard

2. Click "Export Enhanced Data"

3. Open CSV in Excel/Google Sheets

4. For each participant, you'll see:
   - How they performed in Bullish markets
   - How they performed in Bearish markets
   - How they performed in Sideways markets

5. Compare metrics:
   Example Analysis:
   - Participant A: High Sharpe in Bullish (0.85), Low in Bearish (-0.32)
   - Participant B: Consistent across all trends (0.45, 0.43, 0.41)

6. Answer questions like:
   - Who adapts best to different market conditions?
   - Who excels in bull markets but struggles in bears?
   - Who has consistent performance regardless of trend?

═══════════════════════════════════════════════════════════════════
💡 METRIC DEFINITIONS
═══════════════════════════════════════════════════════════════════

Sharpe Ratio:
  Risk-adjusted return. Higher = better risk-adjusted performance
  Formula: (Average Return) / (Volatility)

Max Drawdown:
  Largest peak-to-trough decline. Lower = better downside protection
  Formula: ((Peak - Trough) / Peak) × 100

Volatility:
  Standard deviation of returns. Lower = more stable performance

Win Rate:
  Percentage of profitable trades. Higher = more consistent wins
  Formula: (Winning Trades / Total Trades) × 100

Number of Trades:
  Total buy + sell actions for that trend

Profit Factor:
  Gross profits / Gross losses. Above 1.0 = profitable
  Formula: Total Gains / Total Losses

Time in Market:
  Average days holding positions in that trend

═══════════════════════════════════════════════════════════════════
❓ TROUBLESHOOTING
═══════════════════════════════════════════════════════════════════

Issue: Build fails with "cannot find symbol: marketTrend"
Fix: Make sure ExperimentStock.java has the new field and getters/setters

Issue: Only loading 10 stocks
Fix: Check ExperimentDataLoader is updated to loop 1-15

Issue: Export CSV missing trend columns
Fix: Verify AdminController has exportEnhancedData method
     Verify TrendMetricsService.java exists

Issue: Database error on startup
Fix: Drop existing tables (H2 will recreate with new schema):
     Delete ./data/stockdb.mv.db file and restart

═══════════════════════════════════════════════════════════════════
✅ VERIFICATION CHECKLIST
═══════════════════════════════════════════════════════════════════

Before deploying:
☐ 15 CSV files renamed (stock_1.csv to stock_15.csv)
☐ mvn clean install → BUILD SUCCESS
☐ Console shows "Loaded 15 experiment stocks"
☐ Console shows all 3 trend types (Bullish, Bearish, Sideways)
☐ Admin dashboard has export button
☐ Export CSV downloads successfully
☐ CSV has 31 columns
☐ User experiment shows 150 total decisions (15 × 10)

After Railway deployment:
☐ Railway build succeeded
☐ Logs show 15 stocks loaded
☐ Admin export works on Railway
☐ Users can complete 15-stock experiments

═══════════════════════════════════════════════════════════════════
🎉 YOU'RE ALL SET!
═══════════════════════════════════════════════════════════════════

Your experiment now supports:
✓ 15 stocks (5 Bullish, 5 Bearish, 5 Sideways)
✓ 150 total trading decisions per user
✓ Trend-specific performance metrics
✓ Enhanced CSV export for analysis

Perfect for comparing how participants perform in different market
conditions and identifying their trading strengths/weaknesses!

═══════════════════════════════════════════════════════════════════
