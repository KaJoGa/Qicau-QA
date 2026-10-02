package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** The Riwayat (History) tab (HIST-*). Text-based locators - see WelcomePage javadoc. */
public class HistoryPage {

  private static final By TITLE = By.xpath("//*[contains(text(), 'Riwayat Transaksi')]");
  // Confirmed 2026-09-29 via a real dump: the button's own visible label swaps to whatever
  // category is currently selected (e.g. "Transport"), not just "Semua Kategori"/"Kategori" -
  // its funnel icon (lucide-funnel) is the stable part, unlike the label text.
  private static final By CATEGORY_FILTER = By.xpath("//button[.//*[contains(@class, 'lucide-funnel')]]");
  private static final By TIME_FILTER =
      By.xpath("//button[.//span[contains(text(), 'Hari Terakhir') or contains(text(), 'Semua Waktu') or contains(text(), 'Bulan Ini')]]");
  private static final By EMPTY_STATE = By.xpath("//*[contains(text(), 'Belum ada riwayat transaksi.')]");
  private static final By DELETE_CONFIRM_DIALOG =
      By.xpath("//*[contains(text(), 'Hapus Transaksi?')]");
  private static final By DELETE_CONFIRM_BUTTON =
      By.xpath("//button[normalize-space(text())='Hapus']");
  private static final By DELETE_CANCEL_BUTTON =
      By.xpath("//button[contains(normalize-space(.), 'Batal')]");

  private final WebDriver driver;
  private final WebDriverWait wait;
  private final BottomNav bottomNav;

  public HistoryPage(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    this.bottomNav = new BottomNav(driver);
  }

  public void openViaNav() {
    bottomNav.clickRiwayat();
  }

  public boolean isDisplayed() {
    try {
      wait.until(ExpectedConditions.visibilityOfElementLocated(TITLE));
      return true;
    } catch (TimeoutException e) {
      return false;
    }
  }

  public boolean hasDefaultFilters() {
    return !driver.findElements(CATEGORY_FILTER).isEmpty() && !driver.findElements(TIME_FILTER).isEmpty();
  }

  public boolean isEmptyStateShown() {
    return !driver.findElements(EMPTY_STATE).isEmpty();
  }

  // Confirmed 2026-09-29: each compact row's amount renders as e.g. "-18.000" with NO "Rp"
  // prefix (unlike the total/toast/detail-modal, which all do show "Rp") - a real, separately
  // confirmed detail, not a guess.
  private static final By ROW_AMOUNT = By.xpath("//div[starts-with(normalize-space(text()), '-')]");
  private static final By DETAIL_MODAL_DELETE_BUTTON =
      By.xpath("//button[contains(normalize-space(.), 'Hapus Transaksi')]");

  /** One row per transaction. */
  public int transactionRowCount() {
    return driver.findElements(ROW_AMOUNT).size();
  }

  public void clickFirstRow() {
    driver.findElements(ROW_AMOUNT).get(0).click();
  }

  public void clickDeleteInDetailModal() {
    var el = wait.until(ExpectedConditions.elementToBeClickable(DETAIL_MODAL_DELETE_BUTTON));
    // The detail modal has a brief open animation (animate-in zoom-in-95) - elementToBeClickable
    // only checks displayed+enabled, not "animation finished", so a plain click can land on the
    // still-transitioning backdrop underneath. A JS click bypasses that coordinate-based check.
    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
  }

  public boolean isDeleteConfirmDialogShown() {
    return !driver.findElements(DELETE_CONFIRM_DIALOG).isEmpty();
  }

  public void confirmDelete() {
    wait.until(ExpectedConditions.elementToBeClickable(DELETE_CONFIRM_BUTTON)).click();
    // The confirm dialog's own backdrop (stacked above the detail modal's, z-60) can briefly
    // linger through its closing animation and intercept whatever click comes right after this
    // one (e.g. a bottom-nav tab) - wait for it to actually leave the DOM first.
    wait.until(ExpectedConditions.invisibilityOfElementLocated(DELETE_CONFIRM_DIALOG));
  }

  public void cancelDelete() {
    wait.until(ExpectedConditions.elementToBeClickable(DELETE_CANCEL_BUTTON)).click();
  }

  // Not yet confirmed against a real paginated run - guessed from the same lucide icon-library
  // pattern used everywhere else in the app (chevron-left/chevron-right), same spirit as every
  // other locator here; fix if a real run shows otherwise.
  private static final By PREV_PAGE_BUTTON = By.xpath("//button[.//*[contains(@class, 'lucide-chevron-left')]]");
  private static final By NEXT_PAGE_BUTTON = By.xpath("//button[.//*[contains(@class, 'lucide-chevron-right')]]");

  public boolean isPrevPageDisabled() {
    return driver.findElements(PREV_PAGE_BUTTON).stream()
        .findFirst()
        .map((el) -> !el.isEnabled() || "true".equals(el.getAttribute("disabled")))
        .orElse(false);
  }

  public boolean isNextPageDisabled() {
    return driver.findElements(NEXT_PAGE_BUTTON).stream()
        .findFirst()
        .map((el) -> !el.isEnabled() || "true".equals(el.getAttribute("disabled")))
        .orElse(false);
  }

  public void clickNextPage() {
    wait.until(ExpectedConditions.elementToBeClickable(NEXT_PAGE_BUTTON)).click();
  }

  // Confirmed 2026-09-29 via a real dump: both filters are custom dropdowns (button + an
  // absolutely-positioned options panel + a "fixed inset-0 z-40" click-outside backdrop), not
  // native <select> elements. Clicking an option directly (rather than re-clicking the trigger)
  // both selects it and closes the dropdown - re-clicking the trigger while the backdrop is up
  // gets silently intercepted by that backdrop instead.
  private static final By SYNC_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Sync')]");
  private static final By RESET_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Reset')]");

  public void openCategoryFilter() {
    wait.until(ExpectedConditions.elementToBeClickable(CATEGORY_FILTER)).click();
  }

  public void openTimeFilter() {
    wait.until(ExpectedConditions.elementToBeClickable(TIME_FILTER)).click();
  }

  private By dropdownOption(String exactText) {
    return By.xpath("//button[normalize-space(text())='" + exactText + "']");
  }

  public void selectCategory(String exactCategoryName) {
    openCategoryFilter();
    wait.until(ExpectedConditions.elementToBeClickable(dropdownOption(exactCategoryName))).click();
  }

  public void selectTimeFilter(String exactLabel) {
    openTimeFilter();
    wait.until(ExpectedConditions.elementToBeClickable(dropdownOption(exactLabel))).click();
  }

  public boolean areSyncResetButtonsHidden() {
    return driver.findElements(SYNC_BUTTON).isEmpty() && driver.findElements(RESET_BUTTON).isEmpty();
  }
}
