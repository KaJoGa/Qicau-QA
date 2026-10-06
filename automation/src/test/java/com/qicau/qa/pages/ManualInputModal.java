package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The "Input Manual" modal, both modes (MAN-*). Confirmed 2026-09-28 against a real run: online
 * opens in "Teks AI" mode (a maxlength=500 textarea, no id/name); "Formulir Langsung" mode has 5
 * labelled inputs with no id/name either, each found via `label -> ancestor div -> input` since
 * the category/method fields nest one div deeper than price/platform/note do. Kategori defaults
 * to "Makan", Metode Pembayaran defaults to "QRIS", confirming MAN-11.
 */
public class ManualInputModal {

  private static final By OPEN_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Input Manual')]");
  private static final By CLOSE_X_BUTTON =
      By.xpath("//h3[contains(text(), 'Input Manual')]/following-sibling::button[1]");
  private static final By CANCEL_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Batal')]");
  private static final By TEKS_AI_TAB = By.xpath("//button[normalize-space(text())='Teks AI']");
  private static final By FORMULIR_LANGSUNG_TAB =
      By.xpath("//button[contains(normalize-space(.), 'Formulir Langsung')]");
  private static final By TEXTAREA = By.cssSelector("textarea[maxlength='500']");
  // React likely renders {count}/500 as two separate text nodes, so contains(text(),...) (which
  // only inspects the first node) misses it - normalize-space(.) concatenates the whole element.
  private static final By CHAR_COUNTER =
      By.xpath("//span[contains(normalize-space(.), '/500')]");
  private static final By SEND_BUTTON = By.xpath("//button[@type='submit' and contains(., 'Kirim')]");
  private static final By OFFLINE_LABEL = By.xpath("//*[contains(text(), '(Offline)')]");

  private static final By PRICE_INPUT = fieldByLabel("Harga");
  private static final By PLATFORM_INPUT = fieldByLabel("Platform");
  private static final By KATEGORI_INPUT = fieldByLabel("Kategori");
  private static final By METODE_INPUT = fieldByLabel("Metode");
  private static final By NOTE_INPUT = fieldByLabel("Catatan");
  private static final By SAVE_TRANSACTION_BUTTON =
      By.xpath("//button[@type='submit' and contains(., 'Simpan Transaksi')]");

  private static By fieldByLabel(String labelContains) {
    // Nearest ancestor div that actually contains an input, not a fixed ancestor::div[1] - see
    // EditTransactionModal's fieldByLabel for why the fixed-depth version isn't safe in general.
    return By.xpath(
        "//label[contains(text(), '" + labelContains + "')]/ancestor::div[.//input][1]//input");
  }

  private final WebDriver driver;
  private final WebDriverWait wait;

  public ManualInputModal(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
  }

  public static void open(WebDriver driver) {
    driver.findElement(OPEN_BUTTON).click();
  }

  public boolean isTeksAIModeActive() {
    return !driver.findElements(TEXTAREA).isEmpty();
  }

  public boolean isFormulirLangsungModeActive() {
    return !driver.findElements(PRICE_INPUT).isEmpty();
  }

  public boolean isLabeledOffline() {
    return !driver.findElements(OFFLINE_LABEL).isEmpty();
  }

  public String textareaPlaceholder() {
    return driver.findElement(TEXTAREA).getAttribute("placeholder");
  }

  public String charCounterText() {
    return driver.findElement(CHAR_COUNTER).getText();
  }

  public String textareaValue() {
    return driver.findElement(TEXTAREA).getAttribute("value");
  }

  /** True if the counter's own class marks it red/warning. Confirmed 2026-09-29: the class is
   * literally "text-red-500" at 500/500 - checking the class name directly avoids depending on
   * getCssValue("color")'s format, which varies by Chrome version (rgb() vs the newer oklch()). */
  public boolean isCharCounterRed() {
    String cssClass = driver.findElement(CHAR_COUNTER).getAttribute("class");
    return cssClass != null && cssClass.contains("red");
  }

  public void switchToFormulirLangsung() {
    wait.until(ExpectedConditions.elementToBeClickable(FORMULIR_LANGSUNG_TAB)).click();
  }

  public void switchToTeksAI() {
    wait.until(ExpectedConditions.elementToBeClickable(TEKS_AI_TAB)).click();
  }

  public void typeAiText(String text) {
    driver.findElement(TEXTAREA).sendKeys(text);
  }

  public void submitAiText() {
    wait.until(ExpectedConditions.elementToBeClickable(SEND_BUTTON)).click();
  }

  public void cancel() {
    wait.until(ExpectedConditions.elementToBeClickable(CANCEL_BUTTON)).click();
  }

  public void closeWithX() {
    wait.until(ExpectedConditions.elementToBeClickable(CLOSE_X_BUTTON)).click();
  }

  // ---- Formulir Langsung (direct form) ----

  public String priceValue() {
    return driver.findElement(PRICE_INPUT).getAttribute("value");
  }

  public String kategoriValue() {
    return driver.findElement(KATEGORI_INPUT).getAttribute("value");
  }

  public String metodeValue() {
    return driver.findElement(METODE_INPUT).getAttribute("value");
  }

