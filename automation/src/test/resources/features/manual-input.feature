Feature: Manual Input (MAN)

  @MAN-01 @smoke
  Scenario: Manual input modal opens in Teks AI mode when online
    Given Online, signed in
    When Open "Input Manual" modal
    Then "Teks AI" mode active
    And textarea empty with placeholder example
    And "0/500" counter

  @MAN-02 @regression
  Scenario: Text input is capped at 500 characters
    Given Teks AI mode open
    When Type more than 500 characters
    Then Input truncated at 500
    And counter turns red at 500/500

  @MAN-03 @smoke
  Scenario: Submitting clear expense text saves a transaction
    Given Teks AI mode open, online
    When Type a clear expense sentence
    And Submit
    Then Modal closes, "Memproses...", then transaction saved + toast (SAVE-01)
    And textarea cleared

  @MAN-04 @regression
  Scenario: Submitting empty or whitespace-only text does nothing
    Given Teks AI mode open
    When Leave the textarea empty or type only spaces
    And Submit
    Then No request sent, no transaction created

  @MAN-05 @smoke
  Scenario: Low-confidence text result behaves like voice but with source text
    Given Teks AI mode open; input will yield low confidence
    When Submit an ambiguous/no-signal text
    Then Same as VOICE-09 (not saved, low-confidence modal, logged) but source = text and input_text populated

  @MAN-06 @smoke
  Scenario: Submitting AI text while offline is blocked
    Given Device offline, Teks AI mode open
    When Type a valid expense sentence
    And Submit while offline
    Then Alert that AI text processing needs internet and suggests "Formulir Langsung"
    And no transaction created

  @MAN-07 @regression
  Scenario: Server/AI failure on text submit shows an alert and saves nothing
    Given Teks AI mode open; AI service will fail
    When Submit a valid expense sentence while the AI service is failing
    Then Alert "Gagal memproses teks. ..."
    And no transaction created

  @MAN-08 @regression
  Scenario: Cancel or close on manual input modal discards input
    Given Manual input modal open with text entered
    When Click "Batal" or the close (x) icon
    Then Modal closes
    And no transaction created

  @MAN-10 @smoke
  Scenario: Offline opens directly in Formulir Langsung mode
    Given Device offline
    When Open "Input Manual" modal while offline
    Then Opens directly in "Formulir Langsung" mode labeled "(Offline)"
    And fully usable

  @MAN-11 @regression
  Scenario: Direct form has correct default values
    Given Formulir Langsung mode open
    When Open the form fresh
    Then Kategori = Makan, Metode = QRIS, all other fields empty

  @MAN-12 @regression
  Scenario: Price field accepts digits only thousands-formatted and capped
    Given Formulir Langsung mode open
    When Type "25000" into the price field
    And Attempt to exceed 999999999
    Then Only digits accepted
    And displays as "25.000"
    And capped at 999.999.999

  @MAN-13 @smoke
  Scenario: Saving with empty or zero price is rejected
    Given Formulir Langsung mode open, price left empty or 0
    When Leave price empty (or 0)
    And Try to save
    Then Rejected without any browser-native validation message
    And red inline error "Jumlah pengeluaran wajib diisi." shown under the price field and the price field gets a red border
    And no transaction saved

  @MAN-14 @smoke
  Scenario: Saving with only a valid price uses defaults for the rest
    Given Formulir Langsung mode open
    When Enter only a valid price
    And Save
    Then Saved with kategori Makan, metode QRIS, empty platform, detail = category name
    And success toast

  @MAN-15 @smoke
  Scenario: Saving with all fields filled saves exactly as entered
    Given Formulir Langsung mode open
    When Fill price, platform, category, method, and note
    And Save
    Then Saved exactly as entered
    And detail = the note entered

  @MAN-16 @regression
  Scenario: Empty note with platform filled uses platform as detail
    Given Formulir Langsung mode open, platform filled, note empty
    When Fill platform, leave note empty
    And Save
    Then Detail = platform name

  @MAN-17 @regression
  Scenario: Form resets after save for next use
    Given Just saved via Formulir Langsung
    When Save a transaction via the direct form
    And Reopen the form
    Then Modal closed
    And price/platform/note cleared for the next entry

  @MAN-18 @regression
  Scenario: Direct-form transactions are always high confidence and never logged low-confidence
    Given Formulir Langsung mode open
    When Save a transaction via the direct form
    Then Treated as high confidence
    And never produces a low-confidence log

  @MAN-19 @regression
  Scenario: Platform capped at 50 and Catatan at 200 characters with n/50 and n/200 counters in the direct form
    Given Formulir Langsung mode open
    When Type or paste 60 characters into Platform
    And Type or paste 250 characters into Catatan
    And Check the counters above both fields
    Then Platform stops at 50 and Catatan at 200 (cannot type or paste more)
    And counters "n/50" and "n/200" shown above the fields and turn red when full, like the AI-text input and edit modal
