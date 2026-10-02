package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The signed-out welcome screen (AUTH-01). Locators are intentionally tag-agnostic
 * text/role matches on the literal Indonesian strings from test/Qicau.md, not guessed
 * data-testid/class names - the blind-testing rule (root CLAUDE.md) means this class was
 * never checked against the real rendered DOM. Confirm these against a live run and tighten
 * them (e.g. to a stable test id) if the app ever exposes one - see automation/README.md.
 */
public class WelcomePage {

  private static final By SIGN_IN_BUTTON =
      By.xpath("//button[contains(normalize-space(.), 'Lanjutkan dengan Google')]");
  private static final By TITLE =
      By.xpath("//*[contains(text(), 'Say it, Save it')]");
  private static final By TAB_NAV =
      By.xpath("//*[self::nav or @role='navigation']");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public WelcomePage(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
  }

  public boolean isDisplayed() {
    return !driver.findElements(TITLE).isEmpty() && !driver.findElements(SIGN_IN_BUTTON).isEmpty();
  }

  public boolean hasNoTabNavigation() {
    return driver.findElements(TAB_NAV).isEmpty();
  }

  /** Clicks "Lanjutkan dengan Google" - opens the Auth Emulator's fake account picker popup. */
  public void clickSignInWithGoogle() {
    wait.until(ExpectedConditions.elementToBeClickable(SIGN_IN_BUTTON)).click();
  }
}
