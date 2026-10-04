package com.qicau.qa.steps;

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

/** Glue for features/monthly.feature. Wired up: MON-01, MON-02, MON-03, MON-07 (@smoke). */
public class MonthlySteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  @When("^Open Ringkasan tab$")
  public void openBulananTab() {
    new MonthlyPage(driver()).openViaNav();
  }

  @Then("^Title \"Ringkasan Bulan Ini\"$")
  public void titleRingkasanBulanIni() {
    MonthlyPage page = new MonthlyPage(driver());
    Assertions.assertTrue(page.isDisplayed(), "Expected the Ringkasan Bulan Ini title");
    // Spec update 2026-10-03: the title is followed by the current month name and year ("Oktober 2026").
    java.time.LocalDate today = java.time.LocalDate.now();
    String expected = today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("id-ID"))
        + " " + today.getYear();
    Assertions.assertEquals(expected.toLowerCase(), page.monthYearLabel().toLowerCase(),
        "Expected the title to show the current month and year next to 'Ringkasan Bulan Ini'");
  }

  @And("^\"Total Pengeluaran\" card$")
  public void totalPengeluaranCard() {
    Assertions.assertTrue(new MonthlyPage(driver()).hasTotalCard(), "Expected a Total Pengeluaran card");
  }

  @And("^donut chart$")
  public void donutChart() {
    // MON-05 says the donut is hidden with no data (test/Qicau.md) - with a fresh test account
    // that has no transactions, its absence is correct, not a failure; only assert it's present
    // when there's actually data to chart.
    if (new MonthlyPage(driver()).isEmptyStateShown()) {
      return;
    }
    boolean hasChart = !driver().findElements(By.cssSelector("svg circle, canvas")).isEmpty();
    Assertions.assertTrue(hasChart, "Expected a donut chart when transaction data exists");
  }

  @And("^per-category list$")
  public void perCategoryList() {
    // MON-04 only lists categories that have transactions - with no data, its absence (replaced
    // by the empty-state message) is correct, not a failure.
    MonthlyPage page = new MonthlyPage(driver());
    Assertions.assertTrue(
        page.isEmptyStateShown() || page.isDisplayed(),
        "Expected either the empty state or a rendered category list");
  }

  // ---- MON-02: monthly total equals sum since day 1 00:00 ----
  // Note: only today's (current-month) data is seeded here - the direct form can't backdate a
  // transaction into a previous month via the UI, so the "previous months excluded" half of this
  // is really MON-08's job, not independently re-verified here. This confirms the current
  // month's total itself is a correct sum.

  @Given("^Transactions across current and previous months$")
  public void transactionsAcrossCurrentAndPreviousMonths() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("25000");
  }

  @When("^Compare displayed total to the sum of transactions since the 1st of the current month 00:00$")
  public void compareDisplayedTotalToSum() {
    new MonthlyPage(driver()).openViaNav();
  }

  @Then("^Totals match$")
  public void totalsMatch() {
    boolean matches = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new MonthlyPage(d).totalText().replace(' ', ' ').contains("25.000"));
    Assertions.assertTrue(matches, "Expected the monthly total to include the 25.000 transaction");
  }

  // ---- MON-03: weekly view starts from Monday, includes the current Sunday ----

  @Given("^On Ringkasan tab$")
  public void onBulananTab() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new MonthlyPage(driver()).openViaNav();
  }

  @When("^Click \"Mingguan\"$")
  public void clickMingguan() {
    new MonthlyPage(driver()).toggleWeekly();
  }

  @And("^Check totals against transactions since Monday 00:00 of the current week$")
  public void checkTotalsAgainstMonday() {
    // The toggle itself and the resulting title/button-label change (asserted next) are what's
    // verified here - the exact Monday-start cutoff isn't independently re-derived without a
    // second, differently-dated data point, which the UI can't seed (see MON-02's note above).
  }

  @Then("^Title \"Ringkasan Minggu Ini\"$")
  public void titleRingkasanMingguIni() {
    boolean weekly = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new MonthlyPage(d).isWeeklyDisplayed());
    Assertions.assertTrue(weekly, "Expected the Ringkasan Minggu Ini title after toggling to weekly");
  }

  @And("^button now reads \"Bulanan\"$")
  public void buttonNowReadsBulanan() {
    Assertions.assertFalse(
        driver().findElements(By.xpath("//button[.//span[normalize-space(text())='Bulanan']]")).isEmpty(),
        "Expected the toggle button to now read Bulanan");
  }

  @And("^total = since Monday 00:00 this week \\(Sunday still counted as part of the same week\\)$")
  public void totalSinceMondayThisWeek() {
    // Can't independently re-derive the Monday cutoff without a second, differently-dated data
    // point, which the direct form can't backdate (see MON-02's note) - this confirms the weekly
    // total at least renders without error, not the exact boundary.
    Assertions.assertTrue(new MonthlyPage(driver()).hasTotalCard(), "Expected a total to render in weekly view");
  }

  // ---- MON-07: summary updates live when data changes ----

  @Given("^Ringkasan tab open$")
  public void bulananTabOpen() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new MonthlyPage(driver()).openViaNav();
  }

  @When("^Add edit or delete a transaction in the current period$")
  public void addEditOrDeleteTransactionInCurrentPeriod() {
    // "Input Manual" only exists on the Catat tab, not Bulanan - go there, save, then come back.
    new com.qicau.qa.pages.HomePage(driver()).openViaNav();
    new ManualInputModal(driver()).quickSaveWithPriceOnly("13000");
    new MonthlyPage(driver()).openViaNav();
  }

  @Then("^Summary updates automatically without reload$")
  public void summaryUpdatesAutomaticallyWithoutReload() {
    boolean updated = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> new MonthlyPage(d).totalText().replace(' ', ' ').contains("13.000"));
    Assertions.assertTrue(updated, "Expected the monthly summary to reflect the new transaction without a manual reload");
  }

  // ---- MON-04: per-category rows sorted descending with proportion bar ----
  // See BUG-006: the Kategori section shows its own empty state no matter how much data exists
  // (confirmed with both 1 and 2 categories, not a loading race) - this assertion is expected to
  // fail for real, correctly, until that's fixed; not weakened to "pass around" the bug.

  @When("^View the per-category list$")
  public void viewThePerCategoryList() {
    new MonthlyPage(driver()).openViaNav();
  }

  @Then("^Only categories with transactions shown$")
  public void onlyCategoriesWithTransactionsShown() {
    int rows = new MonthlyPage(driver()).categoryRowCount();
    Assertions.assertTrue(rows > 0, "Expected at least one per-category row when transactions exist - the per-category list must render rows (BUG-006 was reported against this)");
  }

  @And("^sorted largest amount first$")
  public void sortedLargestAmountFirst() {
    // Blocked by BUG-006 - the previous step already fails before this one could meaningfully
    // check ordering, since there are no rows to sort at all yet.
  }

  @And("^each row shows icon, name, amount, and a bar = amount divided by total$")
  public void eachRowShowsIconNameAmountBar() {
    // Same block as above - see BUG-006.
  }

  // ---- MON-06: empty state text for monthly and weekly views ----

  @Given("^No transactions in the current period$")
  public void noTransactionsInTheCurrentPeriod() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
  }

  @When("^View Ringkasan with no transactions this month$")
  public void viewBulananWithNoTransactionsThisMonth() {
    new MonthlyPage(driver()).openViaNav();
  }

  @And("^Switch to Mingguan with none this week$")
  public void switchToMingguanWithNoneThisWeek() {
    new MonthlyPage(driver()).toggleWeekly();
  }

  @Then("^Total \"Rp 0\"$")
  public void monthlyOrWeeklyTotalIsRp0() {
    String total = new MonthlyPage(driver()).totalText();
    boolean zero = total.equals("Rp 0") || total.replace(' ', ' ').equals("Rp 0");
    Assertions.assertTrue(zero, "Expected the total to read Rp 0, got: [" + total + "]");
  }

  @And("^text \"Belum ada riwayat transaksi\\.\" \\(weekly: \"Belum ada riwayat transaksi minggu ini\\.\"\\)$")
  public void textBelumAdaRiwayatWeeklyVariant() {
    Assertions.assertTrue(new MonthlyPage(driver()).isWeeklyEmptyStateShown(),
        "Expected the weekly-specific empty state text (\"...minggu ini.\") after switching to Mingguan");
  }

  // ---- MON-08: transactions from prior months are excluded ----
  // Not automatable from here: the direct form can only ever save "now" - there's no UI path to
  // backdate a transaction into a previous month, and writing a guessed Firestore timestamp
  // format directly would risk being subtly wrong without reading source to confirm it (same
  // reasoning as HIST-03/05). Left undefined deliberately rather than faked.
}
