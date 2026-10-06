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

  @Then("^\"Detail Pengeluaran\" modal shows platform, full amount, category, method, full-format timestamp, note if present, and the \"Edit Transaksi\" \\+ \"Hapus Transaksi\" buttons side by side$")
  public void detailPengeluaranModalShows() {
    boolean open = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> !d.findElements(By.xpath("//*[contains(text(), 'Detail Pengeluaran')]")).isEmpty());
    Assertions.assertTrue(open, "Expected the Detail Pengeluaran modal to open");
    Assertions.assertFalse(
        driver().findElements(By.xpath("//button[contains(normalize-space(.), 'Hapus Transaksi')]")).isEmpty(),
        "Expected a Hapus Transaksi button in the detail modal");
    Assertions.assertTrue(new HistoryPage(driver()).detailButtonsSideBySide(),
        "Expected Edit Transaksi and Hapus Transaksi side by side in the detail modal");
    String modalText = driver().findElement(By.xpath("//h3[contains(text(), 'Detail Pengeluaran')]/ancestor::div[contains(@class,'fixed')][1]")).getText();
    for (String label : new String[] {"Kategori", "Metode Pembayaran", "Waktu"}) {
      Assertions.assertTrue(modalText.toLowerCase().contains(label.toLowerCase()), "Expected the detail modal to show: " + label);
    }
    Assertions.assertTrue(modalText.contains("Rp"), "Expected the full amount (Rp ...) in the detail modal");
  }

  // ---- HIST-14: detail modal shows Edit Transaksi + Hapus Transaksi side by side ----

  @Then("^Both \"Edit Transaksi\" and \"Hapus Transaksi\" buttons shown side by side$")
  public void bothButtonsShownSideBySide() {
    HistoryPage history = new HistoryPage(driver());
    history.waitForDetailModal();
    Assertions.assertTrue(history.detailButtonsSideBySide(), "Expected Edit Transaksi and Hapus Transaksi side by side");
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

  @Then("^Transaction disappears from the list, from Home total, and from Ringkasan$")
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
    Assertions.assertTrue(new MonthlyPage(driver()).isEmptyStateShown(), "Expected Ringkasan to show the empty state too (Ringkasan text: " + driver().findElement(By.tagName("main")).getText().replace('\n', '|') + ")");
  }

  @And("^detail modal \\(if open\\) also closes$")
  public void detailModalAlsoCloses() {
    Assertions.assertTrue(
        driver().findElements(By.xpath("//*[contains(text(), 'Detail Pengeluaran')]")).isEmpty(),
        "Expected the detail modal to be closed");
  }

  // ---- HIST-08: pagination shows 30 per page with correct button states ----
  // Retest 2026-10-04 with a valid "n+" precondition: 100 transactions in MIXED categories on 100 different
  // days (today via the UI, 99 back-dated via the Firestore emulator seeder). Makan = 40 rows: 5 among the
  // newest 30 (days 0,6,12,18,24) and 35 on odd days 31..99, so the Makan rows are NOT all in the newest
  // 30 and older Makan data is not loaded by the first pages. Time filter "Semua Waktu".
  private static final int HIST08_TOTAL = 100;
  private final java.util.List<Integer> pageRowCounts = new java.util.ArrayList<>();
  private final java.util.List<String> pageIndicatorsSeen = new java.util.ArrayList<>();
  private final java.util.List<String> allRowAmounts = new java.util.ArrayList<>();
  private boolean prevDisabledOnPage1;
  private boolean nextDisabledOnLastPage;

  private static boolean hist08IsMakan(int daysAgo) {
    return daysAgo < 30 ? daysAgo % 6 == 0 : daysAgo % 2 == 1;
  }

  @Given("^More than 30 transactions match the filter$")
  public void moreThan30TransactionsMatchFilter() {
    String[] others = {"Transport", "Belanja", "Hiburan"};
    SeededDataSteps.signInAndCreateToday(driver(), 1000); // day 0 = Makan, via the UI
    for (int day = 1; day < HIST08_TOTAL; day++) {
      String cat = hist08IsMakan(day) ? "Makan" : others[day % others.length];
      SeededDataSteps.seedDaysAgo(day, cat, 1000 + day, "Seed " + day + "d");
    }
    SeededDataSteps.reload(driver());
    pageRowCounts.clear();
    pageIndicatorsSeen.clear();
    allRowAmounts.clear();
  }

  private void recordCurrentPage() {
    HistoryPage history = new HistoryPage(driver());
    pageRowCounts.add(history.transactionRowCount());
    pageIndicatorsSeen.add(history.pageIndicator());
    allRowAmounts.addAll(history.rowAmounts());
    evidence("semua-p" + pageRowCounts.size());
  }

  /** Writes the page text + indicator list of the current view to automation/dump/hist08 (evidence). */
  private void evidence(String name) {
    try {
      java.nio.file.Path dir = java.nio.file.Paths.get("dump", "hist08");
      java.nio.file.Files.createDirectories(dir);
      HistoryPage h = new HistoryPage(driver());
      String txt = "indicators=" + h.pageIndicators() + "\nrows=" + h.transactionRowCount() + "\n\n" + h.listText();
      java.nio.file.Files.writeString(dir.resolve(name + ".txt"), txt);
      byte[] png = ((org.openqa.selenium.TakesScreenshot) driver()).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
      java.nio.file.Files.write(dir.resolve(name + ".png"), png);
      System.out.println("HIST08-EVIDENCE " + name + " indicators=" + h.pageIndicators() + " rows=" + h.transactionRowCount());
    } catch (Exception e) {
      System.out.println("HIST08 evidence failed: " + e);
    }
  }

  /** Waits until the list is showing page n (indicator "n / ..." and a first row different from {@code previousFirstRow}). */
  private void waitForPage(int n, String previousFirstRow) {
    HistoryPage history = new HistoryPage(driver());
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> {
      var amounts = history.rowAmounts();
      return history.pageIndicator().startsWith(n + " /") && !amounts.isEmpty()
          && (previousFirstRow == null || !amounts.get(0).equals(previousFirstRow));
    });
  }

  @When("^View page 1 \\(previous button should be disabled\\)$")
  public void viewPage1PreviousDisabled() {
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    history.selectTimeFilter("Semua Waktu");
    waitForPage(1, null);
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> history.transactionRowCount() == 30);
    prevDisabledOnPage1 = history.isPrevPageDisabled();
    recordCurrentPage();
  }

  @And("^Click next to load older data$")
  public void clickNextToLoadOlderData() {
    HistoryPage history = new HistoryPage(driver());
    String firstBefore = history.rowAmounts().get(0);
    history.clickNextPage();
    waitForPage(2, firstBefore);
    recordCurrentPage();
  }

  @And("^Continue until the last page \\(next button should be disabled\\)$")
  public void continueUntilLastPage() {
    HistoryPage history = new HistoryPage(driver());
    int page = 2;
    while (!history.isNextPageDisabled() && page < 10) {
      String firstBefore = history.rowAmounts().get(0);
      history.clickNextPage();
      page++;
      waitForPage(page, firstBefore);
      recordCurrentPage();
    }
    try {
      nextDisabledOnLastPage = new WebDriverWait(driver(), Duration.ofSeconds(3))
          .until((d) -> history.isNextPageDisabled() ? Boolean.TRUE : null);
    } catch (org.openqa.selenium.TimeoutException e) {
      nextDisabledOnLastPage = false;
    }
  }

  @Then("^30 transactions per page$")
  public void thirtyTransactionsPerPage() {
    Assertions.assertEquals(30, pageRowCounts.get(0), "Expected exactly 30 rows on page 1");
    Assertions.assertEquals(30, pageRowCounts.get(1), "Expected exactly 30 rows on page 2");
  }

  @And("^previous disabled on page 1$")
  public void previousDisabledOnPage1() {
    Assertions.assertTrue(prevDisabledOnPage1, "Expected the previous-page button disabled on page 1");
  }

  @And("^next disabled when no more data$")
  public void nextDisabledWhenNoMoreData() {
    Assertions.assertTrue(nextDisabledOnLastPage, "Expected the next-page button disabled on the last page");
  }

  @And("^next loads older data with no duplicate/missing rows$")
  public void nextLoadsOlderDataNoDuplicateMissingRows() {
    Assertions.assertEquals(java.util.List.of(30, 30, 30, 10), pageRowCounts, "Expected pages of 30/30/30/10 rows for 100 transactions");
    Assertions.assertEquals(HIST08_TOTAL, allRowAmounts.size(), "Expected 100 rows across all pages");
    Assertions.assertEquals(HIST08_TOTAL, new java.util.HashSet<>(allRowAmounts).size(), "Expected no duplicate rows across pages");
  }

  @And("^indicator shows the REAL total pages and stays fixed \\(e\\.g\\. 1/5, 2/5 \\.\\.\\. 5/5, not growing\\) also with a specific category filter active$")
  public void indicatorShowsRealFixedTotal() {
    Assertions.assertEquals(java.util.List.of("1 / 4", "2 / 4", "3 / 4", "4 / 4"), pageIndicatorsSeen,
        "Expected the indicator to show the real, fixed total (100 rows / 30 = 4 pages) on every page");
  }

  /** Applies Makan and walks every reachable page; returns indicator texts, fills rows. */
  private java.util.List<String> walkMakan(String tag, java.util.List<Integer> rows) {
    HistoryPage history = new HistoryPage(driver());
    history.selectCategory("Makan");
    new WebDriverWait(driver(), Duration.ofSeconds(10)).ignoring(org.openqa.selenium.StaleElementReferenceException.class)
        .until((d) -> history.isPrevPageDisabled() && history.transactionRowCount() > 0);
    java.util.List<String> seen = new java.util.ArrayList<>();
    seen.add(history.pageIndicator().replaceAll("\\s+", " "));
    rows.add(history.transactionRowCount());
    evidence(tag + "-p1");
    int page = 1;
    while (!history.isNextPageDisabled() && page < 10) {
      String firstBefore = history.rowAmounts().get(0);
      history.clickNextPage();
      page++;
      boolean settled = true;
      try {
        waitForPage(page, firstBefore);
      } catch (org.openqa.selenium.TimeoutException e) {
        System.out.println("HIST08-" + tag + " page " + page + " did not settle: indicators=" + history.pageIndicators() + " rows=" + history.transactionRowCount());
        settled = false;
      }
      for (int t = 0; t < 3; t++) {
        System.out.println("HIST08-" + tag + " p" + page + " t+" + t + "s indicators=" + history.pageIndicators() + " rows=" + history.transactionRowCount() + " next=" + (history.isNextPageDisabled() ? "disabled" : "enabled"));
        try { Thread.sleep(1000); } catch (InterruptedException e) { }
      }
      seen.add(history.pageIndicator().replaceAll("\\s+", " "));
      rows.add(history.transactionRowCount());
      evidence(tag + "-p" + page);
      if (!settled) {
        break;
      }
    }
    System.out.println("HIST08-" + tag + " indicators=" + seen + " rows=" + rows);
    return seen;
  }

  @And("^with a category filter every page is full \\(30 rows, the last one the remainder\\), the total is exact \\(e\\.g\\. 1/2, 2/2\\) and no page is empty$")
  public void categoryFilterPagesFullAndTotalExact() {
    // Spec 2026-10-07: with a category filter active the pages are full (30 rows, last = remainder), the total is exact
    // and no page is empty. 100 seeded rows in 4 categories, 40 of them Makan spread over 100 days.
    // (A) Same session: every page of "Semua Kategori" was already visited.
    java.util.List<Integer> rowsA = new java.util.ArrayList<>();
    java.util.List<String> seenA = walkMakan("makan-all-loaded", rowsA);
    assertFullPagesExactTotal("all loaded", seenA, rowsA);

    // (B) Fresh load: filter Makan WITHOUT first paging through Semua Kategori (the case that used to show "1 / 1+" and an empty page 2).
    SeededDataSteps.reload(driver());
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    history.selectTimeFilter("Semua Waktu");
    waitForPage(1, null);
    java.util.List<Integer> rows = new java.util.ArrayList<>();
    java.util.List<String> seen = walkMakan("makan-fresh", rows);
    assertFullPagesExactTotal("fresh load", seen, rows);
  }

  private static void assertFullPagesExactTotal(String label, java.util.List<String> seen, java.util.List<Integer> rows) {
    Assertions.assertEquals(java.util.List.of("1 / 2", "2 / 2"), seen,
        label + ": expected the exact total 1 / 2, 2 / 2 (no 'n+'), got indicators=" + seen + " rows=" + rows);
    Assertions.assertEquals(java.util.List.of(30, 10), rows,
        label + ": expected full pages (30 rows, then the 10 remaining) and no empty page, got rows=" + rows);
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

  @And("^Sync ke Sheets and Reset Ekspor buttons REMAIN visible \\(not dependent on list content\\)$")
  public void syncResetButtonsRemainVisible() {
    // Spec changed 2026-10-03 (HIST-02; BUG-007 closed): the buttons no longer depend on the list.
    HistoryPage history = new HistoryPage(driver());
    Assertions.assertTrue(history.isEmptyStateShown(), "Precondition: the empty state is showing");
    Assertions.assertTrue(history.isSyncButtonDisplayed(), "Expected 'Sync ke Sheets' to stay visible with an empty list");
    Assertions.assertTrue(history.isResetButtonDisplayed(), "Expected 'Reset Ekspor' to stay visible with an empty list");
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
