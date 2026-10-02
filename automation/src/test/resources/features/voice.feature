Feature: Voice Input (VOICE)

  @VOICE-01 @smoke
  Scenario: Tapping mic starts recording with listening feedback
    Given Online, mic permission granted
    When Tap the microphone button
    Then Recording starts: "Mendengarkan..." text, button becomes a stop button, beep sound starts (+ vibration if supported)

  @VOICE-02 @smoke
  Scenario: Tapping stop while recording begins processing
    Given Currently recording
    When Tap the stop button
    Then Recording stops, beep stops, "Memproses..." shown, button disabled during processing

  @VOICE-03 @smoke
  Scenario: 2 seconds of silence auto-stops recording
    Given Recording in progress
    When Stay silent for at least 2 seconds while recording
    Then Recording auto-stops, then processes the same as tapping stop (VOICE-02)

  @VOICE-04 @regression
  Scenario: Recording auto-stops at 60 seconds
    Given Recording continuously with no silence gap
    When Keep speaking/recording continuously
    Then Recording auto-stops exactly at 60 seconds

  @VOICE-05 @regression
  Scenario: Accidental recording under 0.8s is discarded silently
    Given Mic permission granted
    When Tap mic and release/stop in under 0.8 seconds
    Then Ignored: no request sent, no transaction created, no error shown
    And UI returns to normal

  @VOICE-06 @smoke
  Scenario: Denied mic permission shows an alert
    Given Mic permission denied at OS/browser level
    When Tap the microphone button
    Then Alert "Membutuhkan akses mikrofon."
    And UI returns to ready state

  @VOICE-07 @smoke
  Scenario: Voice input blocked while offline
    Given Device offline
    When Tap the microphone button while offline
    Then Alert that voice recording needs internet and suggests "Input Manual"
    And recording does not start

  @VOICE-08 @smoke
  Scenario: High/medium confidence voice result is saved with success toast
    Given Online, mic permission granted; voice input yields high or medium confidence
    When Record a clear expense sentence
    Then Transaction saved
    And success toast shown (see SAVE-01)

  @VOICE-09 @smoke
  Scenario: Low confidence voice result is not saved shows low-confidence modal and is logged
    Given Voice input yields low confidence (e.g. unclear/noisy audio)
    When Record unclear/ambiguous audio
    Then Transaction not saved
    And "Suara Kurang Jelas" modal shown
    And a low-confidence log is recorded with source = voice

  @VOICE-10 @regression
  Scenario: Server/AI failure during voice processing shows an alert and saves nothing
    Given Voice request will fail server-side (e.g. AI service error)
    When Record a voice input while the AI service is failing
    Then Alert "Gagal memproses suara. ..."
    And no transaction saved
    And mic button becomes active again

  @VOICE-11 @regression
  Scenario: Starting a new recording dismisses an existing success toast
    Given A success toast is currently shown
    When While the toast is visible tap the mic and start recording again
    Then The old toast is dismissed
