package com.qicau.qa.steps;

import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.pages.MonthlyPage;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverContext;
import com.qicau.qa.support.FirestoreSeeder;
import com.qicau.qa.support.NetworkSimulator;
import com.qicau.qa.support.SignInHelper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Data-seeded L4 cases: HIST-03 (day labels), MON-08 (previous month excluded), MON-11 (inconsistent daily
 * summary auto-repaired). HIST-05 reuses {@link #signInCreateTodayAndSeed} from HistoryEditSteps.
 * Seeding: one real transaction is created through the UI (so the app itself writes the transaction,
 * the users doc and the daily summary), then back-dated transactions + matching daily summaries are
 * written to the emulator with the field types OBSERVED from that real write - see FirestoreSeeder.
 * Back-dated rows are stamped 12:00 local time; "today" always comes from the UI (real "now").
 */
public class SeededDataSteps {

  private static final String[] MONTHS = {"Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli",
      "Agustus", "September", "Oktober", "November", "Desember"};

  private WebDriver driver() {
    return DriverContext.get();
  }

  // ---- shared seeding helper ----

  /** uid of the user signed in by the last {@link #signInAndCreateToday} call. */
  static String uid;
  static String email;
  /** Per-day running summary (day key -> category -> amount) of everything seeded, so summaries stay consistent. */
  private static final Map<String, Map<String, Long>> SEEDED_DAYS = new LinkedHashMap<>();

  /** Sign in as a fresh fake user and create ONE real transaction "now" (Makan default, given price) via the UI. */
  static void signInAndCreateToday(WebDriver d, long price) {
    email = "seed+" + System.nanoTime() + "@example.com";
    SEEDED_DAYS.clear();
    d.get(Config.baseUrl());
    SignInHelper.signInAsNewUserWithEmail(d, email);
    new ManualInputModal(d).quickSaveWithPriceOnly(String.valueOf(price));
    HistoryEditSteps.dismissSaveToast(d);
    uid = FirestoreSeeder.uidForEmail(email);
    long now = System.currentTimeMillis();
    SEEDED_DAYS.computeIfAbsent(FirestoreSeeder.dayKey(now), (k) -> new LinkedHashMap<>()).merge("Makan", price, Long::sum);
  }

  /** Writes a back-dated transaction (12:00 local) plus its consistent daily summary. */
  static void seedAt(LocalDate date, String kategori, long harga, String platform) {
    long ts = FirestoreSeeder.epochMillis(date, 12, 0);
    FirestoreSeeder.createTransaction(uid, ts, kategori, harga, platform);
    String day = FirestoreSeeder.dayKey(ts);
    Map<String, Long> cats = SEEDED_DAYS.computeIfAbsent(day, (k) -> new LinkedHashMap<>());
    cats.merge(kategori, harga, Long::sum);
    long total = cats.values().stream().mapToLong(Long::longValue).sum();
    FirestoreSeeder.setDailySummary(uid, day, total, cats);
  }

  static void seedDaysAgo(int daysAgo, String kategori, long harga, String platform) {
    seedAt(LocalDate.now().minusDays(daysAgo), kategori, harga, platform);
  }

  /** Reloads so the app picks up the seeded data from a clean state. */
  static void reload(WebDriver d) {
    d.navigate().refresh();
    new WebDriverWait(d, Duration.ofSeconds(15)).until((x) -> new HomePage(x).isDisplayed());
  }

  /** HIST-05 entry point: today via the UI, each offset seeded as Makan with the given price. */
  static void signInCreateTodayAndSeed(WebDriver d, long todayPrice, int[] daysAgo, long[] prices) {
    signInAndCreateToday(d, todayPrice);
    for (int i = 0; i < daysAgo.length; i++) {
      seedDaysAgo(daysAgo[i], "Makan", prices[i], "Seed " + daysAgo[i] + "d");
    }
    reload(d);
  }

  // ---- HIST-03 ----

  private List<String> hist03Headings = new ArrayList<>();
  private List<String> hist03Expected = new ArrayList<>();

  private static String longLabel(LocalDate d) {
    return d.getDayOfMonth() + " " + MONTHS[d.getMonthValue() - 1] + " " + d.getYear();
  }

  @Given("^Transactions exist across multiple days$")
  public void transactionsExistAcrossMultipleDays() {
    signInAndCreateToday(driver(), 1000);
    seedDaysAgo(1, "Makan", 2000, "Seed kemarin");
    seedDaysAgo(5, "Makan", 3000, "Seed 5d");
    seedDaysAgo(40, "Makan", 4000, "Seed 40d");
    seedDaysAgo(400, "Makan", 5000, "Seed 400d");
    hist03Expected = List.of("Hari Ini", "Kemarin", longLabel(LocalDate.now().minusDays(5)),
        longLabel(LocalDate.now().minusDays(40)), longLabel(LocalDate.now().minusDays(400)));
    reload(driver());
  }

  @When("^View the list with transactions today yesterday and older$")
  public void viewTheListWithTransactionsTodayYesterdayAndOlder() throws InterruptedException {
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((x) -> new HistoryPage(x).transactionRowCount() > 0);
    history.selectTimeFilter("Semua Waktu");
    Thread.sleep(1500);
    hist03Headings = history.dayGroupHeadings();
  }

  @Then("^Grouped by day, newest first$")
  public void groupedByDayNewestFirst() {
    Assertions.assertEquals(hist03Expected.size(), hist03Headings.size(),
        "Expected one group per seeded day " + hist03Expected + " but headings were " + hist03Headings);
  }

  @And("^labels \"Hari Ini\", \"Kemarin\", else \"D NamaBulan YYYY\" \\(e\\.g\\. \"5 Mei 2026\"\\)$")
  public void labelsHariIniKemarinElseDate() {
    Assertions.assertEquals(hist03Expected, hist03Headings, "Day-group headings (newest first) differ from the spec");
  }

  // ---- MON-08 ----

  private LocalDate mon08Today;
  private long mon08ExpectedTotal;
  private long mon08Total;
  private List<String> mon08CatNames = new ArrayList<>();
  private List<Long> mon08CatAmounts = new ArrayList<>();

  @Given("^Transactions exist in the current and a previous month$")
  public void transactionsExistInCurrentAndPreviousMonth() {
    mon08Today = LocalDate.now();
    signInAndCreateToday(driver(), 10000);
    mon08ExpectedTotal = 10000;
    if (mon08Today.getDayOfMonth() >= 2) {
      seedDaysAgo(1, "Makan", 5000, "Seed bulan ini");
      mon08ExpectedTotal += 5000;
    }
    LocalDate lastDayPrev = mon08Today.withDayOfMonth(1).minusDays(1);
    seedAt(lastDayPrev, "Transport", 70000, "Seed bulan lalu akhir");
    seedAt(mon08Today.withDayOfMonth(1).minusMonths(1).withDayOfMonth(15), "Makan", 30000, "Seed bulan lalu tengah");
    reload(driver());
  }

  @When("^View the current month's total and category breakdown$")
  public void viewCurrentMonthTotalAndBreakdown() {
    MonthlyPage monthly = new MonthlyPage(driver());
    monthly.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((x) -> new MonthlyPage(x).hasTotalCard());
    try {
      new WebDriverWait(driver(), Duration.ofSeconds(10)).until((x) -> new MonthlyPage(x).totalValue() >= mon08ExpectedTotal);
    } catch (org.openqa.selenium.TimeoutException ignored) {
      // fall through - the assertion below reports the actual value
    }
    mon08Total = monthly.totalValue();
    mon08CatNames = monthly.categoryNames();
    mon08CatAmounts = monthly.categoryAmounts();
  }

  @Then("^Previous month's transactions are not counted$")
  public void previousMonthsTransactionsAreNotCounted() {
    Assertions.assertEquals(mon08ExpectedTotal, mon08Total,
        "Ringkasan Bulan Ini total must only include the current month (expected " + mon08ExpectedTotal
            + "; seeded previous month rows: Transport 70.000 + Makan 30.000). Categories: " + mon08CatNames + mon08CatAmounts);
    for (int i = 0; i < mon08CatNames.size(); i++) {
      if (mon08CatNames.get(i).contains("Transport")) {
        Assertions.assertEquals(0L, mon08CatAmounts.get(i).longValue(),
            "Previous-month-only category Transport must not have an amount this month: " + mon08CatNames + mon08CatAmounts);
      }
    }
    long sum = mon08CatAmounts.stream().mapToLong(Long::longValue).sum();
    Assertions.assertEquals(mon08ExpectedTotal, sum, "Category rows must add up to the current month total: " + mon08CatNames + mon08CatAmounts);
  }

  // ---- MON-11 ----

  private static final long MON11_TODAY_PRICE = 25000;
  private String mon11Today;
  private String mon11Yesterday; // null when today is the 1st (yesterday would be another month - still seeded, kept for the untouched-day check)
  private String mon11YesterdayUpdateBefore;
  private long mon11ExpectedMonthTotal;
  private long mon11DisplayTotal;
  private List<String> mon11DisplayCats = new ArrayList<>();
  private List<Long> mon11DisplayAmounts = new ArrayList<>();
  private String mon11TodayDocAfterOnline;
  private String mon11TodayDocAfterOffline;
  private String mon11AfterReconnect = "";
  private boolean mon11OverlayFound;
  private String mon11OverlayText = "";

  private void corruptToday() {
    Map<String, Long> bad = new LinkedHashMap<>();
    bad.put("Makan", 5000L);
    bad.put("Hiburan", 1000L);
    FirestoreSeeder.setDailySummary(uid, mon11Today, 99999, bad); // total != sum(by_category) != real transactions
  }

  @Given("^A day whose category breakdown does not match its total \\(legacy data; needs seeded/old data on emulator or a legacy account\\)$")
  public void aDayWhoseBreakdownDoesNotMatchItsTotal() {
    signInAndCreateToday(driver(), MON11_TODAY_PRICE);
    mon11Today = FirestoreSeeder.dayKey(System.currentTimeMillis());
    // A second, consistent day that must NOT be touched by the repair ("only that day").
    seedDaysAgo(1, "Transport", 8000, "Seed kemarin");
    mon11Yesterday = FirestoreSeeder.dayKey(FirestoreSeeder.epochMillis(1, 12, 0));
    mon11YesterdayUpdateBefore = FirestoreSeeder.updateTime(FirestoreSeeder.getDailySummaryRaw(uid, mon11Yesterday));
    LocalDate today = LocalDate.now();
    mon11ExpectedMonthTotal = MON11_TODAY_PRICE + (today.getDayOfMonth() >= 2 ? 8000 : 0);
    // Observe (not assume) the app-written summary for today, then corrupt it.
    String observed = FirestoreSeeder.getDailySummaryRaw(uid, mon11Today);
    Assertions.assertEquals(MON11_TODAY_PRICE, FirestoreSeeder.summaryTotal(observed), "precondition: app wrote a consistent summary: " + observed);
    corruptToday();
    reload(driver());
    // Control: while only Catat (Home) is open the corrupted day must stay as seeded - the repair belongs to opening Ringkasan.
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    Assertions.assertEquals(99999L, FirestoreSeeder.summaryTotal(FirestoreSeeder.getDailySummaryRaw(uid, mon11Today)),
        "Control failed: the corrupted summary changed before Ringkasan was opened");
  }

  // "Open the Ringkasan tab" is defined in MonthlyConsistencySteps (MON-09/10/11 share it).

  @And("^Wait a moment without any action$")
  public void waitAMomentWithoutAnyAction() throws InterruptedException {
    // Poll the emulator document (not the UI) for the background repair; no clicks, no reload.
    long deadline = System.currentTimeMillis() + 20000;
    while (System.currentTimeMillis() < deadline) {
      String raw = FirestoreSeeder.getDailySummaryRaw(uid, mon11Today);
      if (FirestoreSeeder.summaryTotal(raw) == MON11_TODAY_PRICE
          && FirestoreSeeder.summaryByCategory(raw).equals(Map.of("Makan", MON11_TODAY_PRICE))) {
        break;
      }
      Thread.sleep(500);
    }
    Thread.sleep(2000); // let the UI catch up with the repaired document
    mon11TodayDocAfterOnline = FirestoreSeeder.getDailySummaryRaw(uid, mon11Today);
    // Any dialog / toast / alert region visible after the open (no user-visible repair notice is allowed).
    List<String> overlays = new ArrayList<>();
    for (var el : driver().findElements(By.cssSelector("[role='dialog'], [role='alertdialog'], [role='alert'], [role='status'], [class*='toast' i]"))) {
      if (el.isDisplayed() && !el.getText().isBlank()) {
        overlays.add(el.getText().trim());
      }
    }
    String page = driver().findElement(By.tagName("body")).getText().toLowerCase();
    for (String word : new String[] {"perbaik", "memperbaiki", "diperbarui", "rebuild", "bangun ulang", "repair"}) {
      if (page.contains(word)) {
        overlays.add("page text contains '" + word + "'");
      }
    }
    mon11OverlayFound = !overlays.isEmpty();
    mon11OverlayText = String.valueOf(overlays);
  }

  @And("^Compare breakdown/total with Riwayat$")
  public void compareBreakdownTotalWithRiwayat() {
    MonthlyPage monthly = new MonthlyPage(driver());
    monthly.openViaNav();
    mon11DisplayTotal = monthly.totalValue();
    mon11DisplayCats = monthly.categoryNames();
    mon11DisplayAmounts = monthly.categoryAmounts();
    // Riwayat (source of truth = the transactions): sum of the rows in the default 7-day range that fall in this month.
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((x) -> new HistoryPage(x).transactionRowCount() > 0);
    long riwayatSum = 0;
    for (String a : history.rowAmounts()) {
      riwayatSum += MonthlyPage.parseRupiah(a);
    }
    Assertions.assertEquals(MON11_TODAY_PRICE + 8000, riwayatSum, "Riwayat (transactions) sum for the seeded days");
    monthly.openViaNav();
  }

  @And("^Repeat while offline$")
  public void repeatWhileOffline() throws InterruptedException {
    WebDriver d = driver();
    new HomePage(d).openViaNav();
    corruptToday(); // corrupt again via REST (goes straight to the emulator, not through the browser)
    NetworkSimulator.goOffline(d);
    try {
      new MonthlyPage(d).openViaNav();
      Thread.sleep(10000); // generous window for a (wrongly) triggered repair write
      mon11TodayDocAfterOffline = FirestoreSeeder.getDailySummaryRaw(uid, mon11Today);
    } finally {
      NetworkSimulator.goOnline(d);
    }
    Thread.sleep(8000); // observation only: what happens once the connection returns
    mon11AfterReconnect = FirestoreSeeder.getDailySummaryRaw(uid, mon11Today);
  }

  @Then("^The inconsistent day is fixed automatically \\(only that day\\), display becomes correct without user action or reload$")
  public void theInconsistentDayIsFixedAutomatically() {
    Map<String, Long> cats = FirestoreSeeder.summaryByCategory(mon11TodayDocAfterOnline);
    Assertions.assertEquals(MON11_TODAY_PRICE, FirestoreSeeder.summaryTotal(mon11TodayDocAfterOnline),
        "Stored daily summary total not repaired within 20s of opening Ringkasan: " + mon11TodayDocAfterOnline);
    Assertions.assertEquals(Map.of("Makan", MON11_TODAY_PRICE), cats, "Stored by_category not repaired: " + mon11TodayDocAfterOnline);
    String yesterdayAfter = FirestoreSeeder.updateTime(FirestoreSeeder.getDailySummaryRaw(uid, mon11Yesterday));
    Assertions.assertEquals(mon11YesterdayUpdateBefore, yesterdayAfter, "A consistent day (yesterday) must not be rewritten by the repair");
    Assertions.assertEquals(mon11ExpectedMonthTotal, mon11DisplayTotal,
        "Ringkasan total after the repair: " + mon11DisplayTotal + " " + mon11DisplayCats + mon11DisplayAmounts);
    Assertions.assertFalse(mon11DisplayCats.contains("Hiburan"), "Corrupt category 'Hiburan' still displayed: " + mon11DisplayCats + mon11DisplayAmounts);
    Assertions.assertEquals(mon11ExpectedMonthTotal, mon11DisplayAmounts.stream().mapToLong(Long::longValue).sum(),
        "Category rows do not add up to the real total: " + mon11DisplayCats + mon11DisplayAmounts);
  }

  @And("^no toast or dialog$")
  public void noToastOrDialog() {
    Assertions.assertFalse(mon11OverlayFound, "Unexpected toast/dialog/notice while repairing: " + mon11OverlayText);
  }

  @And("^nothing is repaired while offline$")
  public void nothingIsRepairedWhileOffline() {
    System.out.println("[MON-11] after reconnect (observation only), total=" + FirestoreSeeder.summaryTotal(mon11AfterReconnect)
        + " by_category=" + FirestoreSeeder.summaryByCategory(mon11AfterReconnect));
    Assertions.assertEquals(99999L, FirestoreSeeder.summaryTotal(mon11TodayDocAfterOffline),
        "A repair reached the emulator while the browser was offline: " + mon11TodayDocAfterOffline);
    Assertions.assertEquals(Map.of("Makan", 5000L, "Hiburan", 1000L), FirestoreSeeder.summaryByCategory(mon11TodayDocAfterOffline),
        "by_category was changed while offline: " + mon11TodayDocAfterOffline);
  }
}
