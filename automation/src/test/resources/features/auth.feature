Feature: Authentication & Session (AUTH)

  @AUTH-01 @smoke
  Scenario: Welcome screen shown when signed out
    Given No active session
    When Open the app with no prior session
    Then Welcome screen shown: logo, title "Qicau: Say it, Save it.", 3 feature bullets, "Lanjutkan dengan Google" button
    And no tab navigation visible

  @AUTH-02 @regression
  Scenario: Loading skeleton shown while auth state resolves
    Given App just launched
    When Observe the app in the moment before login state resolves
    Then Skeleton/placeholder shown, not the login page nor the main app

  @AUTH-03 @smoke
  Scenario: Successful Google sign-in lands on Catat tab
    Given Signed out
    When Click "Lanjutkan dengan Google"
    And Complete sign-in
    Then Lands on Catat tab
    And header and bottom nav (Catat/Riwayat/Bulanan) shown

  @AUTH-04 @regression
  Scenario: Closing the login popup leaves user on login page without error
    Given Signed out, login popup opened
    When Open the Google sign-in popup
    And Close it without completing sign-in
    Then No error/alert shown
    And user remains on the login page

  @AUTH-05 @regression
  Scenario: Login failure (other than user-cancelled) shows an alert
    Given Signed out; sign-in configured to fail with a non-cancel error
    When Attempt Google sign-in
    And Force an error other than popup-closed
    Then Alert "Gagal login: ..." shown

  @AUTH-06 @smoke
  Scenario: Sign out via Settings ends the session
    Given Signed in
    When Open Settings
    And Click "Keluar"
    Then Session ends, returns to login page, settings modal closes

  @AUTH-07 @smoke
  Scenario: User A never sees or modifies User B's data
    Given Two signed-in users A and B, each with own transactions
    When As A, attempt to read/list B's transactions/history
    And As A, attempt to modify/delete a transaction belonging to B
    Then A cannot see or change B's data at any point (UI or underlying request)
