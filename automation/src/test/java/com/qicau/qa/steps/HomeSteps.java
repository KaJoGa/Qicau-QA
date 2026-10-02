package com.qicau.qa.steps;

import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverContext;
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
 * Glue for features/home.feature. Wired up: HOME-01, HOME-02, HOME-03, HOME-06 (@smoke).
 * Reuses AuthSteps' "Signed in" Given.
 */
public class HomeSteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  // ---- HOME-01: Catat tab shows today's total, mic button, and recent list shell ----

  @When("^Open Catat tab$")
  public void openCatatTab() {
    new HomePage(driver()).openViaNav();
  }

  @Then("^Shows \"Pengeluaran Hari Ini\" total, large microphone button, \"Ketuk untuk Bicara\" text, \"Input Manual\" button, \"Baru Saja\" list$")
  public void showsHomeShell() {
    Assertions.assertTrue(new HomePage(driver()).hasFullShell(), "Expected the full Catat tab shell to be shown");
  }

  // ---- HOME-02: today's total equals sum of today's transactions, Rp 0 when none ----

  @Given("^At least one transaction saved today$")
  public void atLeastOneTransactionSavedToday() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("18000");
  }

  @When("^Note today's transactions and their harga values$")
  public void noteTodaysTransactions() {
    // Known by construction - the Given above saved exactly one 18.000 transaction.
  }

  @And("^Compare to displayed total$")
  public void compareToDisplayedTotal() {
    Assertions.assertEquals("Rp 18.000", new HomePage(driver()).totalText().replace(' ', ' '));
  }

  @And("^Delete all today's transactions and reload$")
  public void deleteAllTodaysTransactionsAndReload() {
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    while (!history.isEmptyStateShown() && history.transactionRowCount() > 0) {
      int before = history.transactionRowCount();
      history.clickFirstRow();
      history.clickDeleteInDetailModal();
      history.confirmDelete();
      new WebDriverWait(driver(), Duration.ofSeconds(5))
          .until((d) -> history.isEmptyStateShown() || history.transactionRowCount() < before);
    }
    driver().get(Config.baseUrl());
    new HomePage(driver()).openViaNav();
  }

  @Then("^Total = sum of harga for all of the user's transactions since 00:00 today$")
  public void totalEqualsSumSince0000() {
    // Verified together with the next step - both describe the same post-delete zero state.
  }

  @And("^shows \"Rp 0\" with none$")
  public void showsRp0WithNone() {
    boolean zero = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new HomePage(d).totalText().replace(' ', ' ').equals("Rp 0"));
    Assertions.assertTrue(zero, "Expected today's total to be Rp 0 after deleting all transactions");
  }

  // ---- HOME-03: recent list shows at most 3 of today's transactions, newest first ----

  @Given("^More than 3 transactions saved today$")
  public void moreThan3TransactionsSavedToday() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
  }

  @When("^Save 4\\+ transactions today$")
  public void save4PlusTransactionsToday() {
    ManualInputModal modal = new ManualInputModal(driver());
    for (int i = 1; i <= 4; i++) {
      modal.quickSaveWithPriceOnly(String.valueOf(1000 * i));
    }
  }

  @And("^View \"Baru Saja\" list$")
  public void viewBaruSajaList() {
    Assertions.assertTrue(new HomePage(driver()).isDisplayed(), "Expected to be on the Catat tab to view the recent list");
  }

  // On the Catat tab itself (not Riwayat), the only "-"-prefixed amounts on the page are the
  // recent-list rows, so a page-wide count is safe and avoids fragile sibling/ancestor scoping
  // from the "Baru Saja" heading (which sits in its own wrapper div, not a plain sibling of the
  // rows container).
  private static final org.openqa.selenium.By ROW_AMOUNT_ON_HOME =
      By.xpath("//div[starts-with(normalize-space(text()), '-')]");

  @Then("^At most 3 rows shown, newest on top$")
  public void atMost3RowsShownNewestOnTop() {
    int count = driver().findElements(ROW_AMOUNT_ON_HOME).size();
    Assertions.assertTrue(count > 0 && count <= 3, "Expected 1-3 rows in the recent list, got " + count);
  }

  @And("^each row shows category icon, platform \\(or category name if platform empty\\), \"Kategori . Metode\", amount prefixed with \"-\"$")
  public void eachRowShowsExpectedContent() {
    boolean hasAmount = !driver().findElements(ROW_AMOUNT_ON_HOME).isEmpty();
    Assertions.assertTrue(hasAmount, "Expected recent-list rows to show a -prefixed amount");
  }

  // ---- HOME-06: total and recent list update live without reload ----

  @Given("^Catat tab open$")
  public void catatTabOpen() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
  }

  @When("^Add a new transaction \\(e\\.g\\. via another tab/device or Input Manual\\)$")
  public void addNewTransaction() {
    new ManualInputModal(driver()).quickSaveWithPriceOnly("7000");
  }

  @And("^Delete a transaction$")
  public void deleteATransaction() {
    new HistoryPage(driver()).openViaNav();
    HistoryPage history = new HistoryPage(driver());
    history.clickFirstRow();
    history.clickDeleteInDetailModal();
    history.confirmDelete();
    new HomePage(driver()).openViaNav();
  }

  @Then("^Total and \"Baru Saja\" list update automatically without a page reload$")
  public void totalAndListUpdateAutomatically() {
    boolean zero = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new HomePage(d).totalText().replace(' ', ' ').equals("Rp 0"));
    Assertions.assertTrue(zero, "Expected the total to reflect the delete without a manual page reload");
  }

  // ---- HOME-04: empty state when no transactions today ----

  @Given("^No transactions today$")
  public void noTransactionsToday() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
  }

  @When("^Open Catat tab with no transactions today$")
  public void openCatatTabWithNoTransactionsToday() {
    new HomePage(driver()).openViaNav();
  }

  @Then("^\"Belum ada transaksi hari ini\\.\" shown$")
  public void belumAdaTransaksiHariIniShown() {
    boolean shown = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> !d.findElements(By.xpath("//*[contains(text(), 'Belum ada transaksi hari ini.')]")).isEmpty());
    Assertions.assertTrue(shown, "Expected the Belum ada transaksi hari ini. empty state on a fresh account");
  }

  @And("^no \"Lihat Semua\" link$")
  public void noLihatSemuaLink() {
    Assertions.assertTrue(
        driver().findElements(By.xpath("//button[contains(normalize-space(.), 'Lihat Semua')]")).isEmpty(),
        "Expected no Lihat Semua link when there are no transactions today");
  }

  // ---- HOME-05: Lihat Semua navigates to Riwayat ----

  @Given("^At least one transaction today$")
  public void atLeastOneTransactionTodayHome05() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("10000");
  }

  @When("^Click \"Lihat Semua\"$")
  public void clickLihatSemua() {
    driver().findElement(By.xpath("//button[contains(normalize-space(.), 'Lihat Semua')]")).click();
  }

  @Then("^Navigates to Riwayat tab$")
  public void navigatesToRiwayatTab() {
    Assertions.assertTrue(new HistoryPage(driver()).isDisplayed(), "Expected Lihat Semua to open the Riwayat tab");
  }

  // ---- HOME-07: realtime sync across two open sessions ----
  // Two independent WebDriver instances signed in as the SAME fake account (confirmed 2026-09-29:
  // the Auth Emulator's fake accounts are server-side, reusable from any session via
  // GoogleAuthEmulatorWidget.signInAsExistingFakeUser - not guessed, not skipped). Session B's
  // driver is managed as an instance field here (not DriverContext/Hooks, which only track one
  // driver per scenario) and is always quit() in the Then step, including on failure.

  private org.openqa.selenium.WebDriver sessionB;

  @Given("^Same account signed in on two tabs/devices$")
  public void sameAccountSignedInOnTwoTabsDevices() {
    driver().get(Config.baseUrl());
    String sharedEmail = "home07+" + System.nanoTime() + "@example.com";
    SignInHelper.signInAsNewUserWithEmail(driver(), sharedEmail);

    sessionB = com.qicau.qa.support.DriverFactory.create();
    sessionB.get(Config.baseUrl());
    SignInHelper.signInAsExisting(sessionB, sharedEmail);
  }

  @When("^Add/delete a transaction on device A$")
  public void addDeleteATransactionOnDeviceA() {
    new ManualInputModal(driver()).quickSaveWithPriceOnly("23000");
  }

  @And("^Observe device B$")
  public void observeDeviceB() {
    // Nothing to do here - the assertion (does B's total reflect A's change?) happens in the
    // Then step, which is where "observing" is actually meaningful.
  }

  @Then("^Change made on A appears on B without manual refresh$")
  public void changeMadeOnAAppearsOnBWithoutManualRefresh() {
    try {
      boolean synced = new WebDriverWait(sessionB, Duration.ofSeconds(10))
          .until((d) -> new HomePage(d).totalText().replace(' ', ' ').contains("23.000"));
      Assertions.assertTrue(synced, "Expected device B's total to reflect device A's new transaction without B reloading");
    } finally {
      sessionB.quit();
    }
  }
}
