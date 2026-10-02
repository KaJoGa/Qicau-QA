package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The post-save success toast (SAVE-*). Confirmed 2026-09-28: title line is "Tersimpan: {platform
 * or kategori}", followed by a "Kategori . Rp N" line and a detail line, then "Edit Transaksi"
 * and "Batal" buttons, and a progress bar with a literal 5s CSS animation (SAVE-02's "~5 detik").
 * Renders fast and can auto-dismiss before a later step gets to it - callers should check for it
 * immediately after the action that triggers a save, not after other steps run first.
 */
public class SaveToast {

  // Confirmed 2026-09-29: "Tersimpan" and ":" render as separate text nodes (same class of bug
  // as the /500 counter) - text() only inspects the first node, so normalize-space(.) (the
  // whole element's concatenated text) is used instead.
  private static final By TITLE = By.xpath("//p[starts-with(normalize-space(.), 'Tersimpan:')]");
  private static final By EDIT_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Edit Transaksi')]");
  private static final By UNDO_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Batal')]");
  // Confirmed 2026-09-29 via a real dump: an icon-only lucide-x button, absolutely positioned
  // (top-2 right-2) within the toast's own "fixed ... max-w-md" wrapper - scoped to that wrapper
  // so it can't accidentally match some other close-x button elsewhere on the page.
  private static final By CLOSE_X_BUTTON = By.xpath(
      "//p[starts-with(normalize-space(.), 'Tersimpan:')]/ancestor::div[contains(@class,'fixed')][1]"
          + "//button[.//*[contains(@class,'lucide-x')]]");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public SaveToast(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(5));
  }

  public boolean isDisplayed() {
    return !driver.findElements(TITLE).isEmpty();
  }

  public String titleText() {
    return driver.findElement(TITLE).getText();
  }

  public void clickEditTransaksi() {
    wait.until(ExpectedConditions.elementToBeClickable(EDIT_BUTTON)).click();
  }

  public void clickUndo() {
    wait.until(ExpectedConditions.elementToBeClickable(UNDO_BUTTON)).click();
  }

  public void clickCloseX() {
    wait.until(ExpectedConditions.elementToBeClickable(CLOSE_X_BUTTON)).click();
  }
}
