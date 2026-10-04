package com.qicau.qa.steps;

import com.qicau.qa.pages.EditTransactionModal;
import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.pages.MonthlyPage;
import com.qicau.qa.pages.SaveToast;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverContext;
import com.qicau.qa.support.DriverFactory;
import com.qicau.qa.support.NetworkSimulator;
import com.qicau.qa.support.SignInHelper;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Sprint 3 glue for MON-09 (no permission-denied, real totals), MON-10 (consistency after
 * add/edit/delete/undo), MON-11 (not automated) and MON-12 (offline create/edit/delete, then online). */
public class MonthlyConsistencySteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  /** Creates a transaction through the direct form (works online and offline). */
  private void saveDirect(String price, String kategori) {
    ManualInputModal modal = new ManualInputModal(driver());
    ManualInputModal.open(driver());
    modal.switchToFormulirLangsung();
    modal.setPrice(price);
    if (kategori != null) {
      modal.setKategori(kategori);
    }
    modal.saveDirectForm();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> !new ManualInputModal(d).isFormulirLangsungModeActive());
  }

  // ---- MON-09: no permission-denied in the console; totals match the real data ----

  private java.util.List<String> consoleErrors = new java.util.ArrayList<>();
  private long mon09Total;
  private java.util.List<Long> mon09Categories = new java.util.ArrayList<>();

  @Given("^Signed in with existing transactions this month; browser DevTools console open$")
  public void signedInWithExistingTransactions() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    saveDirect("15000", "Makan");
    HomePage home = new HomePage(driver());
    saveDirect("7000", "Transport");
    HistoryEditSteps.dismissSaveToast(driver());
    // reload so Ringkasan is opened from a cold start, as a returning user would
    driver().get(Config.baseUrl());
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
  }

  @When("^Open the Ringkasan tab$")
  public void openTheRingkasanTab() {
    new MonthlyPage(driver()).openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> new MonthlyPage(d).isDisplayed());
    try {
      new WebDriverWait(driver(), Duration.ofSeconds(8)).until((d) -> new MonthlyPage(d).totalValue() > 0);
    } catch (org.openqa.selenium.TimeoutException ignored) {
      // asserted below with the observed value
    }
  }

  @And("^Check the console$")
  public void checkTheConsole() {
    consoleErrors.clear();
    for (LogEntry e : driver().manage().logs().get(LogType.BROWSER)) {
      String m = e.getMessage();
      if (m.toLowerCase().contains("permission-denied") || m.toLowerCase().contains("missing or insufficient permissions")) {
        consoleErrors.add(e.getLevel() + " " + m);
      }
    }
  }

  @And("^Compare totals to the transactions$")
  public void compareTotalsToTheTransactions() {
    MonthlyPage page = new MonthlyPage(driver());
    mon09Total = page.totalValue();
    mon09Categories = page.categoryAmounts();
  }

  @Then("^No \"permission-denied\" error in the console$")
  public void noPermissionDeniedInConsole() {
    Assertions.assertTrue(consoleErrors.isEmpty(), "permission-denied seen in the browser console: " + consoleErrors);
  }

  @And("^total and categories match real data, not a wrong Rp 0$")
  public void totalAndCategoriesMatchRealData() {
    Assertions.assertEquals(22000L, mon09Total, "Ringkasan total must equal the 15.000 + 7.000 transactions, not Rp 0");
    Assertions.assertEquals(java.util.List.of(15000L, 7000L), mon09Categories, "Expected Makan 15.000 then Transport 7.000 (largest first)");
  }

  // ---- MON-10: consistency after add / edit / undo / delete ----

  private final java.util.List<String> mon10Problems = new java.util.ArrayList<>();
  private long expectedTotal;
  private boolean mon10RebuildControlSeen;

  /** Opens Ringkasan and checks total == expected, sum(category rows) == total, donut has one slice per row. */
  private void checkpoint(String stepName, long expected) {
    WebDriver d = driver();
    MonthlyPage page = new MonthlyPage(d);
    page.openViaNav();
    try {
      new WebDriverWait(d, Duration.ofSeconds(8)).until((x) -> {
        MonthlyPage m = new MonthlyPage(x);
        return m.totalValue() == expected && m.categoryAmountsSum() == expected;
      });
    } catch (org.openqa.selenium.TimeoutException e) {
      // fall through - recorded below with the observed values
    }
    long total = page.totalValue();
    long sum = page.categoryAmountsSum();
    int rows = page.categoryAmounts().size();
    int slices = page.donutSectorCount();
    for (int i = 0; i < 20 && slices == 0 && expected > 0; i++) { // chart animates in
      try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      slices = page.donutSectorCount();
    }
    if (total != expected) {
      mon10Problems.add(stepName + ": total " + total + ", expected " + expected);
    }
    if (sum != total) {
      mon10Problems.add(stepName + ": sum of category rows " + sum + " != total " + total + " " + page.categoryNames() + page.categoryAmounts());
    }
    long nonZeroRows = page.categoryAmounts().stream().filter((v) -> v > 0).count();
    if (expected > 0 && slices != nonZeroRows) {
      mon10Problems.add(stepName + ": donut slices " + slices + " != non-zero category rows " + nonZeroRows);
    }
    if (rows != nonZeroRows) {
      mon10Problems.add(stepName + ": " + (rows - nonZeroRows) + " category row(s) with Rp 0 still listed (MON-04: only categories with transactions) " + page.categoryNames() + page.categoryAmounts());
    }
    if (page.hasRebuildControl()) {
      mon10RebuildControlSeen = true;
    }
  }

  @Given("^Ringkasan tab reachable; some transactions this month$")
  public void ringkasanReachableWithTransactions() {
    mon10Problems.clear();
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    saveDirect("15000", "Makan");   // A
    saveDirect("7000", "Transport"); // B
    HistoryEditSteps.dismissSaveToast(driver());
    expectedTotal = 22000;
  }

  @When("^Add a transaction$")
  public void addATransaction() {
    new HomePage(driver()).openViaNav();
    saveDirect("11000", "Makan"); // C
    HistoryEditSteps.dismissSaveToast(driver());
    expectedTotal += 11000;
    checkpoint("after add", expectedTotal);
  }

  @And("^Edit its amount and category$")
  public void editItsAmountAndCategory() {
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> history.transactionRowCount() >= 3);
    history.clickRowWithAmount("-7.000"); // B: Transport 7.000 -> Belanja 9.000
    history.waitForDetailModal();
    history.clickEditInDetailModal();
    EditTransactionModal modal = new EditTransactionModal(driver());
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> modal.isOpen());
    modal.setPlatform("Edited MON10");
    modal.setHarga("9000");
    modal.setKategori("Belanja");
    modal.save();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> !new EditTransactionModal(d).isOpen());
    expectedTotal += 2000;
    checkpoint("after edit", expectedTotal);
  }

  @And("^Undo a fresh one from the save toast$")
  public void undoAFreshOneFromTheSaveToast() {
    new HomePage(driver()).openViaNav();
    ManualInputModal.open(driver());
    ManualInputModal modal = new ManualInputModal(driver());
    modal.switchToFormulirLangsung();
    modal.setPrice("5000");
    modal.saveDirectForm();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> new SaveToast(d).isDisplayed());
    new SaveToast(driver()).clickUndo();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> !new SaveToast(d).isDisplayed());
    checkpoint("after undo", expectedTotal);
  }

  @And("^Delete another from Riwayat$")
  public void deleteAnotherFromRiwayat() {
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> history.transactionRowCount() >= 3);
    history.clickRowWithAmount("-15.000"); // A
    history.waitForDetailModal();
    history.clickDeleteInDetailModal();
    history.confirmDelete();
    expectedTotal -= 15000;
    checkpoint("after delete", expectedTotal);
  }

  @And("^After each step open Ringkasan$")
  public void afterEachStepOpenRingkasan() {
    // done inside each action step (checkpoint) - see above
  }

  @And("^Look for any \"rebuild\"/manual repair control$")
  public void lookForAnyRebuildControl() {
    // recorded by every checkpoint
  }

  @Then("^After every step total, donut and per-category rows all agree with each other \\(sum of category rows = total\\)$")
  public void afterEveryStepAllAgree() {
    Assertions.assertTrue(mon10Problems.isEmpty(), "Ringkasan inconsistencies: " + mon10Problems);
  }

  @And("^no button or manual step to rebuild the summary exists$")
  public void noRebuildControlExists() {
    Assertions.assertFalse(mon10RebuildControlSeen, "A manual rebuild/repair control was found on Ringkasan (removed in the Part 3 spec)");
  }

  // ---- MON-11: not automatable ----
  // Needs a day whose stored daily summary disagrees with its transactions (legacy data). The only
  // way to create that from a test is to write the summary document directly in the emulator, which
  // requires knowing its collection name and field layout - app internals the blind-testing rule
  // forbids reading or guessing. Left undefined on purpose (excluded with @MON-11 in the run commands).

  // ---- MON-12: offline create/edit/delete, then online ----

  private String mon12Email;
  private final java.util.List<String> mon12Problems = new java.util.ArrayList<>();
  private long mon12RiwayatSum;
  private long mon12RingkasanTotal;
  private long mon12LocalOfflineTotal = -1;
  static boolean mon12DeleteDialogStuckOffline;
  private long mon12FinalTotalSessionA;

  @Given("^Direct form usable offline; known totals$")
  public void directFormUsableOfflineKnownTotals() {
    mon12Email = "mon12+" + System.nanoTime() + "@example.com";
    driver().get(Config.baseUrl());
    SignInHelper.signInAsNewUserWithEmail(driver(), mon12Email);
    saveDirect("10000", "Makan"); // A, created online (known starting total)
    HistoryEditSteps.dismissSaveToast(driver());
    // let the online write reach the server before going offline
    checkpointQuiet(10000);
  }

  private void checkpointQuiet(long expected) {
    MonthlyPage page = new MonthlyPage(driver());
    page.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(8)).until((x) -> new MonthlyPage(x).totalValue() == expected);
    new HomePage(driver()).openViaNav();
  }

  // "Go offline" is defined in PwaSteps.

  @And("^Create, edit and delete transactions via the direct form / Riwayat$")
  public void createEditDeleteOffline() {
    saveDirect("20000", "Makan");   // B
    HistoryEditSteps.dismissSaveToast(driver());
    saveDirect("5000", "Transport"); // C
    HistoryEditSteps.dismissSaveToast(driver());
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    try {
      new WebDriverWait(driver(), Duration.ofSeconds(8)).until((d) -> history.rowAmounts().containsAll(java.util.List.of("-20.000", "-5.000")));
      // edit B: 20.000 -> 25.000
      history.clickRowWithAmount("-20.000");
      history.waitForDetailModal();
      history.clickEditInDetailModal();
      EditTransactionModal modal = new EditTransactionModal(driver());
      new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> modal.isOpen());
      modal.setPlatform("Offline Edit");
      modal.setHarga("25000");
      modal.save();
      new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> !new EditTransactionModal(d).isOpen());
      // delete C
      new WebDriverWait(driver(), Duration.ofSeconds(8)).until((d) -> history.rowAmounts().contains("-5.000"));
      history.clickRowWithAmount("-5.000");
      history.waitForDetailModal();
      history.clickDeleteInDetailModal();
      history.confirmDelete();
      new WebDriverWait(driver(), Duration.ofSeconds(8)).until((d) -> !history.rowAmounts().contains("-5.000"));
    } catch (org.openqa.selenium.TimeoutException e) {
      mon12Problems.add("offline Riwayat did not show/allow the offline create/edit/delete: rows=" + history.rowAmounts() + " (" + e.getClass().getSimpleName() + ")");
    }
    try {
      new WebDriverWait(driver(), Duration.ofSeconds(10)).until(
          org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'z-60')]")));
    } catch (org.openqa.selenium.TimeoutException e) {
      var overlays = driver().findElements(By.xpath("//div[contains(@class,'z-60')]"));
      try {
        java.nio.file.Files.writeString(java.nio.file.Path.of("dump", "s3", "13-offline-delete-overlay.html"), driver().getPageSource());
      } catch (java.io.IOException ignored) {
        // evidence only
      }
      mon12DeleteDialogStuckOffline = true;
      NetworkSimulator.goOnline(driver()); // the dialog only closes after the server acknowledges; carry on online
      mon12Problems.add("OBSERVATION offline delete: confirm dialog (spinner, Hapus disabled) stays open and blocks the app until back online; overlay text: " + (overlays.isEmpty() ? "" : overlays.get(0).getText().replace(" ", "|")));
    }
    if (mon12DeleteDialogStuckOffline) {
      new WebDriverWait(driver(), Duration.ofSeconds(20)).until(
          org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'z-60')]")));
    }
    MonthlyPage monthly = new MonthlyPage(driver());
    monthly.openViaNav();
    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    mon12LocalOfflineTotal = monthly.totalValue();
  }

  @And("^Go back online and wait for sync$")
  public void goBackOnlineAndWaitForSync() throws InterruptedException {
    NetworkSimulator.goOnline(driver());
    Thread.sleep(10000);
  }

  @And("^Compare the Ringkasan total with the sum of transactions in Riwayat$")
  public void compareRingkasanWithRiwayatSum() {
    // A fresh second session of the same account reads only server-committed state: what the first
    // session wrote offline has to have been saved (transactions + daily summary together).
    WebDriver b = DriverFactory.create();
    try {
      b.get(Config.baseUrl());
      SignInHelper.signInAsExisting(b, mon12Email);
      HistoryPage history = new HistoryPage(b);
      history.openViaNav();
      try {
        new WebDriverWait(b, Duration.ofSeconds(25)).until((x) -> history.transactionRowCount() >= 2);
        Thread.sleep(1500);
      } catch (org.openqa.selenium.TimeoutException | InterruptedException e) {
        // observed values are asserted in the Then step
      }
      mon12RiwayatSum = history.rowAmounts().stream().mapToLong(MonthlyPage::parseRupiah).sum();
      MonthlyPage monthly = new MonthlyPage(b);
      monthly.openViaNav();
      try {
        new WebDriverWait(b, Duration.ofSeconds(10)).until((x) -> new MonthlyPage(x).totalValue() == mon12RiwayatSum);
      } catch (org.openqa.selenium.TimeoutException e) {
        // observed values asserted below
      }
      mon12RingkasanTotal = monthly.totalValue();
      mon12Problems.add("(session B) Riwayat rows=" + history.rowAmounts() + " Ringkasan total=" + mon12RingkasanTotal + " categories=" + monthly.categoryAmounts());
    } finally {
      b.quit();
    }
    // also the first session itself after reconnecting
    MonthlyPage a = new MonthlyPage(driver());
    a.openViaNav();
    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    mon12FinalTotalSessionA = a.totalValue();
    HistoryPage historyA = new HistoryPage(driver());
    historyA.openViaNav();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    mon12Problems.add("(session A after reconnect) Riwayat rows=" + historyA.rowAmounts() + " Ringkasan total=" + mon12FinalTotalSessionA);
  }

  @Then("^Transactions and daily summary are saved together$")
  public void transactionsAndDailySummarySavedTogether() {
    Assertions.assertEquals(35000L, mon12RiwayatSum, "Server-side Riwayat should hold A 10.000 + B 25.000 (C deleted): " + mon12Problems);
  }

  @And("^final Ringkasan numbers equal the Riwayat sum, no difference left over$")
  public void finalRingkasanEqualsRiwayatSum() {
    Assertions.assertEquals(mon12RiwayatSum, mon12RingkasanTotal, "Ringkasan total differs from the Riwayat sum (session B, server state): " + mon12Problems);
    Assertions.assertEquals(mon12RiwayatSum, mon12FinalTotalSessionA, "Ringkasan in the original session differs from the Riwayat sum after reconnecting (offline-local total was " + mon12LocalOfflineTotal + ")");
  }
}
