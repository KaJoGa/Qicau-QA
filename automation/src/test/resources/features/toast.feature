Feature: Global Notifications (TOAST)

  @TOAST-01 @regression
  Scenario: Global toasts appear on every tab and the login page
    Given A global action is available
    When Trigger a global action (Sync Reset install) success and failure from different tabs
    Then Toast shown regardless of active tab (including login page)
    And green for success, red for error
    And auto-dismiss about 5s
    And closable with the close icon

  @TOAST-02 @regression
  Scenario: A new toast replaces an existing one
    Given A global toast is currently shown
    When Trigger a second global toast while the first is still visible
    Then The old toast is replaced by the new one
