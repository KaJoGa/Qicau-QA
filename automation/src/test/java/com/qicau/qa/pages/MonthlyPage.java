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
  private static final By CATEGORY_ROW = By.xpath("//h3[normalize-space(text())='Kategori']/following-sibling::div//span[starts-with(normalize-space(.), 'Rp')]");

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
  // ---- Sprint 3 additions ----

  // Confirmed 2026-10-04 via dump: the category list is a sibling block after <h3>Kategori</h3>;
  // each row is name span + "Rp 12.000" amount span (BUG-006 no longer reproduces).
  private static final By CATEGORY_AMOUNT_SPANS =
      By.xpath("//h3[normalize-space(text())='Kategori']/following-sibling::div//span[starts-with(normalize-space(.), 'Rp')]");
  private static final By CATEGORY_NAME_SPANS =
      By.xpath("//h3[normalize-space(text())='Kategori']/following-sibling::div//div[span[starts-with(normalize-space(.), 'Rp')]]/div[1]/span[2]");
  private static final By MONTH_YEAR_LABEL =
      By.xpath("//h2[.//span[normalize-space(text())='Ringkasan Bulan Ini']]/span[2]");

  public String monthYearLabel() {
    var els = driver.findElements(MONTH_YEAR_LABEL);
    return els.isEmpty() ? "" : els.get(0).getText().trim();
  }

  public static long parseRupiah(String text) {
    String digits = text.replaceAll("[^0-9]", "");
    return digits.isEmpty() ? 0 : Long.parseLong(digits);
  }

  public long totalValue() {
    return parseRupiah(totalText());
  }

  /** Amounts of every per-category row, in on-screen order. */
  public java.util.List<Long> categoryAmounts() {
    java.util.List<Long> out = new java.util.ArrayList<>();
    for (var el : driver.findElements(CATEGORY_AMOUNT_SPANS)) {
      out.add(parseRupiah(el.getText()));
    }
    return out;
  }

  public java.util.List<String> categoryNames() {
    java.util.List<String> out = new java.util.ArrayList<>();
    for (var el : driver.findElements(CATEGORY_NAME_SPANS)) {
      out.add(el.getText().trim());
    }
    return out;
  }

  public long categoryAmountsSum() {
    return categoryAmounts().stream().mapToLong(Long::longValue).sum();
  }

  /** True when the donut chart (recharts svg) is rendered. */
  public boolean hasDonut() {
    return !driver.findElements(By.cssSelector(".recharts-wrapper, .recharts-pie")).isEmpty()
        || !driver.findElements(By.cssSelector("svg path.recharts-sector")).isEmpty();
  }

  public int donutSectorCount() {
    return driver.findElements(By.cssSelector(".recharts-sector")).size();
  }

  /** Any button whose text suggests a manual rebuild/repair control (removed by the Part 3 spec change). */
  public boolean hasRebuildControl() {
    return !driver.findElements(By.xpath(
        "//button[contains(translate(normalize-space(.), 'BUILDPAIRKSEJT', 'buildpairksejt'), 'rebuild') or contains(translate(normalize-space(.), 'BUILDPAIRKSEJT', 'buildpairksejt'), 'bangun ulang') or contains(translate(normalize-space(.), 'BUILDPAIRKSEJT', 'buildpairksejt'), 'perbaiki') or contains(translate(normalize-space(.), 'BUILDPAIRKSEJT', 'buildpairksejt'), 'repair')]")).isEmpty();
  }
}
