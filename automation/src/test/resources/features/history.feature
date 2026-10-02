Feature: History / Riwayat (HIST)

  @HIST-01 @smoke
  Scenario: Riwayat tab opens with default filters and loads a list
    Given Signed in
    When Open Riwayat tab
    Then Title "Riwayat Transaksi"
    And default filters "7 Hari Terakhir" + "Semua Kategori"
    And loading skeleton then list

  @HIST-02 @regression
  Scenario: Empty state for the active filter hides Sync/Reset
    Given No transactions match the active filter
    When Apply a filter with no matching transactions
    Then "Belum ada riwayat transaksi." shown
    And Sync/Reset buttons hidden while list is empty

  @HIST-03 @smoke
  Scenario: Transactions grouped by day with correct labels
    Given Transactions exist across multiple days
    When View the list with transactions today yesterday and older
    Then Grouped by day, newest first
    And labels "Hari Ini", "Kemarin", else "D NamaBulan YYYY" (e.g. "5 Mei 2026")

  @HIST-04 @smoke
  Scenario: Each row shows icon platform/category category-method note amount and delete button
    Given At least one transaction with a note
    When View a transaction row in the list
    Then Category icon, platform (or category if empty), "Kategori . Metode", note if present, amount prefixed "-", delete button

  @HIST-05 @smoke
  Scenario: Time filter restricts the list to the selected range
    Given Transactions spanning more than 30 days
    When Select "Semua Waktu"
    And Select "7 Hari Terakhir"
    And Select "30 Hari Terakhir"
    And Select "Bulan Ini"
    Then Only transactions within the selected range shown each time
    And "Bulan Ini" = since day 1 00:00

  @HIST-06 @regression
  Scenario: Category filter restricts the list to the selected category
    Given Transactions across multiple categories
    When Select a specific category filter
    Then Only transactions in that category shown
    And "Semua Kategori" shows all

  @HIST-07 @regression
  Scenario: Changing a filter resets to page 1
    Given On page 2+ of results
    When While on page 2 change the time or category filter
    Then Returns to page 1

  @HIST-08 @smoke
  Scenario: Pagination shows 30 per page with correct button states
    Given More than 30 transactions match the filter
    When View page 1 (previous button should be disabled)
    And Click next to load older data
    And Continue until the last page (next button should be disabled)
    Then 30 transactions per page
    And previous disabled on page 1
    And next disabled when no more data
    And next loads older data with no duplicate/missing rows
    And page indicator shown

  @HIST-09 @smoke
  Scenario: Clicking a row opens the transaction detail modal
    Given At least one transaction in the list
    When Click a transaction row
    Then "Detail Pengeluaran" modal shows platform, full amount, category, method, full-format timestamp, note if present, "Hapus Transaksi" button

  @HIST-10 @regression
  Scenario: Delete icon or button opens a confirmation dialog
    Given At least one transaction in the list
    When Click the delete icon on a row, or "Hapus Transaksi" in the detail modal
    Then Confirmation dialog "Hapus Transaksi?" (irreversible) with "Batal" & "Hapus"

  @HIST-11 @smoke
  Scenario: Confirming delete removes the transaction everywhere
    Given Delete confirmation dialog open
    When Click "Hapus"
    Then Transaction disappears from the list, from Home total, and from Bulanan
    And detail modal (if open) also closes

  @HIST-12 @regression
  Scenario: Cancelling delete confirmation changes nothing
    Given Delete confirmation dialog open
    When Click "Batal"
    Then Nothing is deleted

  @HIST-13 @regression
  Scenario: New transaction from another tab/device appears on page 1
    Given Riwayat open on page 1, second session available
    When Add a transaction from another tab/device while viewing page 1
    Then New transaction appears automatically on page 1
