package com.qicau.qa.steps;

import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverContext;
import com.qicau.qa.support.NetworkSimulator;
import com.qicau.qa.support.SignInHelper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Glue for features/sync-dialogs.feature: only the in-app custom confirmation dialogs of Sync / Reset
 * Ekspor (SYNC-15, SYNC-19, SYNC-20). Everything past the dialogs needs real Google OAuth / Sheets /
 * Drive and stays in the manual L5 run (test-cases/sheets-sync.csv). "Lanjut" / "Ya, Reset" are never
 * clicked here, so no Google popup can open and no external call is made.
 * "Click "Batal"" is shared with HIST-12 (HistorySteps) and targets the topmost dialog.
 */
public class SyncDialogSteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  private static final By RESET_DIALOG_TITLE = By.xpath("//h3[contains(text(), 'Reset Status Ekspor?')]");
  private static final By SYNC_FIRST_DIALOG_TITLE = By.xpath("//h3[contains(text(), 'Izinkan Akses Google Sheets')]");
  private static final By YA_RESET = By.xpath("//button[normalize-space(.)='Ya, Reset']");

  private void signInSaveOneAndOpenRiwayat() {
    NetworkSimulator.goOnline(driver());
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("12000");
    HistoryEditSteps.dismissSaveToast(driver());
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> history.transactionRowCount() > 0);
  }

  // ---- SYNC-15 ----

  private boolean customDialogSeen;
  private boolean nativeAlertSeen;
  private int windowsBefore;

  @Given("^Online, at least one transaction, Riwayat tab$")
  public void onlineAtLeastOneTransactionRiwayatTab() {
    signInSaveOneAndOpenRiwayat();
  }

  @When("^Click \"Reset Ekspor\"$")
  public void clickResetEkspor() {
    windowsBefore = driver().getWindowHandles().size();
    new HistoryPage(driver()).clickReset();
    try {
      nativeAlertSeen = false;
      new WebDriverWait(driver(), Duration.ofSeconds(5))
          .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(RESET_DIALOG_TITLE));
      customDialogSeen = true;
    } catch (org.openqa.selenium.UnhandledAlertException e) {
      nativeAlertSeen = true;
    } catch (org.openqa.selenium.TimeoutException e) {
      customDialogSeen = false;
    }
    resetClickedAtNanos = System.nanoTime();
  }

  @And("^In the custom confirmation dialog, click \"Batal\"$")
  public void inTheCustomDialogClickBatal() {
    new HistoryPage(driver()).cancelDelete(); // generic: clicks Batal in the topmost dialog
    new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until(org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(RESET_DIALOG_TITLE));
  }

  @Then("^Custom confirmation dialog \\(not a native popup\\) appears$")
  public void customConfirmationDialogAppears() {
    Assertions.assertFalse(nativeAlertSeen, "A native browser dialog appeared instead of the in-app dialog");
    Assertions.assertTrue(customDialogSeen, "Expected the in-app 'Reset Status Ekspor?' dialog");
  }

  @And("^Batal -> no changes$")
  public void batalNoChanges() {
    Assertions.assertEquals(windowsBefore, driver().getWindowHandles().size(), "Batal must not open any popup window");
    Assertions.assertTrue(driver().findElements(RESET_DIALOG_TITLE).isEmpty(), "Dialog should be closed after Batal");
    Assertions.assertTrue(new HistoryPage(driver()).transactionRowCount() > 0, "The transaction must still be listed");
    Assertions.assertTrue(driver().findElements(By.xpath("//*[contains(text(), 'Berhasil mereset')]")).isEmpty(), "No reset-success toast expected");
  }

  // ---- SYNC-19 ----

  private long resetClickedAtNanos;
  private boolean disabledRightAfterOpen;
  private boolean enabledLater;
  private long enabledAfterMillis = -1;

  @Given("^Online, Riwayat tab$")
  public void onlineRiwayatTab() {
    signInSaveOneAndOpenRiwayat();
  }

  @And("^Immediately try to click \"Ya, Reset\"$")
  public void immediatelyTryToClickYaReset() {
    var buttons = driver().findElements(YA_RESET);
    Assertions.assertFalse(buttons.isEmpty(), "Expected the 'Ya, Reset' button in the dialog");
    var button = buttons.get(0);
    disabledRightAfterOpen = !button.isEnabled() || "true".equals(button.getAttribute("disabled"));
    if (!disabledRightAfterOpen) {
      return; // asserted in the Then step; do not click a live reset button (it would start the Google flow)
    }
    try {
      button.click(); // must have no effect while disabled
    } catch (org.openqa.selenium.WebDriverException ignored) {
      // a disabled button may refuse the click outright - equally fine
    }
  }

  @And("^Wait about 1-2 seconds and observe the button$")
  public void waitAbout1To2SecondsAndObserve() throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(4).toNanos();
    while (System.nanoTime() < deadline) {
      var b = driver().findElement(YA_RESET);
      if (b.isEnabled() && !"true".equals(b.getAttribute("disabled"))) {
        enabledLater = true;
        enabledAfterMillis = (System.nanoTime() - resetClickedAtNanos) / 1_000_000;
        break;
      }
      Thread.sleep(100);
    }
  }

  @Then("^Button disabled for ~1s, then enabled normally \\(test must wait, not click instantly\\)$")
  public void buttonDisabledThenEnabled() {
    Assertions.assertTrue(disabledRightAfterOpen, "'Ya, Reset' must be disabled right after the dialog opens");
    Assertions.assertTrue(enabledLater, "'Ya, Reset' never became enabled within 4s");
    Assertions.assertTrue(enabledAfterMillis >= 500 && enabledAfterMillis <= 3500,
        "Expected the button to enable after roughly 1s, observed " + enabledAfterMillis + " ms after the Reset click");
    // leave without confirming: nothing may have been started by the early click
    new HistoryPage(driver()).cancelDelete();
    Assertions.assertEquals(1, driver().getWindowHandles().size(), "No popup (Google) may have opened");
  }

  // ---- SYNC-20 ----

  @Given("^Online; browser storage cleared \\(never synced on this browser\\)$")
  public void onlineBrowserStorageCleared() {
    signInSaveOneAndOpenRiwayat();
    // a fresh Selenium browser profile has never synced; additionally clear site storage explicitly
    // is NOT done here: it would sign the user out. A new profile per scenario is the cleared state.
  }

  @When("^Click \"Sync ke Sheets\"$")
  public void clickSyncKeSheets() {
    windowsBefore = driver().getWindowHandles().size();
    new HistoryPage(driver()).clickSync();
  }

  private String firstSyncDialogText = "";

  @And("^Read the dialog$")
  public void readTheDialog() {
    var title = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(SYNC_FIRST_DIALOG_TITLE));
    firstSyncDialogText = title.findElement(By.xpath("./ancestor::div[contains(@class,'fixed')][1]")).getText();
  }

  // "Click "Batal"" is defined in HistorySteps (topmost dialog).

  @Then("^Dialog \"Izinkan Akses Google Sheets & Drive\" explains a Google permission popup is coming$")
  public void dialogExplainsPermissionPopup() {
    Assertions.assertTrue(firstSyncDialogText.contains("Izinkan Akses Google Sheets & Drive"), firstSyncDialogText);
    Assertions.assertTrue(firstSyncDialogText.toLowerCase().contains("jendela izin") || firstSyncDialogText.toLowerCase().contains("izin dari google"),
        "Expected the dialog to say a Google permission window will appear, got: " + firstSyncDialogText);
  }

  @And("^Batal -> no process, no Google popup$")
  public void batalNoProcessNoPopup() {
    new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until(org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(SYNC_FIRST_DIALOG_TITLE));
    Assertions.assertEquals(windowsBefore, driver().getWindowHandles().size(), "Batal must not open a Google popup");
    Assertions.assertTrue(driver().findElements(By.xpath("//*[contains(text(), 'Sinkronisasi')]")).isEmpty()
        || driver().findElements(By.xpath("//*[contains(text(), 'Berhasil')]")).isEmpty(), "No sync progress/result expected");
  }
}
