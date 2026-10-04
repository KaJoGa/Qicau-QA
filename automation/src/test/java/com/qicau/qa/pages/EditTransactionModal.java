package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The transaction edit modal opened from the save toast's "Edit Transaksi" button (SAVE-05..11).
 * Confirmed 2026-09-28: Platform maxlength=50 (SAVE-06), Kategori/Metode are the same
 * type-ahead-combobox pattern as the direct form (SAVE-07), Harga is a real
 * {@code type="number"} input capped at 999999999 (SAVE-08), Catatan maxlength=200 (SAVE-09),
 * submit is "Simpan Perubahan" (SAVE-10).
 */
public class EditTransactionModal {

  private static final By HEADING = By.xpath("//h3[contains(text(), 'Edit Transaksi')]");
  private static final By CLOSE_X_BUTTON =
      By.xpath("//h3[contains(text(), 'Edit Transaksi')]/following-sibling::button[1]");
  private static final By PLATFORM_INPUT = fieldByLabel("Platform");
  private static final By KATEGORI_INPUT = fieldByLabel("Kategori");
  private static final By HARGA_INPUT = fieldByLabel("Harga");
  private static final By METODE_INPUT = fieldByLabel("Metode");
  private static final By CATATAN_INPUT = fieldByLabel("Catatan");
  private static final By SAVE_BUTTON =
      By.xpath("//button[@type='submit' and contains(., 'Simpan Perubahan')]");

  // The edit modal nests Harga's label in its own sub-row (sibling of the input's row), unlike
  // the direct form's flatter layout - so "nearest ancestor div that actually contains an
  // input" is used instead of a fixed ancestor::div[1], which only worked for the flatter cases.
  private static By fieldByLabel(String labelContains) {
    return By.xpath("//label[contains(text(), '" + labelContains + "')]/ancestor::div[.//input][1]//input");
  }

  private final WebDriver driver;
  private final WebDriverWait wait;

  public EditTransactionModal(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
  }

  public boolean isOpen() {
    return !driver.findElements(HEADING).isEmpty();
  }

  public String platformValue() {
    return driver.findElement(PLATFORM_INPUT).getAttribute("value");
  }

  public String hargaValue() {
    return driver.findElement(HARGA_INPUT).getAttribute("value");
  }

  public String kategoriValue() {
    return driver.findElement(KATEGORI_INPUT).getAttribute("value");
  }

  public String metodeValue() {
    return driver.findElement(METODE_INPUT).getAttribute("value");
  }

  public String catatanValue() {
    return driver.findElement(CATATAN_INPUT).getAttribute("value");
  }

  public void setPlatform(String value) {
    var el = driver.findElement(PLATFORM_INPUT);
    el.clear();
    el.sendKeys(value);
  }

  public void setHarga(String digitsOnly) {
    var el = driver.findElement(HARGA_INPUT);
    el.clear();
    el.sendKeys(digitsOnly);
  }

  // See ManualInputModal.clearViaKeyboard - plain clear() doesn't reliably reset this combobox.
  private void clearViaKeyboard(org.openqa.selenium.WebElement el) {
    el.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
    el.sendKeys(org.openqa.selenium.Keys.DELETE);
  }

  public void setKategori(String exactCategoryName) {
    var el = driver.findElement(KATEGORI_INPUT);
    clearViaKeyboard(el);
    el.sendKeys(exactCategoryName);
    el.sendKeys(org.openqa.selenium.Keys.TAB);
    // See ManualInputModal.setKategori's comment - TAB alone can leave the suggestion dropdown
    // open, intercepting a later click; a real outside-click actually closes it.
    driver.findElement(HEADING).click();
  }

  public void setMetode(String exactMethodName) {
    var el = driver.findElement(METODE_INPUT);
    clearViaKeyboard(el);
    el.sendKeys(exactMethodName);
    el.sendKeys(org.openqa.selenium.Keys.TAB);
    driver.findElement(HEADING).click();
  }

  public void setCatatan(String value) {
    var el = driver.findElement(CATATAN_INPUT);
    el.clear();
    el.sendKeys(value);
  }

  /** The n/50 or n/200 counter nearest the given field's own label, using the same
   * nearest-input-containing-ancestor scoping as fieldByLabel, so it doesn't pick up the wrong
   * field's counter when there are several on the page at once. */
  public String counterTextFor(String labelContains) {
    // normalize-space(.), not text() - the count and "/50" can render as separate text nodes
    // (same class of bug as ManualInputModal's CHAR_COUNTER), so text() alone can miss it.
    By counter = By.xpath(
        "//label[contains(text(), '" + labelContains + "')]/ancestor::div[.//input][1]//span[contains(normalize-space(.), '/')]");
    return driver.findElement(counter).getText();
  }

  public boolean isSaveButtonEnabled() {
    return driver.findElements(SAVE_BUTTON).stream().findFirst().map(org.openqa.selenium.WebElement::isEnabled).orElse(false);
  }

  public void save() {
    if (com.qicau.qa.support.Config.android()) {
      // Real phone: the first tap after typing only dismisses the soft keyboard (see ManualInputModal.saveDirectForm).
      ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
          "if (document.activeElement) { document.activeElement.blur(); }");
      try {
        Thread.sleep(700);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    wait.until(ExpectedConditions.elementToBeClickable(SAVE_BUTTON)).click();
  }

  public void closeWithX() {
    wait.until(ExpectedConditions.elementToBeClickable(CLOSE_X_BUTTON)).click();
  }
  /** True if any date/time-like input or a date/time label exists in the modal (HIST-16 says none may). */
  public boolean hasDateOrTimeField() {
    var scope = driver.findElement(By.xpath("//h3[contains(text(), 'Edit Transaksi')]/ancestor::div[contains(@class,'fixed')][1]"));
    boolean typed = !scope.findElements(By.xpath(".//input[@type='date' or @type='time' or @type='datetime-local' or @type='month']")).isEmpty();
    boolean labelled = !scope.findElements(By.xpath(
        ".//label[contains(translate(., 'TANGALWKU', 'tangalwku'), 'tanggal') or contains(translate(., 'TANGALWKU', 'tangalwku'), 'waktu')]")).isEmpty();
    return typed || labelled;
  }

  /** Names of every labelled field in the modal, for the HIST-16 / HIST-15 evidence. */
  public java.util.List<String> labelTexts() {
    java.util.List<String> out = new java.util.ArrayList<>();
    for (var el : driver.findElements(By.xpath("//h3[contains(text(), 'Edit Transaksi')]/ancestor::div[contains(@class,'fixed')][1]//label"))) {
      out.add(el.getText().trim());
    }
    return out;
  }
}
