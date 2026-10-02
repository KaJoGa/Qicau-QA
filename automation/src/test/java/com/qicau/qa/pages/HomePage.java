package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** The Catat (Home) tab after sign-in (HOME-*, AUTH-03). Text-based locators - see WelcomePage javadoc. */
public class HomePage {

  private static final By TODAY_TOTAL_HEADING =
      By.xpath("//*[contains(text(), 'Pengeluaran Hari Ini')]");
  private static final By TODAY_TOTAL_VALUE =
      By.xpath("//p[contains(text(), 'Pengeluaran Hari Ini')]/following-sibling::h2[1]");
  private static final By TAP_TO_SPEAK =
      By.xpath("//*[contains(text(), 'Ketuk untuk Bicara')]");
  // Confirmed 2026-09-28 against a real run: the mic button itself has no text (just an icon) -
  // "Ketuk untuk Bicara" is a caption in a sibling <p> right after it, not inside the button.
  private static final By MIC_BUTTON =
      By.xpath("//p[contains(text(), 'Ketuk untuk Bicara')]/preceding-sibling::div[1]//button");
  private static final By INPUT_MANUAL_BUTTON =
      By.xpath("//button[contains(normalize-space(.), 'Input Manual')]");
  private static final By RECENT_LIST_HEADING =
      By.xpath("//*[contains(text(), 'Baru Saja')]");

  private final WebDriver driver;
  private final WebDriverWait wait;
  private final BottomNav bottomNav;

  public HomePage(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    this.bottomNav = new BottomNav(driver);
  }

  public boolean isDisplayed() {
    try {
      wait.until(ExpectedConditions.visibilityOfElementLocated(TODAY_TOTAL_HEADING));
    } catch (TimeoutException e) {
      return false;
    }
    return !driver.findElements(TAP_TO_SPEAK).isEmpty();
  }

  public boolean hasBottomNav() {
    return bottomNav.isDisplayed();
  }

  public String totalText() {
    return driver.findElement(TODAY_TOTAL_VALUE).getText();
  }

  /** HOME-01: every element of the Catat tab's default shell is present. */
  public boolean hasFullShell() {
    return isDisplayed()
        && !driver.findElements(MIC_BUTTON).isEmpty()
        && !driver.findElements(INPUT_MANUAL_BUTTON).isEmpty()
        && !driver.findElements(RECENT_LIST_HEADING).isEmpty();
  }

  public void clickMicButtonIfPresent() {
    driver.findElements(MIC_BUTTON).stream().findFirst().ifPresent(org.openqa.selenium.WebElement::click);
  }

  // Confirmed 2026-09-29: MIC_BUTTON is anchored on the "Ketuk untuk Bicara" idle-state caption,
  // which is replaced by "Memproses..." while processing - so MIC_BUTTON itself can't be found
  // during that state. The button's own shape class (w-40 h-40 rounded-full, seen in every real
  // dump so far) is stable across all its states, unlike the caption text next to it.
  private static final By MIC_BUTTON_BY_SHAPE =
      By.xpath("//button[contains(@class, 'w-40') and contains(@class, 'rounded-full')]");

  public boolean isMicButtonDisabled() {
    return driver.findElements(MIC_BUTTON_BY_SHAPE).stream().findFirst()
        .map((el) -> !el.isEnabled() || "true".equals(el.getAttribute("disabled")))
        .orElse(false);
  }

  public void openViaNav() {
    bottomNav.clickCatat();
  }
}
