package com.qicau.qa.support;

import java.time.Duration;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Drives the Firebase Auth Emulator's own sign-in widget - the "fake account picker" that
 * `signInWithPopup(GoogleAuthProvider)` opens when VITE_USE_FIREBASE_EMULATOR=true
 * (test-plan.md section 3.1). This is Firebase's own tool UI, not Qicau's, so knowing its
 * shape is the same kind of tooling knowledge already used for L3
 * ({@code @firebase/rules-unit-testing}) - not a source-code read of the app under test.
 *
 * Locators confirmed 2026-09-28 against a real run (firebase-tools' emulator widget,
 * `#email-input`/`#display-name-input`/`#sign-in` - the emulator's own markup, not Qicau's).
 */
public class GoogleAuthEmulatorWidget {

  private static final By ADD_NEW_ACCOUNT = By.id("add-account-button");
  // Confirmed 2026-09-29: the emulator's fake accounts are server-side, not tied to any one
  // browser profile - a second, completely independent WebDriver session opening the popup sees
  // the same account in this "reuse" list (used for HOME-07/HIST-13's two-session scenarios).
  private static By reuseAccountRow(String email) {
    return By.xpath("//li[contains(@class,'js-reuse-account')][.//span[@id='reuse-email' and normalize-space(text())='" + email + "']]");
  }
  private static final By EMAIL_INPUT = By.id("email-input");
  private static final By DISPLAY_NAME_INPUT = By.id("display-name-input");
  private static final By SIGN_IN_SUBMIT = By.id("sign-in");

  private final WebDriver driver;

  public GoogleAuthEmulatorWidget(WebDriver driver) {
    this.driver = driver;
  }

  /** Switches to the popup, creates/selects a fake account, and switches back to the app window. */
  public void signInAsNewFakeUser(String email, String displayName) {
    String mainWindow = driver.getWindowHandle();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until((d) -> d.getWindowHandles().size() > 1);

    Set<String> handles = driver.getWindowHandles();
    handles.stream()
        .filter((h) -> !h.equals(mainWindow))
        .findFirst()
        .ifPresent((h) -> driver.switchTo().window(h));

    wait.until(ExpectedConditions.elementToBeClickable(ADD_NEW_ACCOUNT)).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT)).sendKeys(email);
    driver.findElements(DISPLAY_NAME_INPUT).stream().findFirst().ifPresent((el) -> el.sendKeys(displayName));
    wait.until(ExpectedConditions.elementToBeClickable(SIGN_IN_SUBMIT)).click();

    wait.until((d) -> d.getWindowHandles().size() == 1);
    driver.switchTo().window(mainWindow);
  }

  /** Signs in as a fake account created earlier (by any session, including a different
   * WebDriver instance) via the widget's own "reuse account" list, instead of creating a new
   * one - lets two independent browser sessions share the same signed-in user. */
  public void signInAsExistingFakeUser(String email) {
    String mainWindow = driver.getWindowHandle();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until((d) -> d.getWindowHandles().size() > 1);

    driver.getWindowHandles().stream()
        .filter((h) -> !h.equals(mainWindow))
        .findFirst()
        .ifPresent((h) -> driver.switchTo().window(h));

    wait.until(ExpectedConditions.elementToBeClickable(reuseAccountRow(email))).click();

    wait.until((d) -> d.getWindowHandles().size() == 1);
    driver.switchTo().window(mainWindow);
  }

  /** Opens the popup (same as the start of signInAsNewFakeUser) but closes it without ever
   * submitting a sign-in - the "user cancelled" path (AUTH-04). */
  public void openThenCloseWithoutSigningIn() {
    String mainWindow = driver.getWindowHandle();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until((d) -> d.getWindowHandles().size() > 1);

    driver.getWindowHandles().stream()
        .filter((h) -> !h.equals(mainWindow))
        .findFirst()
        .ifPresent((h) -> {
          driver.switchTo().window(h);
          driver.close();
        });
    driver.switchTo().window(mainWindow);
  }

  /** Opens the popup, fills in a fake account like signInAsNewFakeUser, but does not submit yet
   * - used by AUTH-05 to induce a network failure right before submission. */
  public void openAndFillNewFakeUser(String email, String displayName) {
    String mainWindow = driver.getWindowHandle();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until((d) -> d.getWindowHandles().size() > 1);

    driver.getWindowHandles().stream()
        .filter((h) -> !h.equals(mainWindow))
        .findFirst()
        .ifPresent((h) -> driver.switchTo().window(h));

    wait.until(ExpectedConditions.elementToBeClickable(ADD_NEW_ACCOUNT)).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT)).sendKeys(email);
    driver.findElements(DISPLAY_NAME_INPUT).stream().findFirst().ifPresent((el) -> el.sendKeys(displayName));
  }

  /** Submits the currently-open, already-filled popup (see openAndFillNewFakeUser) and switches
   * back to the main window regardless of whether the submit itself succeeds or errors out. */
  public void submitAndReturnToMainWindow() {
    String mainWindow = driver.getWindowHandles().stream()
        .filter((h) -> !h.equals(driver.getWindowHandle()))
        .findFirst()
        .orElse(driver.getWindowHandle());
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.elementToBeClickable(SIGN_IN_SUBMIT)).click();
    try {
      new WebDriverWait(driver, Duration.ofSeconds(5)).until((d) -> d.getWindowHandles().size() == 1);
    } catch (org.openqa.selenium.TimeoutException ignored) {
      // popup may stay open on a network error rather than closing - fall through and switch
      // back to the app window regardless, where the alert (if any) is expected to appear.
    }
    driver.switchTo().window(mainWindow);
  }
}
