package com.qicau.qa.runners;

import com.qicau.qa.pages.BottomNav;
import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.WelcomePage;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverFactory;
import com.qicau.qa.support.GoogleAuthEmulatorWidget;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * NOT part of the Cucumber suite (surefire's <includes> only picks up CucumberTestRunner) -
 * run explicitly with `mvn test -Dtest=ExplorerTest`. Exploratory tool only: dumps real page
 * source at various app states to a local dump/ folder so glue code can be written against
 * confirmed real markup instead of guesses, without ever opening the app's source. Not a
 * deliverable - delete before considering the automation project "finished".
 */
public class ExplorerTest {

  @Test
  void dumpPageStates() throws Exception {
    Path dumpDir = Path.of("dump");
    Files.createDirectories(dumpDir);

    WebDriver driver = DriverFactory.create();
    try {
      driver.manage().window().setSize(new org.openqa.selenium.Dimension(430, 900));
      driver.get(Config.baseUrl());
      dump(driver, dumpDir, "01-welcome");

      new WelcomePage(driver).clickSignInWithGoogle();
      String email = "qa+" + System.currentTimeMillis() + "@example.com";
      new GoogleAuthEmulatorWidget(driver).signInAsNewFakeUser(email, "Explorer QA");
      new WebDriverWait(driver, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
      dump(driver, dumpDir, "02-home-signed-in");

      // Settings modal (NAV-05/06/07, AUTH-06)
      driver.findElement(By.xpath("//*[@aria-label='Pengaturan' or @title='Pengaturan']")).click();
      Thread.sleep(500);
      dump(driver, dumpDir, "03-settings-modal");
      // close it (try a couple of plausible ways, best effort)
      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Tutup')]"));
      Thread.sleep(300);

      // Input Manual modal - default (online) should be Teks AI mode
      driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Input Manual')]")).click();
      Thread.sleep(500);
      dump(driver, dumpDir, "04-input-manual-teks-ai");

      // switch to Formulir Langsung mode
      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Formulir Langsung')]"));
      Thread.sleep(400);
      dump(driver, dumpDir, "04b-input-manual-formulir-langsung");

      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Batal')]"));
      Thread.sleep(300);

      BottomNav nav = new BottomNav(driver);
      nav.clickRiwayat();
      Thread.sleep(1000);
      dump(driver, dumpDir, "05-riwayat-empty");

      nav.clickRingkasan();
      Thread.sleep(1000);
      dump(driver, dumpDir, "06-ringkasan-empty");

      nav.clickCatat();
      Thread.sleep(500);

      // Save a transaction via the direct form, capture the SAVE-01 toast immediately, then
      // click Edit Transaksi to capture the edit modal too.
      driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Input Manual')]")).click();
      Thread.sleep(400);
      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Formulir Langsung')]"));
      Thread.sleep(300);
      try {
        driver.findElement(By.xpath("//label[contains(text(),'Harga')]/ancestor::div[1]//input")).sendKeys("18000");
      } catch (Exception ignored) {
      }
      try {
        driver.findElement(By.xpath("//button[@type='submit' and contains(., 'Simpan Transaksi')]")).click();
      } catch (Exception ignored) {
      }
      Thread.sleep(200);
      dump(driver, dumpDir, "09-save-toast");

      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Edit Transaksi')]"));
      Thread.sleep(500);
      dump(driver, dumpDir, "10-edit-modal");

      // Edit modal closes via its X icon (h3 "Edit Transaksi" -> following-sibling button), not
      // Tutup/Batal text - close it properly so it doesn't intercept the next clicks.
      tryClick(driver, By.xpath("//h3[contains(text(), 'Edit Transaksi')]/following-sibling::button[1]"));
      Thread.sleep(500);

      // Riwayat with one real transaction, and its detail modal
      nav.clickRiwayat();
      Thread.sleep(1200);
      dump(driver, dumpDir, "11-riwayat-with-data");
      try {
        driver.findElements(By.xpath("//div[starts-with(normalize-space(text()), '-')]")).get(0).click();
        Thread.sleep(500);
        dump(driver, dumpDir, "12-riwayat-detail-modal");
        tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Hapus Transaksi')]"));
        Thread.sleep(500);
        dump(driver, dumpDir, "13-delete-confirm-dialog");
      } catch (Exception e) {
        System.err.println("row click / detail modal capture failed: " + e);
      }
      // Reload rather than trying to click through the still-open delete-confirm dialog above -
      // its cancel button has the same open-animation click-intercept issue as the detail modal's
      // delete button (see HistoryPage.clickDeleteInDetailModal), not worth fighting for an
      // exploration-only script.
      driver.get(Config.baseUrl());
      Thread.sleep(800);

      // History filter dropdowns (HIST-02/06/07)
      nav.clickRiwayat();
      Thread.sleep(500);
      tryClick(driver, By.xpath("//button[.//span[contains(text(), 'Semua Kategori') or contains(text(), 'Kategori')]]"));
      Thread.sleep(400);
      dump(driver, dumpDir, "14-riwayat-category-filter-open");
      tryClick(driver, By.xpath("//button[.//span[contains(text(), 'Semua Kategori') or contains(text(), 'Kategori')]]"));
      Thread.sleep(300);
      tryClick(driver, By.xpath("//button[.//span[contains(text(), 'Hari Terakhir') or contains(text(), 'Semua Waktu') or contains(text(), 'Bulan Ini')]]"));
      Thread.sleep(400);
      dump(driver, dumpDir, "15-riwayat-time-filter-open");

      // A second transaction with a different category, then Bulanan with 2 categories (MON-04/05)
      driver.get(Config.baseUrl());
      Thread.sleep(800);
      nav.clickCatat();
      Thread.sleep(400);
      driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Input Manual')]")).click();
      Thread.sleep(400);
      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Formulir Langsung')]"));
      Thread.sleep(300);
      try {
        driver.findElement(By.xpath("//label[contains(text(),'Kategori')]/ancestor::div[.//input][1]//input")).sendKeys("Transport");
      } catch (Exception ignored) {
      }
      try {
        driver.findElement(By.xpath("//label[contains(text(),'Harga')]/ancestor::div[.//input][1]//input")).sendKeys("50000");
      } catch (Exception ignored) {
      }
      try {
        driver.findElement(By.xpath("//button[@type='submit' and contains(., 'Simpan Transaksi')]")).click();
      } catch (Exception ignored) {
      }
      Thread.sleep(500);
      nav.clickRingkasan();
      Thread.sleep(1200);
      dump(driver, dumpDir, "16-ringkasan-two-categories");
      Thread.sleep(2000);
      dump(driver, dumpDir, "16b-ringkasan-two-categories-after-wait");
      driver.navigate().refresh();
      Thread.sleep(1500);
      dump(driver, dumpDir, "16c-ringkasan-two-categories-after-reload");

      // Reconnect banner (PWA-02)
      com.qicau.qa.support.NetworkSimulator.goOffline(driver);
      Thread.sleep(800);
      com.qicau.qa.support.NetworkSimulator.goOnline(driver);
      Thread.sleep(300);
      dump(driver, dumpDir, "17-reconnect-banner");
      Thread.sleep(4500);
      dump(driver, dumpDir, "18-reconnect-banner-after-4s");

    } finally {
      driver.quit();
    }
  }

  /** Isolated check for the MON-04 per-category list looking empty despite a non-zero total -
   * confirms whether that needs a second, non-default category to reproduce, or happens even
   * with just one (default "Makan") transaction. */
  @Test
  void dumpMonthlySingleCategory() throws Exception {
    Path dumpDir = Path.of("dump");
    Files.createDirectories(dumpDir);
    WebDriver driver = DriverFactory.create();
    try {
      driver.manage().window().setSize(new org.openqa.selenium.Dimension(800, 1000));
      driver.get(Config.baseUrl());
      new WelcomePage(driver).clickSignInWithGoogle();
      String email = "qa+" + System.currentTimeMillis() + "@example.com";
      new GoogleAuthEmulatorWidget(driver).signInAsNewFakeUser(email, "Explorer QA Single Cat");
      new WebDriverWait(driver, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());

      driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Input Manual')]")).click();
      Thread.sleep(400);
      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Formulir Langsung')]"));
      Thread.sleep(300);
      driver.findElement(By.xpath("//label[contains(text(),'Harga')]/ancestor::div[.//input][1]//input")).sendKeys("18000");
      driver.findElement(By.xpath("//button[@type='submit' and contains(., 'Simpan Transaksi')]")).click();
      Thread.sleep(600);

      new BottomNav(driver).clickRingkasan();
      Thread.sleep(1500);
      dump(driver, dumpDir, "19-ringkasan-single-default-category");
    } finally {
      driver.quit();
    }
  }

  /** Checks whether the Auth Emulator widget's account picker lists a previously-created fake
   * account (server-side, not tied to any one browser profile) so a second independent
   * WebDriver session could sign in as the SAME user - needed for HOME-07/HIST-13 (realtime sync
   * across two sessions). */
  @Test
  void dumpAuthWidgetExistingAccountPicker() throws Exception {
    Path dumpDir = Path.of("dump");
    Files.createDirectories(dumpDir);
    String sharedEmail = "sync-test@example.com";

    WebDriver driver1 = DriverFactory.create();
    try {
      driver1.get(Config.baseUrl());
      new WelcomePage(driver1).clickSignInWithGoogle();
      new GoogleAuthEmulatorWidget(driver1).signInAsNewFakeUser(sharedEmail, "Sync Test User");
      new WebDriverWait(driver1, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
    } finally {
      driver1.quit();
    }

    WebDriver driver2 = DriverFactory.create();
    try {
      driver2.get(Config.baseUrl());
      new WelcomePage(driver2).clickSignInWithGoogle();
      new WebDriverWait(driver2, Duration.ofSeconds(10)).until((d) -> d.getWindowHandles().size() > 1);
      String main = driver2.getWindowHandle();
      driver2.getWindowHandles().stream().filter((h) -> !h.equals(main)).findFirst()
          .ifPresent((h) -> driver2.switchTo().window(h));
      Thread.sleep(500);
      dump(driver2, dumpDir, "20-auth-widget-account-picker");
    } finally {
      driver2.quit();
    }
  }

  /** Checks the Kategori type-ahead combobox's real suggestion-list markup (needed to fix
   * HIST-06/SAVE-07's flaky typed+Tab category selection). */
  @Test
  void dumpKategoriTypeahead() throws Exception {
    Path dumpDir = Path.of("dump");
    Files.createDirectories(dumpDir);
    WebDriver driver = DriverFactory.create();
    try {
      driver.get(Config.baseUrl());
      new WelcomePage(driver).clickSignInWithGoogle();
      new GoogleAuthEmulatorWidget(driver).signInAsNewFakeUser("qa+" + System.currentTimeMillis() + "@example.com", "Typeahead QA");
      new WebDriverWait(driver, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
      driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Input Manual')]")).click();
      Thread.sleep(400);
      tryClick(driver, By.xpath("//button[contains(normalize-space(.), 'Formulir Langsung')]"));
      Thread.sleep(300);
      var kategoriInput = driver.findElement(By.xpath("//label[contains(text(),'Kategori')]/ancestor::div[.//input][1]//input"));
      kategoriInput.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
      kategoriInput.sendKeys(org.openqa.selenium.Keys.DELETE);
      kategoriInput.sendKeys("Transport");
      Thread.sleep(400);
      dump(driver, dumpDir, "21-kategori-typeahead-open");
      System.err.println("Kategori input value after keyboard-clear+type: [" + kategoriInput.getAttribute("value") + "]");
      kategoriInput.sendKeys(org.openqa.selenium.Keys.TAB);
      Thread.sleep(300);
      System.err.println("Kategori input value after TAB: [" + kategoriInput.getAttribute("value") + "]");
    } finally {
      driver.quit();
    }
  }

  /** Checks the char counter's actual rendered color/class at 500/500 (MAN-02). */
  @Test
  void dumpCharCounterAt500() throws Exception {
    Path dumpDir = Path.of("dump");
    Files.createDirectories(dumpDir);
    WebDriver driver = DriverFactory.create();
    try {
      driver.get(Config.baseUrl());
      new WelcomePage(driver).clickSignInWithGoogle();
      new GoogleAuthEmulatorWidget(driver).signInAsNewFakeUser("qa+" + System.currentTimeMillis() + "@example.com", "Counter QA");
      new WebDriverWait(driver, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
      driver.findElement(By.xpath("//button[contains(normalize-space(.), 'Input Manual')]")).click();
      Thread.sleep(400);
      driver.findElement(By.cssSelector("textarea[maxlength='500']")).sendKeys("a".repeat(520));
      Thread.sleep(300);
      var counter = driver.findElement(By.xpath("//span[contains(normalize-space(.), '/500')]"));
      System.err.println("counter text=[" + counter.getText() + "] class=[" + counter.getAttribute("class") + "] color=[" + counter.getCssValue("color") + "]");
      dump(driver, dumpDir, "22-char-counter-500");
    } finally {
      driver.quit();
    }
  }

  /** Checks whether quickSaveWithPriceOnly's loop double-saves under repeated rapid calls
   * (suspected cause of HIST-08's page-2 row count coming back as 30 instead of 1). */
  @Test
  void dumpRepeatedQuickSaveCount() throws Exception {
    WebDriver driver = DriverFactory.create();
    try {
      driver.get(Config.baseUrl());
      new WelcomePage(driver).clickSignInWithGoogle();
      new GoogleAuthEmulatorWidget(driver).signInAsNewFakeUser("qa+" + System.currentTimeMillis() + "@example.com", "Repeat QA");
      new WebDriverWait(driver, Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
      com.qicau.qa.pages.ManualInputModal modal = new com.qicau.qa.pages.ManualInputModal(driver);
      for (int i = 1; i <= 5; i++) {
        modal.quickSaveWithPriceOnly(String.valueOf(1000 + i));
      }
      com.qicau.qa.pages.HistoryPage history = new com.qicau.qa.pages.HistoryPage(driver);
      history.openViaNav();
      Thread.sleep(1500);
      System.err.println("Row count after 5 quickSaveWithPriceOnly calls: " + history.transactionRowCount());
    } finally {
      driver.quit();
    }
  }

  private void tryClick(WebDriver driver, By by) {
    try {
      driver.findElement(by).click();
    } catch (Exception ignored) {
      // best-effort only, this is exploration
    }
  }

  private void dump(WebDriver driver, Path dir, String name) {
    try {
      Files.writeString(dir.resolve(name + ".html"), driver.getPageSource());
    } catch (Exception e) {
      System.err.println("dump failed for " + name + ": " + e);
    }
  }
}
