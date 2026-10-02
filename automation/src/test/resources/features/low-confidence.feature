Feature: Low-Confidence Modal (LOWC)

  @LOWC-01 @regression
  Scenario: Low-confidence modal shows title explanation and two actions
    Given A voice/text input resulted in low confidence
    When Trigger a low-confidence result
    Then Modal titled "Suara Kurang Jelas" with explanation, "Input Manual" and "Tutup" buttons

  @LOWC-02 @regression
  Scenario: Tutup dismisses the low-confidence modal without creating a transaction
    Given Low-confidence modal open
    When Click "Tutup"
    Then Modal closes
    And no new transaction created

  @LOWC-03 @regression
  Scenario: Input Manual from low-confidence modal opens the manual input modal
    Given Low-confidence modal open
    When Click "Input Manual"
    Then Low-confidence modal closes
    And Input Manual modal opens

  @LOWC-04 @smoke
  Scenario: Low-confidence event is logged with transcript rejected output and source
    Given A low-confidence result occurred (voice or text)
    When Trigger a low-confidence result from voice
    And Trigger a low-confidence result from text
    Then One log document per event containing raw transcript, the rejected AI output, source (voice/text), and input_text present only when source = text
