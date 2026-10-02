package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The bottom tab bar shown on every signed-in screen (NAV-01..04). Confirmed 2026-09-28: each
 * tab is a {@code <button>} whose visible label is a {@code <span>} - a plain
 * {@code contains(., 'Riwayat')} xpath matches the whole <nav> wrapper too (it also contains
 * that text via its descendants), so a click on it does nothing. Scoping to the actual
 * {@code <button>} fixes that. The third tab's real label is "Ringkasan", not "Bulanan" - see
 * HomePage's note on this.
 */
public class BottomNav {

  private static final By CATAT = By.xpath("//button[.//span[normalize-space(text())='Catat']]");
  private static final By RIWAYAT = By.xpath("//button[.//span[normalize-space(text())='Riwayat']]");
  private static final By RINGKASAN = By.xpath("//button[.//span[normalize-space(text())='Ringkasan']]");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public BottomNav(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
  }

  public boolean isDisplayed() {
    return !driver.findElements(CATAT).isEmpty()
        && !driver.findElements(RIWAYAT).isEmpty()
        && !driver.findElements(RINGKASAN).isEmpty();
  }

  public void clickCatat() {
    wait.until(ExpectedConditions.elementToBeClickable(CATAT)).click();
  }

  public void clickRiwayat() {
    wait.until(ExpectedConditions.elementToBeClickable(RIWAYAT)).click();
  }

  public void clickRingkasan() {
    wait.until(ExpectedConditions.elementToBeClickable(RINGKASAN)).click();
  }
}
