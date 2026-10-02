package com.qicau.qa.steps;

import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.pages.SettingsModal;
import com.qicau.qa.pages.WelcomePage;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverContext;
import com.qicau.qa.support.GoogleAuthEmulatorWidget;
import com.qicau.qa.support.SignInHelper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Glue for the @smoke scenarios in features/auth.feature (AUTH-01, AUTH-03, AUTH-06). This is a
 * representative slice, not the full AUTH suite - see automation/README.md for what's still
 * pending glue code. Step text is matched literally (regex) against the Gherkin generated from
 * test-cases/auth-nav.csv, so it reads a bit like prose rather than reusable parameterized steps.
 */
public class AuthSteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  // ---- AUTH-01: welcome screen shown when signed out ----

  @Given("^No active session$")
  public void noActiveSession() {
    // Hooks.before() already loads a fresh browser profile with no session.
  }

  @When("^Open the app with no prior session$")
  public void openAppWithNoPriorSession() {
    driver().get(Config.baseUrl());
  }

  @Then("^Welcome screen shown: logo, title \"Qicau: Say it, Save it\\.\", 3 feature bullets, \"Lanjutkan dengan Google\" button$")
  public void welcomeScreenShown() {
    Assertions.assertTrue(new WelcomePage(driver()).isDisplayed(), "Expected the welcome screen to be shown");
  }

  @Then("^no tab navigation visible$")
  public void noTabNavigationVisible() {
    Assertions.assertTrue(new WelcomePage(driver()).hasNoTabNavigation(), "Expected no tab navigation while signed out");
  }

  // ---- AUTH-03: successful Google sign-in lands on Catat tab ----

  @Given("^Signed out$")
  public void signedOut() {
    driver().get(Config.baseUrl());
  }

  @When("^Click \"Lanjutkan dengan Google\"$")
  public void clickSignInWithGoogle() {
    new WelcomePage(driver()).clickSignInWithGoogle();
  }

  @And("^Complete sign-in$")
  public void completeSignIn() {
    new GoogleAuthEmulatorWidget(driver()).signInAsNewFakeUser("qa+" + System.nanoTime() + "@example.com", "Qicau QA");
  }

  @Then("^Lands on Catat tab$")
  public void landsOnCatatTab() {
    Assertions.assertTrue(new HomePage(driver()).isDisplayed(), "Expected to land on the Catat tab after sign-in");
  }

  @And("^header and bottom nav \\(Catat/Riwayat/Bulanan\\) shown$")
  public void headerAndBottomNavShown() {
    Assertions.assertTrue(new HomePage(driver()).hasBottomNav(), "Expected the Catat/Riwayat/Bulanan bottom nav");
  }

  // ---- AUTH-06: sign out via Settings ends the session ----

  @Given("^Signed in$")
  public void signedIn() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
  }

  @When("^Open Settings$")
  public void openSettingsModal() {
    SettingsModal.open(driver());
  }

  @And("^Click \"Keluar\"$")
  public void clickKeluar() {
    new SettingsModal(driver()).clickSignOut();
  }

  @Then("^Session ends, returns to login page, settings modal closes$")
  public void sessionEndsReturnsToLoginPage() {
    Assertions.assertTrue(new WelcomePage(driver()).isDisplayed(), "Expected to return to the welcome/login page");
    Assertions.assertFalse(new SettingsModal(driver()).isOpen(), "Expected the settings modal to be closed");
  }

  // ---- AUTH-07: user A never sees or modifies user B's data ----
  // The UI itself has no way to even attempt viewing another uid's data (no such feature exists
  // to click into) - the black-box way to probe this from the UI layer is: A saves a transaction,
  // signs out, a completely fresh B signs in, and B's own Home/Riwayat must be empty (not showing
  // A's data). This is a lighter, UI-level companion check; the rigorous version of this
  // (SEC-03/05/09) already runs directly against the rules at L3.

  private static final String USER_A_MARKER = "AUTH07-user-A-only";

  @Given("^Two signed-in users A and B, each with own transactions$")
  public void twoSignedInUsersAAndB() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    ManualInputModal modalA = new ManualInputModal(driver());
    ManualInputModal.open(driver());
    modalA.switchToFormulirLangsung();
    modalA.setPrice("21000");
    modalA.setNote(USER_A_MARKER);
    modalA.saveDirectForm();

    SettingsModal.open(driver());
    new SettingsModal(driver()).clickSignOut();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> new WelcomePage(d).isDisplayed());

    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("5000");
  }

  @When("^As A, attempt to read/list B's transactions/history$")
  public void asAAttemptToReadListBsTransactions() {
    // Already signed in as the fresh user B at this point (the Given above ends on B) - B's own
    // Home/Riwayat are what's being inspected for any trace of A's marker.
    new HistoryPage(driver()).openViaNav();
  }

  @And("^As A, attempt to modify/delete a transaction belonging to B$")
  public void asAAttemptToModifyDeleteBsTransaction() {
    // No UI path exists to even target another uid's transaction id from here - there is nothing
    // further to attempt beyond confirming B's own view never contains A's data (checked next).
  }

  @Then("^A cannot see or change B's data at any point \\(UI or underlying request\\)$")
  public void aCannotSeeOrChangeBsData() {
    boolean leaked = !driver().findElements(By.xpath("//*[contains(text(), '" + USER_A_MARKER + "')]")).isEmpty();
    Assertions.assertFalse(leaked, "Expected user B's Riwayat to never show user A's transaction");
  }

  // ---- AUTH-04: closing the login popup leaves the user on the login page without error ----

  @Given("^Signed out, login popup opened$")
  public void signedOutLoginPopupOpened() {
    driver().get(Config.baseUrl());
    new WelcomePage(driver()).clickSignInWithGoogle();
  }

  @When("^Open the Google sign-in popup$")
  public void openTheGoogleSignInPopup() {
    // Already opened by the Given above - nothing further to do here.
  }

  @And("^Close it without completing sign-in$")
  public void closeItWithoutCompletingSignIn() {
    new GoogleAuthEmulatorWidget(driver()).openThenCloseWithoutSigningIn();
  }

  @Then("^No error/alert shown$")
  public void noErrorAlertShown() {
    try {
      org.openqa.selenium.Alert alert = driver().switchTo().alert();
      Assertions.fail("Expected no alert after cancelling sign-in, got: " + alert.getText());
    } catch (org.openqa.selenium.NoAlertPresentException expected) {
      // correct - no alert should appear on a user-cancelled popup close
    }
  }

  @And("^user remains on the login page$")
  public void userRemainsOnTheLoginPage() {
    Assertions.assertTrue(new WelcomePage(driver()).isDisplayed(), "Expected to remain on the welcome/login page after cancelling sign-in");
  }

  // ---- AUTH-05: a login failure other than user-cancelled shows an alert ----
  // Forced by going offline right before the emulator widget's own submit, so the underlying
  // signInWithPopup() call fails with a network error rather than the popup simply being closed
  // - a genuinely different failure mode than AUTH-04's cancellation, not a guess at one.
  // Confirmed 2026-09-29: this does NOT reliably reproduce the alert even waiting 25s - going
  // "offline" via CDP network conditions on the main window/browser doesn't necessarily fail the
  // popup's own request the way a real network drop would (or the SDK just hangs rather than
  // erroring). Left wired (like VOICE-06) rather than quietly dropped - the gap is in how the
  // failure is *forced* from outside, not a locator bug worth continuing to chase.

  @Given("^Signed out; sign-in configured to fail with a non-cancel error$")
  public void signedOutSignInConfiguredToFail() {
    driver().get(Config.baseUrl());
  }

  @When("^Attempt Google sign-in$")
  public void attemptGoogleSignIn() {
    new WelcomePage(driver()).clickSignInWithGoogle();
    new GoogleAuthEmulatorWidget(driver())
        .openAndFillNewFakeUser("qa+" + System.nanoTime() + "@example.com", "Qicau QA Fail");
  }

  @And("^Force an error other than popup-closed$")
  public void forceAnErrorOtherThanPopupClosed() {
    com.qicau.qa.support.NetworkSimulator.goOffline(driver());
    new GoogleAuthEmulatorWidget(driver()).submitAndReturnToMainWindow();
  }

  @Then("^Alert \"Gagal login: \\.\\.\\.\" shown$")
  public void alertGagalLoginShown() {
    try {
      String text = new WebDriverWait(driver(), Duration.ofSeconds(25))
          .until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent())
          .getText();
      Assertions.assertTrue(text.contains("Gagal login"), "Expected an alert starting with Gagal login, got: " + text);
      driver().switchTo().alert().accept();
    } finally {
      com.qicau.qa.support.NetworkSimulator.goOnline(driver());
    }
  }
}
