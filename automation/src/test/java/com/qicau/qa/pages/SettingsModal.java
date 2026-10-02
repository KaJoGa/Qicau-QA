package com.qicau.qa.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** The "Pengaturan" (Settings) modal (NAV-05/06/07, AUTH-06). Text-based locators - see WelcomePage javadoc. */
public class SettingsModal {

  private static final By SETTINGS_ICON = By.xpath("//*[@aria-label='Pengaturan' or @title='Pengaturan']");
  private static final By SIGN_OUT_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Keluar')]");
  private static final By CLOSE_BUTTON = By.xpath("//button[contains(normalize-space(.), 'Tutup')]");
  // Confirmed 2026-09-28: the modal has no role="dialog" attribute - its own heading is the
  // reliable "is it open" signal instead.
  private static final By MODAL_ROOT = By.xpath("//h3[contains(text(), 'Pengaturan')]");
  private static final By THEME_SISTEM = By.xpath("//button[.//span[normalize-space(text())='Sistem']]");
  private static final By THEME_TERANG = By.xpath("//button[.//span[normalize-space(text())='Terang']]");
  private static final By THEME_GELAP = By.xpath("//button[.//span[normalize-space(text())='Gelap']]");
  // Confirmed 2026-09-29: the modal's own full-width install row, not the header's compact
  // "Pasang App" button (same lucide-download icon, different label/size - see automation/dump).
  private static final By INSTALL_ROW = By.xpath("//p[contains(text(), 'Pasang Aplikasi')]");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public SettingsModal(WebDriver driver) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
  }

  public static void open(WebDriver driver) {
    driver.findElement(SETTINGS_ICON).click();
  }

  public boolean isOpen() {
    return !driver.findElements(MODAL_ROOT).isEmpty();
  }

  public boolean hasThemeOptions() {
    return !driver.findElements(THEME_SISTEM).isEmpty()
        && !driver.findElements(THEME_TERANG).isEmpty()
        && !driver.findElements(THEME_GELAP).isEmpty();
  }

  public void selectSistem() {
    wait.until(ExpectedConditions.elementToBeClickable(THEME_SISTEM)).click();
  }

  public void selectTerang() {
    wait.until(ExpectedConditions.elementToBeClickable(THEME_TERANG)).click();
  }

  public void selectGelap() {
    wait.until(ExpectedConditions.elementToBeClickable(THEME_GELAP)).click();
  }

  public boolean hasInstallButton() {
    return !driver.findElements(INSTALL_ROW).isEmpty();
  }

  /** True if either "dark" (Tailwind's dark-mode class toggle) is present on <html>. */
  public boolean isDarkThemeApplied() {
    String classes = driver.findElement(By.tagName("html")).getAttribute("class");
    return classes != null && classes.contains("dark");
  }

  public void clickSignOut() {
    wait.until(ExpectedConditions.elementToBeClickable(SIGN_OUT_BUTTON)).click();
  }

  public void close() {
    wait.until(ExpectedConditions.elementToBeClickable(CLOSE_BUTTON)).click();
  }
}
