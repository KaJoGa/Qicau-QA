package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** The Ringkasan/Bulanan (Monthly summary) tab (MON-*). Text-based locators - see WelcomePage javadoc. */
public class MonthlyPage {

  private static final By TITLE = By.xpath("//*[contains(text(), 'Ringkasan Bulan Ini')]");
  private static final By WEEKLY_TITLE = By.xpath("//*[contains(text(), 'Ringkasan Minggu Ini')]");
  private static final By TOTAL_LABEL = By.xpath("//p[contains(text(), 'Total Pengeluaran')]");
  private static final By TOTAL_VALUE =
      By.xpath("//p[contains(text(), 'Total Pengeluaran')]/following-sibling::h3[1]");
  private static final By MINGGUAN_TOGGLE =
      By.xpath("//button[.//span[normalize-space(text())='Mingguan' or normalize-space(text())='Bulanan']]");
  private static final By EMPTY_STATE = By.xpath("//*[contains(text(), 'Belum ada riwayat transaksi')]");
  private static final By EMPTY_STATE_MONTHLY = By.xpath("//*[normalize-space(text())='Belum ada riwayat transaksi.']");
  private static final By EMPTY_STATE_WEEKLY = By.xpath("//*[contains(text(), 'Belum ada riwayat transaksi minggu ini.')]");
  // Never confirmed populated with real data (see BUG-006 - the per-category list itself always
  // renders its own empty state regardless of data) - kept as the best-available guess for
  // MON-04's row structure, consistent with every other locator here being confirmed-or-labelled.
  private static final By CATEGORY_ROW = By.xpath("//h3[contains(text(),'Kategori')]/following-sibling::*//div[.//text()[contains(., 'Rp')]]");

  private final WebDriver driver;
  private final WebDriverWait wait;
  private final BottomNav bottomNav;

  public MonthlyPage(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    this.bottomNav = new BottomNav(driver);
  }

  public void openViaNav() {
    bottomNav.clickRingkasan();
  }

  public boolean isDisplayed() {
    try {
      wait.until(ExpectedConditions.visibilityOfElementLocated(TITLE));
      return true;
    } catch (TimeoutException e) {
      return false;
    }
  }

  public boolean isWeeklyDisplayed() {
    return !driver.findElements(WEEKLY_TITLE).isEmpty();
  }

  public boolean hasTotalCard() {
    return !driver.findElements(TOTAL_LABEL).isEmpty();
  }

  public String totalText() {
    return driver.findElement(TOTAL_VALUE).getText();
  }

  public boolean isEmptyStateShown() {
    return !driver.findElements(EMPTY_STATE).isEmpty();
  }

  public void toggleWeekly() {
    wait.until(ExpectedConditions.elementToBeClickable(MINGGUAN_TOGGLE)).click();
  }

  public boolean isMonthlyEmptyStateShown() {
    return !driver.findElements(EMPTY_STATE_MONTHLY).isEmpty();
  }

  public boolean isWeeklyEmptyStateShown() {
    return !driver.findElements(EMPTY_STATE_WEEKLY).isEmpty();
  }

  public int categoryRowCount() {
    return driver.findElements(CATEGORY_ROW).size();
  }
}
