Feature: Home / Catat Tab (HOME)

  @HOME-01 @smoke
  Scenario: Catat tab shows today's total mic button and recent list shell
    Given Signed in
    When Open Catat tab
    Then Shows "Pengeluaran Hari Ini" total, large microphone button, "Ketuk untuk Bicara" text, "Input Manual" button, "Baru Saja" list

  @HOME-02 @smoke
  Scenario: Today's total equals sum of today's transactions Rp 0 when none
    Given At least one transaction saved today
    When Note today's transactions and their harga values
    And Compare to displayed total
    And Delete all today's transactions and reload
    Then Total = sum of harga for all of the user's transactions since 00:00 today
    And shows "Rp 0" with none

  @HOME-03 @smoke
  Scenario: Recent list shows at most 3 of today's transactions newest first
    Given More than 3 transactions saved today
    When Save 4+ transactions today
    And View "Baru Saja" list
    Then At most 3 rows shown, newest on top
    And each row shows category icon, platform (or category name if platform empty), "Kategori . Metode", amount prefixed with "-"

  @HOME-04 @regression
  Scenario: Empty state when no transactions today
    Given No transactions today
    When Open Catat tab with no transactions today
    Then "Belum ada transaksi hari ini." shown
    And no "Lihat Semua" link

  @HOME-05 @regression
  Scenario: Lihat Semua navigates to Riwayat
    Given At least one transaction today
    When Click "Lihat Semua"
    Then Navigates to Riwayat tab

  @HOME-06 @smoke
  Scenario: Total and recent list update live without reload
    Given Catat tab open
    When Add a new transaction (e.g. via another tab/device or Input Manual)
    And Delete a transaction
    Then Total and "Baru Saja" list update automatically without a page reload

  @HOME-07 @regression
  Scenario: Realtime sync across two open sessions
    Given Same account signed in on two tabs/devices
    When Add/delete a transaction on device A
    And Observe device B
    Then Change made on A appears on B without manual refresh
