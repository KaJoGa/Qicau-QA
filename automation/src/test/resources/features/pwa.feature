Feature: Offline & PWA (PWA)

  @PWA-01 @smoke
  Scenario: Offline banner appears when connection drops
    Given App open and online
    When Disconnect network while app is open
    Then Banner "Mode Offline: Data tersimpan lokal & siap sync." appears at top

  @PWA-02 @regression
  Scenario: Reconnect shows a temporary green banner
    Given App currently offline
    When Restore network after being offline
    Then Green "Kembali Online..." banner shown for about 4 seconds, then disappears

  @PWA-03 @smoke
  Scenario: Direct-form entries made offline sync automatically on reconnect
    Given App online
    When Go offline
    And Save a transaction via Formulir Langsung
    And Reconnect
    Then Transaction shown immediately in-app while offline
    And auto-syncs to the server once back online

  @PWA-04 @smoke
  Scenario: Internet-dependent features are blocked offline without crashing
    Given Device offline
    When Attempt voice input AI text input Sync and Reset Ekspor while offline
    Then Each is rejected with a clear message (see VOICE-07, MAN-06, SYNC-01, SYNC-17)
    And no crash

  @PWA-05 @regression
  Scenario: Update prompt appears after a new release
    Given A new version has been deployed
    When Revisit the app after a new version has been deployed
    Then Update prompt appears
    And accepting it loads the new version

  @PWA-06 @regression
  Scenario: Install button appears when not installed and install succeeds
    Given Open in a supporting browser, not yet installed
    When Click the install button
    Then Install button available (header & settings)
    And after successful install, toast "Aplikasi berhasil dipasang..." and button disappears

  @PWA-07 @regression
  Scenario: Install button is hidden when already running as an installed app
    Given App running as an installed PWA
    When Open the app as an installed PWA
    Then Install button not shown

  @PWA-08 @regression @manual-only
  Scenario: iOS and unsupported browsers show manual install guidance
    Given Browser without native install prompt support (e.g. iOS Safari)
    When Open the app in that browser
    Then Install button shows manual instructions (e.g. Share -> Add to Home Screen)

  @PWA-09 @regression
  Scenario: Manifest values match spec
    Given App installed or manifest inspected
    When Inspect the web app manifest
    Then Name "Qicau - Pencatat Pengeluaran Suara"
    And 192/512 icons + maskable
    And standalone mode
    And portrait orientation
    And theme color #0a0a0a
    And 3 shortcuts (Catat, Riwayat, Ringkasan)

  @PWA-10 @regression
  Scenario: Manifest shortcuts open the correct tab
    Given App installed
    When Launch the app via the "Riwayat" shortcut
    And Launch the app via the "Ringkasan" shortcut
    Then Opens Riwayat tab (?tab=history) / Bulanan tab (?tab=monthly) respectively

  @PWA-11 @regression
  Scenario: HTML response is not cached by the server
    Given the app is in its default state
    When Inspect response headers for the HTML page
    Then Cache-Control: no-store on the HTML response
