Feature: Post-Save: Toast, Edit, Undo (SAVE)

  @SAVE-01 @smoke
  Scenario: Save success toast shows summary and action buttons
    Given A transaction was just saved
    When Save a transaction via voice or manual input
    Then Toast shows "Tersimpan: {platform or category}", a "Kategori . Rp ..." line, a detail line, and "Edit Transaksi" & "Batal" buttons

  @SAVE-02 @regression
  Scenario: Toast auto-dismisses after about 5 seconds
    Given Save toast visible
    When Leave the toast untouched
    Then Toast disappears automatically after roughly 5 seconds

  @SAVE-03 @regression
  Scenario: Dismissing toast with the close icon keeps the transaction
    Given Save toast visible
    When Click the close (x) icon on the toast
    Then Toast disappears
    And transaction remains saved

  @SAVE-04 @smoke
  Scenario: Undo (Batal) removes the transaction and reduces today's total
    Given Save toast visible
    When Click "Batal" on the toast
    Then Toast disappears
    And transaction is deleted
    And today's total decreases accordingly

  @SAVE-05 @smoke
  Scenario: Edit Transaksi from toast opens the edit modal pre-filled
    Given Save toast visible
    When Click "Edit Transaksi"
    Then Toast disappears
    And edit modal opens pre-filled with that transaction's data

  @SAVE-06 @regression
  Scenario: Edit modal Platform field is required max 50 chars with counter
    Given Edit modal open
    When Clear the Platform field and try to save
    And Type more than 50 characters into Platform
    Then Platform required to save
    And input capped at 50 chars with an "n/50" counter

  @SAVE-07 @regression
  Scenario: Category/Payment method are type-ahead selects and unmatched text reverts
    Given Edit modal open
    When Type a partial match, e.g. "pay", into the payment method field, then press Enter/click outside
    And Type text that matches nothing, then click outside
    Then Partial match resolves to the matching fixed option (e.g. Paylater)
    And text matching nothing reverts to the previous value

  @SAVE-08 @regression
  Scenario: Price field enforces digit-only cap and non-negative
    Given Edit modal open
    When Enter a value with 10+ digits or greater than 999999999
    And Try to enter a negative value
    Then Value is capped at 999.999.999
    And negative values are not accepted

  @SAVE-09 @regression
  Scenario: Notes field is optional max 200 chars with counter
    Given Edit modal open
    When Leave Catatan empty and save
    And Type more than 200 characters into Catatan
    Then Empty Catatan is accepted
    And input capped at 200 chars with an "n/200" counter

  @SAVE-10 @smoke
  Scenario: Saving edits updates Home Riwayat and Ringkasan
    Given Edit modal open with changes made
    When Change one or more fields
    And Click "Simpan Perubahan"
    Then Modal closes
    And the change is reflected on Home, Riwayat, and Ringkasan

  @SAVE-11 @regression
  Scenario: Closing edit modal without saving discards changes
    Given Edit modal open with unsaved changes
    When Change a field
    And Click the close (x) icon to close the modal
    Then Changes are discarded
    And transaction unchanged
