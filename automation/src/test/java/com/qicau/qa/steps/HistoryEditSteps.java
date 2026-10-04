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
 * Sprint 3 glue: HIST-05 (partial), HIST-14..HIST-19 (edit a transaction from Riwayat), which
 * reuses the SAVE-05..11 edit modal (spec: same component, same validation rules).
 * Fixture used throughout: one transaction, Platform "Warung Edit", Rp 20.000, Makan, QRIS,
 * note "catatan awal", created "now" (the direct form cannot backdate - see README).
 */
public class HistoryEditSteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  private static final String FIXTURE_PLATFORM = "Warung Edit";
  private static final String FIXTURE_NOTE = "catatan awal";

  /** Signs in, creates the fixture transaction, dismisses its toast. Does not navigate. */
  static void signInAndCreateFixture(WebDriver d, String email) {
    d.get(Config.baseUrl());
    if (email == null) {
      SignInHelper.signIn(d);
    } else {
      SignInHelper.signInAsNewUserWithEmail(d, email);
    }
    ManualInputModal modal = new ManualInputModal(d);
    ManualInputModal.open(d);
    modal.switchToFormulirLangsung();
    modal.setPlatform(FIXTURE_PLATFORM);
    modal.setPrice("20000");
    modal.setNote(FIXTURE_NOTE);
    modal.saveDirectForm();
    new WebDriverWait(d, Duration.ofSeconds(15)).until((x) -> !new ManualInputModal(x).isFormulirLangsungModeActive()); // 15s: saving to a real phone over USB can exceed 5s
    dismissSaveToast(d);
  }

  /** The 5s save toast carries its own Edit Transaksi / Batal buttons - close it so it cannot be hit by mistake. */
  static void dismissSaveToast(WebDriver d) {
    try {
      SaveToast toast = new SaveToast(d);
      if (new WebDriverWait(d, Duration.ofSeconds(3)).until((x) -> toast.isDisplayed())) {
        toast.clickCloseX();
        new WebDriverWait(d, Duration.ofSeconds(3)).until((x) -> !toast.isDisplayed());
      }
    } catch (org.openqa.selenium.TimeoutException ignored) {
      // toast already gone
    }
  }

  static void openDetailForFirstRow(WebDriver d) {
    HistoryPage history = new HistoryPage(d);
    history.openViaNav();
    new WebDriverWait(d, Duration.ofSeconds(10)).until((x) -> history.transactionRowCount() > 0);
    history.clickFirstRow();
    history.waitForDetailModal();
  }

  /** Fixture + Riwayat + detail modal + Edit Transaksi click, ending with the edit modal open. */
  static void openEditModalFromRiwayat(WebDriver d) {
    signInAndCreateFixture(d, null);
    openDetailForFirstRow(d);
    new HistoryPage(d).clickEditInDetailModal();
    EditTransactionModal modal = new EditTransactionModal(d);
    new WebDriverWait(d, Duration.ofSeconds(5)).until((x) -> modal.isOpen());
  }

  // ---- HIST-05: filter options + range boundaries, with back-dated data seeded through the emulator REST API ----
  // Seed (offset in days -> price): today via the UI (1000); 5d 2000; 8d 3000; 25d 4000; 31d 5000; 89d 6000; 91d 7000; 150d 8000.
  // Expected: 7 Hari {1000,2000}; 30 Hari {+3000,4000}; 3 Bulan (90 days back) {+5000,6000}; Semua Waktu = all 8 (7000, 8000 only there).

  private final java.util.Map<String, java.util.List<String>> amountsPerFilter = new java.util.LinkedHashMap<>();
  private boolean optionLabelsOk;
  private String optionLabelProblem = "";

  @Given("^Transactions spanning more than 30 days$")
  public void transactionsSpanningMoreThan30Days() {
    SeededDataSteps.signInCreateTodayAndSeed(driver(), 1000,
        new int[] {5, 8, 25, 31, 89, 91, 150}, new long[] {2000, 3000, 4000, 5000, 6000, 7000, 8000});
    new HistoryPage(driver()).openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((x) -> new HistoryPage(x).transactionRowCount() > 0);
  }

  @When("^Select \"(Semua Waktu|7 Hari Terakhir|30 Hari Terakhir|3 Bulan Terakhir)\"$")
  public void selectTimeFilter(String label) throws InterruptedException {
    HistoryPage history = new HistoryPage(driver());
    if (amountsPerFilter.isEmpty()) {
      history.openTimeFilter();
      StringBuilder problems = new StringBuilder();
      for (String expected : new String[] {"Semua Waktu", "7 Hari Terakhir", "30 Hari Terakhir", "3 Bulan Terakhir"}) {
        if (!history.timeFilterOptionPresent(expected)) {
          problems.append("missing option '").append(expected).append("'; ");
        }
      }
      if (history.timeFilterOptionPresent("Bulan Ini")) {
        problems.append("old option 'Bulan Ini' still offered; ");
      }
      optionLabelProblem = problems.toString();
      optionLabelsOk = optionLabelProblem.isEmpty();
      history.selectTimeFilterFromOpenMenu(label);
    } else {
      history.selectTimeFilter(label);
    }
    Thread.sleep(1500); // let the list reload for the chosen range
    java.util.List<String> amounts = new java.util.ArrayList<>(history.rowAmounts());
    java.util.Collections.sort(amounts);
    amountsPerFilter.put(label, amounts);
  }

  @Then("^Only transactions within the selected range shown each time$")
  public void onlyTransactionsWithinRangeShownEachTime() {
    Assertions.assertTrue(optionLabelsOk, "Time filter options wrong: " + optionLabelProblem);
    Assertions.assertEquals(sorted("-1.000", "-2.000"), amountsPerFilter.get("7 Hari Terakhir"), "7 Hari Terakhir");
    Assertions.assertEquals(sorted("-1.000", "-2.000", "-3.000", "-4.000"), amountsPerFilter.get("30 Hari Terakhir"), "30 Hari Terakhir");
    Assertions.assertEquals(sorted("-1.000", "-2.000", "-3.000", "-4.000", "-5.000", "-6.000"),
        amountsPerFilter.get("3 Bulan Terakhir"), "3 Bulan Terakhir (89d in, 91d out)");
    Assertions.assertEquals(sorted("-1.000", "-2.000", "-3.000", "-4.000", "-5.000", "-6.000", "-7.000", "-8.000"),
        amountsPerFilter.get("Semua Waktu"), "Semua Waktu");
  }

  private static java.util.List<String> sorted(String... v) {
    java.util.List<String> l = new java.util.ArrayList<>(java.util.List.of(v));
    java.util.Collections.sort(l);
    return l;
  }

  @And("^\"3 Bulan Terakhir\" = 90 days back \\(not 3 calendar months\\)$")
  public void threeMonthsIs90DaysBack() {
    // 89-day-old (-6.000) visible and 91-day-old (-7.000) not visible under "3 Bulan Terakhir", both visible under "Semua Waktu".
    java.util.List<String> three = amountsPerFilter.get("3 Bulan Terakhir");
    java.util.List<String> all = amountsPerFilter.get("Semua Waktu");
    Assertions.assertTrue(three.contains("-6.000"), "89-day-old transaction must be listed under 3 Bulan Terakhir: " + three);
    Assertions.assertFalse(three.contains("-7.000"), "91-day-old transaction must NOT be listed under 3 Bulan Terakhir: " + three);
    Assertions.assertTrue(all.contains("-6.000") && all.contains("-7.000"), "Both must be listed under Semua Waktu: " + all);
  }

  // ---- HIST-15: Edit Transaksi from the detail modal; same validation as SAVE-06..09 ----

  private boolean detailClosedAfterEdit;
  private final java.util.Map<String, String> prefilled = new java.util.HashMap<>();
  private final java.util.List<String> validationProblems = new java.util.ArrayList<>();

  @Given("^Detail modal open for a transaction$")
  public void detailModalOpenForATransaction() {
    signInAndCreateFixture(driver(), null);
    openDetailForFirstRow(driver());
  }

  // "Click "Edit Transaksi"" is defined in SaveSteps (routes to the detail-modal button when it is open).

  @And("^Check the fields$")
  public void checkTheFields() {
    EditTransactionModal modal = new EditTransactionModal(driver());
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((x) -> modal.isOpen());
    detailClosedAfterEdit = !new HistoryPage(driver()).isDetailModalOpen();
    prefilled.put("platform", modal.platformValue());
    prefilled.put("harga", modal.hargaValue());
    prefilled.put("kategori", modal.kategoriValue());
    prefilled.put("metode", modal.metodeValue());
    prefilled.put("catatan", modal.catatanValue());
  }

  @And("^Try the SAVE-07 combo-box, SAVE-08 price limit and negative value, and SAVE-09 200-char note rules$")
  public void tryTheSaveRules() {
    EditTransactionModal modal = new EditTransactionModal(driver());
    validationProblems.clear();
    // SAVE-06: Platform required (modal must stay open on an empty Platform) and max 50 with n/50
    modal.setPlatform("");
    modal.save();
    if (!modal.isOpen()) {
      validationProblems.add("SAVE-06: modal closed although Platform was empty");
    }
    modal.setPlatform("P".repeat(60));
    if (modal.platformValue().length() != 50) {
      validationProblems.add("SAVE-06: Platform not capped at 50 (len " + modal.platformValue().length() + ")");
    }
    if (!modal.counterTextFor("Platform").contains("/50")) {
      validationProblems.add("SAVE-06: no n/50 counter");
    }
    // SAVE-07: partial match resolves; text matching nothing reverts
    modal.setMetode("Pay");
    if (!modal.metodeValue().toLowerCase().contains("pay")) {
      validationProblems.add("SAVE-07: 'Pay' did not resolve to a Pay* method, got " + modal.metodeValue());
    }
    String kategoriBefore = modal.kategoriValue();
    modal.setKategori("NotARealCategoryXYZ");
    if (modal.kategoriValue().equals("NotARealCategoryXYZ")) {
      validationProblems.add("SAVE-07: unmatched category text stuck");
    } else if (!modal.kategoriValue().equals(kategoriBefore)) {
      validationProblems.add("SAVE-07: unmatched category text did not revert to previous value (" + kategoriBefore + " -> " + modal.kategoriValue() + ")");
    }
    // SAVE-08: cap at 999.999.999, no negatives
    modal.setHarga("12345678901");
    if (!"999999999".equals(modal.hargaValue())) {
      validationProblems.add("SAVE-08: price not capped at 999999999, got " + modal.hargaValue());
    }
    modal.setHarga("-500");
    if (modal.hargaValue().contains("-")) {
      validationProblems.add("SAVE-08: negative value accepted: " + modal.hargaValue());
    }
    // SAVE-09: note max 200 with n/200
    modal.setCatatan("C".repeat(220));
    if (modal.catatanValue().length() != 200) {
      validationProblems.add("SAVE-09: note not capped at 200 (len " + modal.catatanValue().length() + ")");
    }
    if (!modal.counterTextFor("Catatan").contains("/200")) {
      validationProblems.add("SAVE-09: no n/200 counter");
    }
  }

  @Then("^Detail modal closes$")
  public void detailModalCloses() {
    Assertions.assertTrue(detailClosedAfterEdit, "Expected the detail modal to close when the edit modal opens");
  }

  @And("^edit modal opens pre-filled with that transaction$")
  public void editModalOpensPrefilled() {
    Assertions.assertEquals(FIXTURE_PLATFORM, prefilled.get("platform"));
    Assertions.assertEquals("20000", prefilled.get("harga"));
    Assertions.assertEquals("Makan", prefilled.get("kategori"));
    Assertions.assertEquals("QRIS", prefilled.get("metode"));
    Assertions.assertEquals(FIXTURE_NOTE, prefilled.get("catatan"));
  }

  @And("^validation rules behave exactly as SAVE-06\\.\\.09$")
  public void validationRulesBehaveAsSave() {
    Assertions.assertTrue(validationProblems.isEmpty(), "Edit-from-Riwayat validation deviates from SAVE-06..09: " + validationProblems);
  }

  // ---- HIST-16: no date/time field; transaction stays under its original day ----

  @Given("^Edit modal open for an older transaction$")
  public void editModalOpenForAnOlderTransaction() {
    // The UI cannot create an older transaction; today's fixture stands in (limitation noted in README).
    openEditModalFromRiwayat(driver());
  }

  private boolean dateFieldFound;
  private java.util.List<String> labelsSeen = new java.util.ArrayList<>();
  private java.util.List<String> headingsAfterSave = new java.util.ArrayList<>();

  @When("^Inspect the edit modal fields$")
  public void inspectTheEditModalFields() {
    EditTransactionModal modal = new EditTransactionModal(driver());
    dateFieldFound = modal.hasDateOrTimeField();
    labelsSeen = modal.labelTexts();
  }

  @And("^Save a change$")
  public void saveAChange() {
    EditTransactionModal modal = new EditTransactionModal(driver());
    modal.setCatatan("diubah HIST-16");
    modal.save();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((x) -> !new EditTransactionModal(x).isOpen());
  }

  @And("^Check the day group in Riwayat$")
  public void checkTheDayGroupInRiwayat() {
    HistoryPage history = new HistoryPage(driver());
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((x) -> history.listText().contains("diubah HIST-16"));
    headingsAfterSave = history.dayGroupHeadings();
  }

  @Then("^No date/time field exists$")
  public void noDateTimeFieldExists() {
    Assertions.assertFalse(dateFieldFound, "Edit modal must have no date/time field; labels: " + labelsSeen);
  }

  @And("^transaction stays under its original day$")
  public void transactionStaysUnderOriginalDay() {
    Assertions.assertEquals(java.util.List.of("Hari Ini"), headingsAfterSave,
        "Expected the edited transaction to remain in its original day group (Hari Ini)");
  }

  // ---- HIST-17 (+MON-07): saving an edit updates Riwayat, Home and Ringkasan ----

  private final java.util.List<String> hist17Problems = new java.util.ArrayList<>();

  @Given("^Edit modal open for a transaction inside the current month$")
  public void editModalOpenForATransactionInsideTheCurrentMonth() {
    openEditModalFromRiwayat(driver());
  }

  @When("^Change the price and category$")
  public void changeThePriceAndCategory() {
    EditTransactionModal modal = new EditTransactionModal(driver());
    modal.setHarga("35000");
    modal.setKategori("Transport");
  }

  // "Click "Simpan Perubahan"" is defined in SaveSteps.

  @And("^Check Riwayat, Home and Ringkasan \\(monthly \\+ weekly if in range\\)$")
  public void checkRiwayatHomeAndRingkasan() {
    hist17Problems.clear();
    WebDriver d = driver();
    new WebDriverWait(d, Duration.ofSeconds(5)).until((x) -> !new EditTransactionModal(x).isOpen());
    HistoryPage history = new HistoryPage(d);
    try {
      new WebDriverWait(d, Duration.ofSeconds(5)).until((x) -> history.rowAmounts().contains("-35.000"));
    } catch (org.openqa.selenium.TimeoutException e) {
      hist17Problems.add("Riwayat row did not change to -35.000, rows: " + history.rowAmounts());
    }
    if (!history.listText().contains("Transport")) {
      hist17Problems.add("Riwayat row does not show category Transport");
    }
    new HomePage(d).openViaNav();
    try {
      new WebDriverWait(d, Duration.ofSeconds(8)).until((x) -> new HomePage(x).totalText().contains("35.000"));
    } catch (org.openqa.selenium.TimeoutException e) {
      hist17Problems.add("Home total is [" + new HomePage(d).totalText() + "], expected Rp 35.000");
    }
    MonthlyPage monthly = new MonthlyPage(d);
    monthly.openViaNav();
    try {
      new WebDriverWait(d, Duration.ofSeconds(8)).until((x) -> {
        MonthlyPage m = new MonthlyPage(x);
        return m.totalValue() == 35000 && m.categoryAmounts().equals(java.util.List.of(35000L));
      });
    } catch (org.openqa.selenium.TimeoutException e) {
      hist17Problems.add("Ringkasan (monthly) total=" + monthly.totalValue() + " categories=" + monthly.categoryNames() + monthly.categoryAmounts()
          + ", expected 35000 / [Transport 35000] (old Makan 20000 removed)");
    }
    if (!monthly.categoryNames().equals(java.util.List.of("Transport"))) {
      hist17Problems.add("Ringkasan monthly categories " + monthly.categoryNames() + ", expected [Transport]");
    }
    monthly.toggleWeekly();
    try {
      new WebDriverWait(d, Duration.ofSeconds(8)).until((x) -> new MonthlyPage(x).totalValue() == 35000);
    } catch (org.openqa.selenium.TimeoutException e) {
      hist17Problems.add("Ringkasan (weekly) total=" + monthly.totalValue() + ", expected 35000");
    }
  }

  @And("^change shows immediately in Riwayat$")
  public void changeShowsImmediatelyInRiwayat() {
    Assertions.assertTrue(hist17Problems.stream().noneMatch((p) -> p.startsWith("Riwayat")), String.valueOf(hist17Problems));
  }

  @And("^final numbers in Home/Ringkasan reflect the edit \\(old category amount reduced, new one increased\\)$")
  public void finalNumbersReflectTheEdit() {
    Assertions.assertTrue(hist17Problems.stream().noneMatch((p) -> p.startsWith("Home") || p.startsWith("Ringkasan")), String.valueOf(hist17Problems));
  }

  // ---- HIST-18: closing the edit modal discards changes ----
  // Given "Edit modal open with changes made" is shared with SAVE-10 (SaveSteps); for @HIST-18 it opens the modal from Riwayat.

  @When("^Change fields$")
  public void changeFields() {
    EditTransactionModal modal = new EditTransactionModal(driver());
    modal.setPlatform("Should Not Persist H18");
    modal.setHarga("77777");
  }

  @And("^Click the close \\(X\\)$")
  public void clickTheCloseX() {
    new EditTransactionModal(driver()).closeWithX();
  }

  @Then("^Changes discarded$")
  public void changesDiscarded() {
    boolean closed = new WebDriverWait(driver(), Duration.ofSeconds(5)).until((x) -> !new EditTransactionModal(x).isOpen());
    Assertions.assertTrue(closed, "Expected the edit modal to close via X");
  }

  @And("^transaction unchanged in the list$")
  public void transactionUnchangedInTheList() {
    HistoryPage history = new HistoryPage(driver());
    String text = history.listText();
    Assertions.assertFalse(text.contains("Should Not Persist H18"), "Discarded Platform edit leaked into the list");
    Assertions.assertFalse(history.rowAmounts().contains("-77.777"), "Discarded price edit leaked into the list");
    Assertions.assertTrue(history.rowAmounts().contains("-20.000"), "Expected the original -20.000 row, got " + history.rowAmounts());
    Assertions.assertTrue(text.contains(FIXTURE_PLATFORM), "Expected the original platform in the list");
  }

  // ---- HIST-19: save failure -> toast "Gagal menyimpan perubahan: ..." ----
  // Failure induction: a write the server rejects. The Firestore web SDK queues writes while
  // offline (no error is raised), so blocking the network does NOT produce a failed save. Instead
  // a second session of the same account deletes the transaction while the edit modal is open;
  // the first session's update then targets a document that no longer exists.

  @Given("^Edit modal open; connection/server failure induced \\(e\\.g\\. block the Firestore request in DevTools or stop the emulator\\)$")
  public void editModalOpenFailureInduced() {
    String email = "hist19+" + System.nanoTime() + "@example.com";
    signInAndCreateFixture(driver(), email);
    openDetailForFirstRow(driver());
    new HistoryPage(driver()).clickEditInDetailModal();
    EditTransactionModal modal = new EditTransactionModal(driver());
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((x) -> modal.isOpen());

    WebDriver sessionB = DriverFactory.create();
    try {
      sessionB.get(Config.baseUrl());
      SignInHelper.signInAsExisting(sessionB, email);
      HistoryPage historyB = new HistoryPage(sessionB);
      openDetailForFirstRow(sessionB);
      historyB.clickDeleteInDetailModal();
      historyB.confirmDelete();
      new WebDriverWait(sessionB, Duration.ofSeconds(8)).until((x) -> new HistoryPage(x).isEmptyStateShown());
    } finally {
      sessionB.quit();
    }
  }

  // "Change a field" is defined in SaveSteps (sets Platform to "Should Not Persist").
  // "Click "Simpan Perubahan"" is defined in SaveSteps.

  @Then("^Error toast \"Gagal menyimpan perubahan: \\.\\.\\.\"$")
  public void errorToastGagalMenyimpan() {
    try {
      boolean shown = new WebDriverWait(driver(), Duration.ofSeconds(10))
          .until((x) -> !x.findElements(By.xpath("//*[contains(normalize-space(.), 'Gagal menyimpan perubahan')]")).isEmpty());
      Assertions.assertTrue(shown);
    } catch (org.openqa.selenium.TimeoutException e) {
      Assertions.fail("Expected an error toast 'Gagal menyimpan perubahan: ...' after the failed save; none appeared. "
          + "Edit modal still open: " + new EditTransactionModal(driver()).isOpen());
    }
  }

  @And("^list unchanged$")
  public void listUnchanged() {
    Assertions.assertFalse(new HistoryPage(driver()).listText().contains("Should Not Persist"), "The failed edit must not appear in the list");
  }
}