  public String platformValue() {
    return driver.findElement(PLATFORM_INPUT).getAttribute("value");
  }

  public String noteValue() {
    return driver.findElement(NOTE_INPUT).getAttribute("value");
  }

  public void setPrice(String digitsOnly) {
    WebElement el = driver.findElement(PRICE_INPUT);
    el.clear();
    el.sendKeys(digitsOnly);
  }

  public void setPlatform(String platform) {
    WebElement el = driver.findElement(PLATFORM_INPUT);
    el.clear();
    el.sendKeys(platform);
  }

  /** Type-ahead combobox (confirmed 2026-09-29 against a real dropdown dump): typing an exact
   * option name and pressing Tab/Enter resolves it; anything else reverts (SAVE-07's pattern). */
  private static final By MODAL_HEADING = By.xpath("//h3[contains(text(), 'Input Manual')]");

  // Confirmed 2026-09-29: plain el.clear() does NOT reliably clear this specific combobox -
  // the field snapped back to its previous value (e.g. "Makan") right after, so a later
  // sendKeys landed as "MakanTrans" instead of "Trans". A real select-all + delete (dispatching
  // actual key events, the same way every other confirmed-working sendKeys() call here does)
  // clears it correctly; WebDriver's own clear command apparently doesn't sync this component's
  // internal combobox state the way plain typing does.
  private void clearViaKeyboard(WebElement el) {
    el.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
    el.sendKeys(org.openqa.selenium.Keys.DELETE);
  }

  public void setKategori(String exactCategoryName) {
    WebElement el = driver.findElement(KATEGORI_INPUT);
    clearViaKeyboard(el);
    el.sendKeys(exactCategoryName);
    el.sendKeys(org.openqa.selenium.Keys.TAB);
    // Confirmed 2026-09-29: TAB alone can leave the suggestion dropdown visually open, which then
    // intercepts a later click on "Simpan Transaksi" underneath it - a real outside-click (not
    // just a focus change) is what actually closes it.
    driver.findElement(MODAL_HEADING).click();
  }

  public void setMetode(String exactMethodName) {
    WebElement el = driver.findElement(METODE_INPUT);
    clearViaKeyboard(el);
    el.sendKeys(exactMethodName);
    el.sendKeys(org.openqa.selenium.Keys.TAB);
    driver.findElement(MODAL_HEADING).click();
  }

  public void setNote(String note) {
    WebElement el = driver.findElement(NOTE_INPUT);
    el.clear();
    el.sendKeys(note);
  }

  public void saveDirectForm() {
    if (com.qicau.qa.support.Config.android()) {
      // On a real phone the first tap after typing only dismisses the soft keyboard and the layout shifts,
      // so the tap misses the button. Blur the focused input first, then give the layout a moment.
      ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
          "if (document.activeElement) { document.activeElement.blur(); }");
      try {
        Thread.sleep(700);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    wait.until(ExpectedConditions.elementToBeClickable(SAVE_TRANSACTION_BUTTON)).click();
  }

  /** Convenience for scenarios that just need *a* transaction to exist (HOME/HIST/MON/SAVE). */
  public void quickSaveWithPriceOnly(String price) {
    open(driver);
    switchToFormulirLangsung();
    setPrice(price);
    saveDirectForm();
    try {
      wait.until(ExpectedConditions.stalenessOf(driver.findElement(By.xpath("//h3[contains(text(), 'Input Manual')]"))));
    } catch (org.openqa.selenium.NoSuchElementException alreadyClosed) {
      // fine - the modal was already gone by the time we checked
    }
  }

  // ---- Sprint 3: MAN-19 counters in the direct form ----

  public WebElement counterFor(String labelContains) {
    return driver.findElement(By.xpath("//label[contains(text(), '" + labelContains + "')]/following-sibling::span[1]"));
  }

  public WebElement platformInputElement() {
    return driver.findElement(PLATFORM_INPUT);
  }

  public WebElement noteInputElement() {
    return driver.findElement(NOTE_INPUT);
  }

  /** True if the Formulir Langsung <form> carries the novalidate attribute (no browser-native validation bubbles). */
  public boolean directFormIsNoValidate() {
    Object v = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
        "var i = arguments[0]; var f = i.closest('form'); return f ? f.noValidate : null;", driver.findElement(PRICE_INPUT));
    return Boolean.TRUE.equals(v);
  }

  // ---- MAN-13 (spec 2026-10-06): inline price error instead of an alert ----

  public WebElement priceInputElement() {
    return driver.findElement(PRICE_INPUT);
  }

  public java.util.List<WebElement> priceErrorMessages() {
    return driver.findElements(By.xpath(
        "//p[contains(normalize-space(.), 'Jumlah pengeluaran wajib diisi.')]"
            + " | //span[contains(normalize-space(.), 'Jumlah pengeluaran wajib diisi.')]"
            + " | //div[not(.//div) and contains(normalize-space(.), 'Jumlah pengeluaran wajib diisi.')]"));
  }

  public boolean hasNativeAlertOpen() {
    try {
      driver.switchTo().alert();
      return true;
    } catch (org.openqa.selenium.NoAlertPresentException e) {
      return false;
    }
  }
}
