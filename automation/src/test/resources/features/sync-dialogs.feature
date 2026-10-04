Feature: Sync / Reset confirmation dialogs - in-app part only (SYNC)

  @SYNC-15 @regression
  Scenario: Reset Ekspor requires confirmation and Batal makes no change
    Given Online, at least one transaction, Riwayat tab
    When Click "Reset Ekspor"
    And In the custom confirmation dialog, click "Batal"
    Then Custom confirmation dialog (not a native popup) appears
    And Batal -> no changes

  @SYNC-19 @regression
  Scenario: "Ya, Reset" is disabled for about 1 second after the Reset dialog appears
    Given Online, Riwayat tab
    When Click "Reset Ekspor"
    And Immediately try to click "Ya, Reset"
    And Wait about 1-2 seconds and observe the button
    Then Button disabled for ~1s, then enabled normally (test must wait, not click instantly)

  @SYNC-20 @regression
  Scenario: First sync on this browser shows the educational Google access dialog
    Given Online; browser storage cleared (never synced on this browser)
    When Click "Sync ke Sheets"
    And Read the dialog
    And Click "Batal"
    Then Dialog "Izinkan Akses Google Sheets & Drive" explains a Google permission popup is coming
    And Batal -> no process, no Google popup
