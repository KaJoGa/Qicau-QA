package com.qicau.qa.support;

import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.WelcomePage;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Shared "sign in as a fresh fake user" flow - every scenario whose Given implies an
 * authenticated session (however it's phrased in the generated feature text) uses this rather
 * than repeating the click-popup-fill-wait sequence in every steps class. */
public final class SignInHelper {

  private SignInHelper() {
  }

  public static void signIn(WebDriver driver) {
    signInAsNewUserWithEmail(driver, "qa+" + System.nanoTime() + "@example.com");
  }

  /** For scenarios that need to sign back in as a SPECIFIC, already-known email later (via
   * signInAsExisting) - e.g. two sessions sharing one account (HOME-07/HIST-13). */
  public static void signInAsNewUserWithEmail(WebDriver driver, String email) {
    new WelcomePage(driver).clickSignInWithGoogle();
    new GoogleAuthEmulatorWidget(driver).signInAsNewFakeUser(email, "Qicau QA");
    new WebDriverWait(driver, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
  }

  /** Signs in as a fake account created earlier by ANY session (see
   * GoogleAuthEmulatorWidget.signInAsExistingFakeUser) - the two-session mechanism behind
   * HOME-07/HIST-13. */
  public static void signInAsExisting(WebDriver driver, String email) {
    new WelcomePage(driver).clickSignInWithGoogle();
    new GoogleAuthEmulatorWidget(driver).signInAsExistingFakeUser(email);
    new WebDriverWait(driver, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
  }
}
