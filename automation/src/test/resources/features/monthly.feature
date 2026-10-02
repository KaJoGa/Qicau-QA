Feature: Monthly / Weekly Summary (MON)

  @MON-01 @smoke
  Scenario: Bulanan tab shows title total card donut chart and category list
    Given Signed in
    When Open Bulanan tab
    Then Title "Ringkasan Bulan Ini"
    And "Total Pengeluaran" card
    And donut chart
    And per-category list

  @MON-02 @smoke
  Scenario: Monthly total equals sum since day 1 00:00
    Given Transactions across current and previous months
    When Compare displayed total to the sum of transactions since the 1st of the current month 00:00
    Then Totals match

  @MON-03 @smoke
  Scenario: Weekly view starts from Monday and includes the current Sunday
    Given On Bulanan tab
    When Click "Mingguan"
    And Check totals against transactions since Monday 00:00 of the current week
    Then Title "Ringkasan Minggu Ini"
    And button now reads "Bulanan"
    And total = since Monday 00:00 this week (Sunday still counted as part of the same week)

  @MON-04 @regression
  Scenario: Per-category rows sorted descending with proportion bar
    Given Transactions across multiple categories
    When View the per-category list
    Then Only categories with transactions shown
    And sorted largest amount first
    And each row shows icon, name, amount, and a bar = amount divided by total

  @MON-05 @regression
  Scenario: Donut chart shows fixed per-category colors and tooltip hidden with no data
    Given Transactions across multiple categories; also test with none
    When View the donut chart with data
    And Hover a slice
    And Remove all transactions and view again
    Then One slice per category, consistent color per category, tooltip shows the Rupiah amount
    And chart hidden when there is no data

  @MON-06 @regression
  Scenario: Empty state text for monthly and weekly views
    Given No transactions in the current period
    When View Bulanan with no transactions this month
    And Switch to Mingguan with none this week
    Then Total "Rp 0"
    And text "Belum ada riwayat transaksi." (weekly: "Belum ada riwayat transaksi minggu ini.")

  @MON-07 @smoke
  Scenario: Summary updates live when data changes
    Given Bulanan tab open
    When Add edit or delete a transaction in the current period
    Then Summary updates automatically without reload

  @MON-08 @regression
  Scenario: Transactions from prior months are excluded
    Given Transactions exist in the current and a previous month
    When View the current month's total and category breakdown
    Then Previous month's transactions are not counted
