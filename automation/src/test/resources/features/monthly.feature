Feature: Monthly / Weekly Summary (MON)

  @MON-01 @smoke
  Scenario: Ringkasan tab shows title total card donut chart and category list
    Given Signed in
    When Open Ringkasan tab
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
    Given On Ringkasan tab
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
    When View Ringkasan with no transactions this month
    And Switch to Mingguan with none this week
    Then Total "Rp 0"
    And text "Belum ada riwayat transaksi." (weekly: "Belum ada riwayat transaksi minggu ini.")

  @MON-07 @smoke
  Scenario: Summary updates live when data changes
    Given Ringkasan tab open
    When Add edit or delete a transaction in the current period
    Then Summary updates automatically without reload

  @MON-08 @regression
  Scenario: Transactions from prior months are excluded
    Given Transactions exist in the current and a previous month
    When View the current month's total and category breakdown
    Then Previous month's transactions are not counted

  @MON-09 @smoke
  Scenario: Ringkasan opens without permission-denied errors and shows real totals
    Given Signed in with existing transactions this month; browser DevTools console open
    When Open the Ringkasan tab
    And Check the console
    And Compare totals to the transactions
    Then No "permission-denied" error in the console
    And total and categories match real data, not a wrong Rp 0

  @MON-10 @smoke
  Scenario: Add / edit / delete / undo keeps category breakdown, donut and total consistent
    Given Ringkasan tab reachable; some transactions this month
    When Add a transaction
    And Edit its amount and category
    And Undo a fresh one from the save toast
    And Delete another from Riwayat
    And After each step open Ringkasan
    And Look for any "rebuild"/manual repair control
    Then After every step total, donut and per-category rows all agree with each other (sum of category rows = total)
    And no button or manual step to rebuild the summary exists

  @MON-11 @regression
  Scenario: Inconsistent day is repaired automatically in the background when Ringkasan opens
    Given A day whose category breakdown does not match its total (legacy data; needs seeded/old data on emulator or a legacy account)
    When Open the Ringkasan tab
    And Wait a moment without any action
    And Compare breakdown/total with Riwayat
    And Repeat while offline
    Then The inconsistent day is fixed automatically (only that day), display becomes correct without user action or reload
    And no toast or dialog
    And nothing is repaired while offline

  @MON-12 @smoke
  Scenario: Offline create/edit/delete then online keeps Riwayat and Ringkasan in agreement
    Given Direct form usable offline; known totals
    When Go offline
    And Create, edit and delete transactions via the direct form / Riwayat
    And Go back online and wait for sync
    And Compare the Ringkasan total with the sum of transactions in Riwayat
    Then Transactions and daily summary are saved together
    And final Ringkasan numbers equal the Riwayat sum, no difference left over
