Feature: Offline & PWA (PWA)

  @PWA-01 @smoke
  Scenario: Offline banner appears when connection drops
    Given App open and online
    When Disconnect network while app is open
    Then Banner "Mode Offline: Data tersimpan lokal & siap sync." appears below the header and pushes content down, not covering Reset Ekspor / Sync ke Sheets buttons or the page title, on both desktop and mobile widths

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
  Scenario: Update banner slides down from the very top and cannot be dismissed
    Given A new version has been deployed
    When Revisit the app after a new version has been deployed
    And Look at the banner position and for any close control
    And Click "Perbarui"
    Then "Pembaruan Tersedia" banner slides down from the very top of the screen, above all other elements
    And no close/X button
    And stays until updated
    And "Perbarui" loads the new version

  @PWA-06 @regression
  Scenario: Install button appears when not installed; install goes through the browser dialog
    Given Open in a supporting browser, not yet installed
    When Click the install button
    Then Install button available in header and Settings
    And click opens the browser install dialog
    And after install the button disappears. In-app success toast is NOT required (confirmation comes from the browser notification). Best effort: installed state is remembered so the button does not reappear after refresh in a normal tab

  @PWA-07 @regression
  Scenario: Install button is hidden when already running as an installed app
    Given App running as an installed PWA
    When Open the app as an installed PWA
    Then Install button not shown

  @PWA-08 @regression @manual-only
  Scenario: iOS and unsupported browsers show manual install guidance
    Given Browser without native install prompt support (e.g. iOS Safari)
    When Open the app in that browser
    Then Install button shows manual guidance (e.g. Share -> Add to Home Screen)
    And dialog is fully visible and centered when opened from both the header button and Settings. iOS Safari: "pada bilah navigasi Safari"
    And iOS Chrome/Firefox/Edge: neutral wording ("di bilah alamat atau menu browser Anda")

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
    Then Opens Riwayat tab (?tab=history) / Ringkasan tab (?tab=monthly) respectively

  @PWA-11 @regression
  Scenario: HTML response is not cached by the server
    Given the app is in its default state
    When Inspect response headers for the HTML page
    Then Cache-Control: no-store on the HTML response for every route including unknown SPA routes
    And hashed /assets/* files are long-cached (immutable). Test against a production build or the Worker - the local Vite dev server sends no-cache instead

  @PWA-12 @regression
  Scenario: Pending update installs automatically on return unless a Sync/Reset is running
    Given Update banner is showing
    When With the update banner showing, switch to another tab/app briefly and return
    And Repeat while a Sync or Reset is in progress
    Then Page reloads by itself with the new version, no click needed
    And if a Sync/Reset is running the page does NOT reload and the banner stays
