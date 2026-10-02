Feature: Navigation & Settings (NAV)

  @NAV-01 @smoke
  Scenario: Root path after login opens Catat tab
    Given Signed in
    When Navigate to "/"
    Then Catat tab is active

  @NAV-02 @regression
  Scenario: tab query param opens the matching tab
    Given Signed in
    When Navigate to "/?tab=history"
    And Navigate to "/?tab=monthly"
    And Navigate to "/?tab=home"
    Then The corresponding tab (Riwayat / Bulanan / Catat) is active in each case

  @NAV-03 @regression
  Scenario: Unknown tab query value falls back to Catat without error
    Given Signed in
    When Navigate to "/?tab=doesnotexist"
    Then Catat tab active, no error shown

  @NAV-04 @smoke
  Scenario: Clicking a tab switches content and highlights it
    Given Signed in
    When Click Riwayat tab
    And Click Bulanan tab
    And Click Catat tab
    Then Content changes accordingly each time
    And active tab is visually marked

  @NAV-05 @regression
  Scenario: Settings modal shows theme install sign-out and close
    Given Signed in
    When Click the settings icon
    Then "Pengaturan" modal shows Theme options (Sistem/Terang/Gelap), an install-app button, "Keluar", "Tutup"

  @NAV-06 @regression
  Scenario: Selecting Dark/Light theme applies immediately and persists
    Given Settings modal open
    When Select "Gelap"
    And Reload the app
    And Select "Terang"
    And Reload the app
    Then Theme changes immediately each time and is still applied after reload

  @NAV-07 @regression
  Scenario: System theme follows OS preference and updates live
    Given Settings modal open, theme set to "Sistem"
    When Select "Sistem"
    And Change the OS-level dark/light preference
    Then App theme follows the OS preference and updates when the OS setting changes

  @NAV-08 @regression
  Scenario: Wide desktop screen keeps content in a centered column
    Given Signed in, wide desktop viewport
    When Open the app on a wide desktop browser window
    Then Content is constrained to a centered column, not full width
