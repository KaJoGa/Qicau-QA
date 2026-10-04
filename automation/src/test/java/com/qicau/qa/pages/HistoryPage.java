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
      By.xpath("//button[.//span[contains(text(), 'Hari Terakhir') or contains(text(), 'Semua Waktu') or contains(text(), 'Bulan Terakhir')]]");
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

  // Batal inside a full-screen dialog layer (delete confirm, Reset/Sync confirm). Scoped to the
  // "fixed inset-0" overlay and taking the last one in DOM order (the topmost dialog) so the
  // save toast's own "Batal" (undo) button can never be hit by mistake.
  private static final By DIALOG_CANCEL_BUTTON = By.xpath(
      "(//div[contains(@class,'fixed') and contains(@class,'inset-0')]//button[normalize-space(.)='Batal'])[last()]");

  public void cancelDelete() {
    var scoped = driver.findElements(DIALOG_CANCEL_BUTTON);
    if (!scoped.isEmpty()) {
      wait.until(ExpectedConditions.elementToBeClickable(DIALOG_CANCEL_BUTTON)).click();
      return;
    }
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

  // ---- Sprint 3 additions (spec update 2026-10-03/04) ----

  private static final By SYNC_BTN = By.xpath("//button[contains(normalize-space(.), 'Sync ke Sheets')]");
  private static final By RESET_BTN = By.xpath("//button[contains(normalize-space(.), 'Reset Ekspor')]");
  private static final By DETAIL_EDIT_BTN = By.xpath("//button[contains(normalize-space(.), 'Edit Transaksi')]");
  private static final By DETAIL_TITLE = By.xpath("//h3[contains(text(), 'Detail Pengeluaran')]");
  private static final By INDICATOR = By.xpath("//span[contains(normalize-space(.), ' / ')]");
  private static final java.util.regex.Pattern INDICATOR_RE = java.util.regex.Pattern.compile("^\\d+ / \\d+\\+?$");

  public boolean isSyncButtonDisplayed() {
    return driver.findElements(SYNC_BTN).stream().anyMatch(org.openqa.selenium.WebElement::isDisplayed);
  }

  public boolean isResetButtonDisplayed() {
    return driver.findElements(RESET_BTN).stream().anyMatch(org.openqa.selenium.WebElement::isDisplayed);
  }

  public void clickSync() {
    wait.until(ExpectedConditions.elementToBeClickable(SYNC_BTN)).click();
  }

  public void clickReset() {
    wait.until(ExpectedConditions.elementToBeClickable(RESET_BTN)).click();
  }

  public boolean isDetailModalOpen() {
    return !driver.findElements(DETAIL_TITLE).isEmpty();
  }

  public void waitForDetailModal() {
    wait.until(ExpectedConditions.visibilityOfElementLocated(DETAIL_TITLE));
  }

  /** Clicks the Edit Transaksi button inside the detail modal (JS click: the modal has an open animation). */
  public void clickEditInDetailModal() {
    var el = wait.until(ExpectedConditions.elementToBeClickable(DETAIL_EDIT_BTN));
    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
  }

  /** True if the detail modal has both buttons on (roughly) the same row, Edit to the left of Hapus. */
  public boolean detailButtonsSideBySide() {
    var edit = driver.findElement(DETAIL_EDIT_BTN).getRect();
    var del = driver.findElement(DETAIL_MODAL_DELETE_BUTTON).getRect();
    return Math.abs(edit.getY() - del.getY()) <= 4 && edit.getX() + edit.getWidth() <= del.getX() + 2;
  }

  /** The page indicator text(s) like "1 / 3" or "1 / 2+" (it renders above and below the list). */
  public java.util.List<String> pageIndicators() {
    java.util.List<String> out = new java.util.ArrayList<>();
    for (var el : driver.findElements(INDICATOR)) {
      String t = el.getText().trim();
      if (INDICATOR_RE.matcher(t).matches()) {
        out.add(t);
      }
    }
    return out;
  }

  public String pageIndicator() {
    var all = pageIndicators();
    return all.isEmpty() ? "" : all.get(0);
  }

  /** Texts of every row's amount cell ("-12.000"), in on-screen order. */
  public java.util.List<String> rowAmounts() {
    java.util.List<String> out = new java.util.ArrayList<>();
    for (int attempt = 0; attempt < 3; attempt++) {
      out.clear();
      try {
        for (var el : driver.findElements(ROW_AMOUNT)) {
          out.add(el.getText().trim());
        }
        return out;
      } catch (org.openqa.selenium.StaleElementReferenceException e) {
        // the list re-rendered mid-read - retry
      }
    }
    return out;
  }

  /** Clicks the Nth row (0-based). */
  public void clickRow(int index) {
    driver.findElements(ROW_AMOUNT).get(index).click();
  }

  /** Visible text of all row title lines on the page (platform / category lines). */
  public String listText() {
    return driver.findElement(By.tagName("main")).getText();
  }
  /** Clicks the row whose amount cell reads exactly e.g. "-7.000". */
  public void clickRowWithAmount(String amountText) {
    wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//div[starts-with(normalize-space(text()), '-') and normalize-space(.)='" + amountText + "']"))).click();
  }

  /** Exact labels offered by the open time-filter dropdown (options are buttons in the panel). */
  public boolean timeFilterOptionPresent(String exactLabel) {
    return !driver.findElements(dropdownOption(exactLabel)).isEmpty();
  }

  /** Text of the "Hari Ini"/"Kemarin"/date group headings. */
  public java.util.List<String> dayGroupHeadings() {
    java.util.List<String> out = new java.util.ArrayList<>();
    for (var el : driver.findElements(By.xpath("//main//h3"))) {
      out.add(el.getText().trim());
    }
    return out;
  }
  /** Picks an option while the time dropdown is already open. */
  public void selectTimeFilterFromOpenMenu(String exactLabel) {
    wait.until(ExpectedConditions.elementToBeClickable(dropdownOption(exactLabel))).click();
  }
}
