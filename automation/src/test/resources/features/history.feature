Feature: History / Riwayat (HIST)

  @HIST-01 @smoke
  Scenario: Riwayat tab opens with default filters and loads a list
    Given Signed in
    When Open Riwayat tab
    Then Title "Riwayat Transaksi"
    And default filters "7 Hari Terakhir" + "Semua Kategori"
    And loading skeleton then list

  @HIST-02 @regression
  Scenario: Empty state for the active filter keeps Sync ke Sheets and Reset Ekspor visible
    Given No transactions match the active filter
    When Apply a filter with no matching transactions
    Then "Belum ada riwayat transaksi." shown
    And Sync ke Sheets and Reset Ekspor buttons REMAIN visible (not dependent on list content)

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
    And Select "3 Bulan Terakhir"
    Then Only transactions within the selected range shown each time
    And "3 Bulan Terakhir" = 90 days back (not 3 calendar months)

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
    And indicator shows the REAL total pages and stays fixed (e.g. 1/5, 2/5 ... 5/5, not growing) also with a specific category filter active
    And with a category filter every page is full (30 rows, the last one the remainder), the total is exact (e.g. 1/2, 2/2) and no page is empty

  @HIST-09 @smoke
  Scenario: Clicking a row opens the transaction detail modal
    Given At least one transaction in the list
    When Click a transaction row
    Then "Detail Pengeluaran" modal shows platform, full amount, category, method, full-format timestamp, note if present, and the "Edit Transaksi" + "Hapus Transaksi" buttons side by side

  @HIST-10 @regression
  Scenario: Delete icon or button opens a confirmation dialog
    Given At least one transaction in the list
    When Click the delete icon on a row, or "Hapus Transaksi" in the detail modal
    Then Confirmation dialog "Hapus Transaksi?" (irreversible) with "Batal" & "Hapus"

  @HIST-11 @smoke
  Scenario: Confirming delete removes the transaction everywhere
    Given Delete confirmation dialog open
    When Click "Hapus"
    Then Transaction disappears from the list, from Home total, and from Ringkasan
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

  @HIST-14 @regression
  Scenario: Detail modal shows Edit Transaksi and Hapus Transaksi side by side
    Given At least one transaction in the list
    When Click a transaction row
    Then Both "Edit Transaksi" and "Hapus Transaksi" buttons shown side by side

  @HIST-15 @SAVE-06 @SAVE-07 @SAVE-08 @SAVE-09 @smoke
  Scenario: Edit Transaksi opens a pre-filled edit modal with the same validation as SAVE
    Given Detail modal open for a transaction
    When Click "Edit Transaksi"
    And Check the fields
    And Try the SAVE-07 combo-box, SAVE-08 price limit and negative value, and SAVE-09 200-char note rules
    Then Detail modal closes
    And edit modal opens pre-filled with that transaction
    And validation rules behave exactly as SAVE-06..09

  @HIST-16 @regression
  Scenario: Transaction date/time cannot be changed from the edit modal
    Given Edit modal open for an older transaction
    When Inspect the edit modal fields
    And Save a change
    And Check the day group in Riwayat
    Then No date/time field exists
    And transaction stays under its original day

  @HIST-17 @MON-07 @smoke
  Scenario: Saving an edit updates Riwayat, Home and Ringkasan
    Given Edit modal open for a transaction inside the current month
    When Change the price and category
    And Click "Simpan Perubahan"
    And Check Riwayat, Home and Ringkasan (monthly + weekly if in range)
    Then Modal closes
    And change shows immediately in Riwayat
    And final numbers in Home/Ringkasan reflect the edit (old category amount reduced, new one increased)

  @HIST-18 @regression
  Scenario: Closing the edit modal discards changes
    Given Edit modal open with changes made
    When Change fields
    And Click the close (X)
    Then Changes discarded
    And transaction unchanged in the list

  @HIST-19 @regression
  Scenario: Save failure shows an error toast and leaves the list unchanged
    Given Edit modal open; connection/server failure induced (e.g. block the Firestore request in DevTools or stop the emulator)
    When Change a field
    And Click "Simpan Perubahan"
    Then Error toast "Gagal menyimpan perubahan: ..."
    And list unchanged

  @HIST-20 @regression
  Scenario: Confirming delete while offline closes the dialog immediately and syncs after reconnect
    Given Signed in with two transactions, Riwayat open
    When Go offline and delete one transaction through the confirmation dialog
    Then The confirmation dialog closes right away and the row disappears from the list
    When Go back online and wait for the offline delete to sync
    Then The transaction stays deleted after a reload
    And Ringkasan total equals the sum of the remaining transactions in Riwayat
