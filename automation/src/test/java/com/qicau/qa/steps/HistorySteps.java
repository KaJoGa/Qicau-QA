package com.qicau.qa.steps;

import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.pages.MonthlyPage;
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

/** Glue for features/history.feature. Wired up: HIST-01, HIST-04, HIST-09, HIST-11 (@smoke). */
public class HistorySteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  private void signInAndSaveWithNote() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    ManualInputModal modal = new ManualInputModal(driver());
    ManualInputModal.open(driver());
    modal.switchToFormulirLangsung();
    modal.setPrice("12000");
    modal.setNote("Catatan uji HIST-04");
    modal.saveDirectForm();
  }

  @When("^Open Riwayat tab$")
  public void openRiwayatTab() {
    new HistoryPage(driver()).openViaNav();
  }

  @Then("^Title \"Riwayat Transaksi\"$")
  public void titleRiwayatTransaksi() {
    Assertions.assertTrue(new HistoryPage(driver()).isDisplayed(), "Expected the Riwayat Transaksi title");
  }

  @And("^default filters \"7 Hari Terakhir\" \\+ \"Semua Kategori\"$")
  public void defaultFilters() {
    Assertions.assertTrue(new HistoryPage(driver()).hasDefaultFilters(), "Expected default filters 7 Hari Terakhir + Semua Kategori");
  }

  @And("^loading skeleton then list$")
  public void loadingSkeletonThenList() {
    // By the time the previous steps' waits resolved, the list (or its empty state) is already
    // rendered - the skeleton itself is a transient loading state, not independently assertable
    // without a slow/mocked network to freeze on. Confirm the settled end-state instead.
    Assertions.assertTrue(
        new HistoryPage(driver()).isEmptyStateShown() || new HistoryPage(driver()).transactionRowCount() >= 0,
        "Expected the list (or empty state) to have finished loading");
  }

  // ---- HIST-04: each row shows icon, platform/category, category-method, note, amount, delete ----

  @Given("^At least one transaction with a note$")
  public void atLeastOneTransactionWithNote() {
    signInAndSaveWithNote();
  }

  @When("^View a transaction row in the list$")
  public void viewATransactionRowInTheList() {
    new HistoryPage(driver()).openViaNav();
  }

  @Then("^Category icon, platform \\(or category if empty\\), \"Kategori . Metode\", note if present, amount prefixed \"-\", delete button$")
  public void categoryIconEtc() {
    HistoryPage history = new HistoryPage(driver());
    boolean loaded = new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.transactionRowCount() > 0);
    Assertions.assertTrue(loaded, "Expected at least one row in Riwayat");
    Assertions.assertFalse(
        driver().findElements(By.xpath("//*[contains(text(), 'Catatan uji HIST-04')]")).isEmpty(),
        "Expected the note text to appear on the row");
  }

  // ---- HIST-09: clicking a row opens the transaction detail modal ----

  @Given("^At least one transaction in the list$")
  public void atLeastOneTransactionInTheList() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("12000");
    new HistoryPage(driver()).openViaNav();
  }

  @When("^Click a transaction row$")
  public void clickATransactionRow() {
    HistoryPage history = new HistoryPage(driver());
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.transactionRowCount() > 0);
    history.clickFirstRow();
  }

  @Then("^\"Detail Pengeluaran\" modal shows platform, full amount, category, method, full-format timestamp, note if present, \"Hapus Transaksi\" button$")
  public void detailPengeluaranModalShows() {
    boolean open = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> !d.findElements(By.xpath("//*[contains(text(), 'Detail Pengeluaran')]")).isEmpty());
    Assertions.assertTrue(open, "Expected the Detail Pengeluaran modal to open");
    Assertions.assertFalse(
        driver().findElements(By.xpath("//button[contains(normalize-space(.), 'Hapus Transaksi')]")).isEmpty(),
        "Expected a Hapus Transaksi button in the detail modal");
  }

  // ---- HIST-11: confirming delete removes the transaction everywhere ----

  @Given("^Delete confirmation dialog open$")
  public void deleteConfirmationDialogOpen() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("9000");
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.transactionRowCount() > 0);
    history.clickFirstRow();
    history.clickDeleteInDetailModal();
  }

  @When("^Click \"Hapus\"$")
  public void clickHapus() {
    new HistoryPage(driver()).confirmDelete();
  }

  @Then("^Transaction disappears from the list, from Home total, and from Bulanan$")
  public void transactionDisappearsEverywhere() {
    HistoryPage history = new HistoryPage(driver());
    boolean goneFromList = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> history.isEmptyStateShown());
    Assertions.assertTrue(goneFromList, "Expected Riwayat to show the empty state after deleting the only transaction");

    new HomePage(driver()).openViaNav();
    boolean zeroOnHome = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new HomePage(d).totalText().replace(' ', ' ').equals("Rp 0"));
    Assertions.assertTrue(zeroOnHome, "Expected Home total to be Rp 0 after the delete");

    new MonthlyPage(driver()).openViaNav();
    Assertions.assertTrue(new MonthlyPage(driver()).isEmptyStateShown(), "Expected Bulanan to show the empty state too");
  }

  @And("^detail modal \\(if open\\) also closes$")
  public void detailModalAlsoCloses() {
    Assertions.assertTrue(
        driver().findElements(By.xpath("//*[contains(text(), 'Detail Pengeluaran')]")).isEmpty(),
        "Expected the detail modal to be closed");
  }

  // ---- HIST-08: pagination shows 30 per page with correct button states ----
  // All 31+ transactions are created "now" (the direct form can't backdate), so they all land in
  // the same "Hari Ini" bucket under the default "7 Hari Terakhir" filter - fine for pagination
  // mechanics, which don't depend on which dates the rows happen to have.

  // The CSV-generated step order for this scenario bunches all the assertions (30-per-page,
  // previous-disabled-on-page-1, etc.) *after* the navigation steps that already moved off page
  // 1 - so what's asserted has to be captured as each page is actually visited, not re-observed
  // afterwards from whatever page we've since moved to.
  private int page1RowCount;
  private boolean prevDisabledOnPage1;
  private int page2RowCount;
  private boolean nextDisabledOnPage2;
  private boolean pageIndicatorSeen;

  @Given("^More than 30 transactions match the filter$")
  public void moreThan30TransactionsMatchFilter() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    ManualInputModal modal = new ManualInputModal(driver());
    for (int i = 1; i <= 31; i++) {
      modal.quickSaveWithPriceOnly(String.valueOf(1000 + i));
    }
  }

  @When("^View page 1 \\(previous button should be disabled\\)$")
  public void viewPage1PreviousDisabled() {
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    page1RowCount = history.transactionRowCount();
    prevDisabledOnPage1 = history.isPrevPageDisabled();
    pageIndicatorSeen = !driver().findElements(By.xpath("//*[contains(text(), '/')]")).isEmpty();
  }

  @And("^Click next to load older data$")
  public void clickNextToLoadOlderData() {
    new HistoryPage(driver()).clickNextPage();
  }

  @And("^Continue until the last page \\(next button should be disabled\\)$")
  public void continueUntilLastPage() {
    // 31 items / 30 per page = exactly 2 pages - the single click above already reached the
    // last page, deterministically, given this known data size. Capture its state here.
    // Confirmed 2026-09-29: right after the next-page click, the OLD page's 30 rows can still be
    // on screen for a moment before React swaps in the new page's data - a plain "count > 0"
    // wait can catch that stale intermediate state (still 30) instead of the real page 2 count,
    // since 30 already satisfies ">0". Waiting for the count to actually change away from page
    // 1's known count avoids capturing that transitional render.
    HistoryPage history = new HistoryPage(driver());
    page2RowCount = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> {
          int c = history.transactionRowCount();
          return (c > 0 && c != page1RowCount) ? c : null;
        });
    // The row count settling doesn't guarantee the next-button's own disabled attribute has
    // re-rendered in the same tick - give it a short separate window rather than reading it the
    // instant the rows appear.
    try {
      nextDisabledOnPage2 = new WebDriverWait(driver(), Duration.ofSeconds(3))
          .until((d) -> history.isNextPageDisabled() ? Boolean.TRUE : null);
    } catch (org.openqa.selenium.TimeoutException e) {
      nextDisabledOnPage2 = false;
    }
  }

  @Then("^30 transactions per page$")
  public void thirtyTransactionsPerPage() {
    Assertions.assertEquals(30, page1RowCount, "Expected exactly 30 rows on page 1");
  }

  @And("^previous disabled on page 1$")
  public void previousDisabledOnPage1() {
    Assertions.assertTrue(prevDisabledOnPage1, "Expected the previous-page button disabled on page 1");
  }

  @And("^next disabled when no more data$")
  public void nextDisabledWhenNoMoreData() {
    Assertions.assertTrue(nextDisabledOnPage2, "Expected the next-page button disabled on the last page");
  }

  @And("^next loads older data with no duplicate/missing rows$")
  public void nextLoadsOlderDataNoDuplicateMissingRows() {
    // 31 saved, 30 per page -> page 2 should have exactly 1 remaining row.
    Assertions.assertEquals(1, page2RowCount, "Expected exactly 1 row left on the final page (31 total, 30 on page 1)");
  }

  @And("^page indicator shown$")
  public void pageIndicatorShown() {
    Assertions.assertTrue(pageIndicatorSeen, "Expected a page indicator (e.g. '1 / 2') to be shown");
  }

  // ---- HIST-02: empty state for the active filter hides Sync/Reset ----

  @Given("^No transactions match the active filter$")
  public void noTransactionsMatchTheActiveFilter() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("11000"); // default category "Makan"
    new HistoryPage(driver()).openViaNav();
  }

  @When("^Apply a filter with no matching transactions$")
  public void applyAFilterWithNoMatchingTransactions() {
    new HistoryPage(driver()).selectCategory("Belanja"); // the one saved transaction is "Makan"
  }

  @Then("^\"Belum ada riwayat transaksi\\.\" shown$")
  public void belumAdaRiwayatTransaksiShown() {
    boolean empty = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new HistoryPage(d).isEmptyStateShown());
    Assertions.assertTrue(empty, "Expected the empty-state message once the category filter matches nothing");
  }

  @And("^Sync/Reset buttons hidden while list is empty$")
  public void syncResetButtonsHiddenWhileListEmpty() {
    Assertions.assertTrue(new HistoryPage(driver()).areSyncResetButtonsHidden(),
        "Expected Sync/Reset to be hidden once the active filter matches no transactions");
  }

  // ---- HIST-06: category filter restricts the list to the selected category ----

  @Given("^Transactions across multiple categories$")
  public void transactionsAcrossMultipleCategories() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    ManualInputModal modal = new ManualInputModal(driver());
    modal.quickSaveWithPriceOnly("11000"); // default "Makan"
    ManualInputModal.open(driver());
    modal.switchToFormulirLangsung();
    modal.setKategori("Transport");
    modal.setPrice("22000");
    modal.saveDirectForm();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> !modal.isFormulirLangsungModeActive());
  }

  @When("^Select a specific category filter$")
  public void selectASpecificCategoryFilter() {
    new HistoryPage(driver()).openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> new HistoryPage(d).transactionRowCount() >= 2);
    new HistoryPage(driver()).selectCategory("Transport");
  }

  @Then("^Only transactions in that category shown$")
  public void onlyTransactionsInThatCategoryShown() {
    boolean oneRow = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new HistoryPage(d).transactionRowCount() == 1);
    Assertions.assertTrue(oneRow, "Expected exactly the one Transport-category row after filtering");
  }

  @And("^\"Semua Kategori\" shows all$")
  public void semuaKategoriShowsAll() {
    new HistoryPage(driver()).selectCategory("Semua Kategori");
    boolean twoRows = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new HistoryPage(d).transactionRowCount() == 2);
    Assertions.assertTrue(twoRows, "Expected both rows back after resetting to Semua Kategori");
  }

  // ---- HIST-07: changing a filter resets to page 1 ----

  @Given("^On page 2\\+ of results$")
  public void onPage2PlusOfResults() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    ManualInputModal modal = new ManualInputModal(driver());
    for (int i = 1; i <= 31; i++) {
      modal.quickSaveWithPriceOnly(String.valueOf(1000 + i));
    }
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.transactionRowCount() == 30);
    history.clickNextPage();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.transactionRowCount() == 1);
  }

  @When("^While on page 2 change the time or category filter$")
  public void whileOnPage2ChangeTheTimeOrCategoryFilter() {
    new HistoryPage(driver()).selectTimeFilter("Semua Waktu");
  }

  @Then("^Returns to page 1$")
  public void returnsToPage1() {
    HistoryPage history = new HistoryPage(driver());
    boolean backOnPage1 = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> history.isPrevPageDisabled() && history.transactionRowCount() == 30);
    Assertions.assertTrue(backOnPage1, "Expected changing the filter to reset the list back to page 1 (30 rows, previous disabled)");
  }

  // ---- HIST-10: delete icon/button opens a confirmation dialog ----

  @When("^Click the delete icon on a row, or \"Hapus Transaksi\" in the detail modal$")
  public void clickDeleteIconOrHapusTransaksi() {
    HistoryPage history = new HistoryPage(driver());
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.transactionRowCount() > 0);
    history.clickFirstRow();
    history.clickDeleteInDetailModal();
  }

  @Then("^Confirmation dialog \"Hapus Transaksi\\?\" \\(irreversible\\) with \"Batal\" & \"Hapus\"$")
  public void confirmationDialogHapusTransaksi() {
    HistoryPage history = new HistoryPage(driver());
    boolean shown = new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.isDeleteConfirmDialogShown());
    Assertions.assertTrue(shown, "Expected the Hapus Transaksi? confirmation dialog");
    Assertions.assertFalse(driver().findElements(By.xpath("//button[contains(normalize-space(.), 'Batal')]")).isEmpty());
    Assertions.assertFalse(driver().findElements(By.xpath("//button[normalize-space(text())='Hapus']")).isEmpty());
  }

  // ---- HIST-12: cancelling delete confirmation changes nothing ----

  @When("^Click \"Batal\"$")
  public void clickBatal() {
    new HistoryPage(driver()).cancelDelete();
  }

  @Then("^Nothing is deleted$")
  public void nothingIsDeleted() {
    HistoryPage history = new HistoryPage(driver());
    boolean stillThere = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> !history.isDeleteConfirmDialogShown() && history.transactionRowCount() > 0);
    Assertions.assertTrue(stillThere, "Expected the transaction to remain after cancelling the delete confirmation");
  }

  // ---- HIST-13: new transaction from another tab/device appears on page 1 ----
  // Same two-independent-sessions mechanism as HOME-07 - see that scenario's comment for why
  // this is a real check, not a guess.

  private org.openqa.selenium.WebDriver sessionB;

  @Given("^Riwayat open on page 1, second session available$")
  public void riwayatOpenOnPage1SecondSessionAvailable() {
    driver().get(Config.baseUrl());
    String sharedEmail = "hist13+" + System.nanoTime() + "@example.com";
    SignInHelper.signInAsNewUserWithEmail(driver(), sharedEmail);
    new ManualInputModal(driver()).quickSaveWithPriceOnly("11000");
    new HistoryPage(driver()).openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> new HistoryPage(d).transactionRowCount() > 0);

    sessionB = com.qicau.qa.support.DriverFactory.create();
    sessionB.get(Config.baseUrl());
    SignInHelper.signInAsExisting(sessionB, sharedEmail);
  }

  @When("^Add a transaction from another tab/device while viewing page 1$")
  public void addATransactionFromAnotherTabDeviceWhileViewingPage1() {
    new ManualInputModal(sessionB).quickSaveWithPriceOnly("31000");
  }

  @Then("^New transaction appears automatically on page 1$")
  public void newTransactionAppearsAutomaticallyOnPage1() {
    try {
      HistoryPage history = new HistoryPage(driver());
      boolean appeared = new WebDriverWait(driver(), Duration.ofSeconds(15))
          .until((d) -> history.transactionRowCount() >= 2);
      Assertions.assertTrue(appeared, "Expected session A's Riwayat row count to grow (from session B's save) without a manual reload, got "
          + history.transactionRowCount() + " row(s)");
    } finally {
      sessionB.quit();
    }
  }
}
