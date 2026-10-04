package com.qicau.qa.steps;

import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.HomePage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.pages.MonthlyPage;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** HIST-20: confirm delete while offline. The "server rejects -> error toast, row returns" clause
 * cannot be forced from the UI (offline writes are queued, not rejected) and is NOT automated. */
public class HistoryOfflineDeleteSteps {

  private static final By HAPUS_CONFIRM = By.xpath("//button[normalize-space(text())='Hapus']");
  private static final By HAPUS_DIALOG = By.xpath("//*[contains(text(), 'Hapus Transaksi?')]");

  private WebDriver driver() {
    return DriverContext.get();
  }

  private void saveDirect(String price, String kategori) {
    ManualInputModal modal = new ManualInputModal(driver());
    ManualInputModal.open(driver());
    modal.switchToFormulirLangsung();
    modal.setPrice(price);
    modal.setKategori(kategori);
    modal.saveDirectForm();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> !new ManualInputModal(d).isFormulirLangsungModeActive());
    HistoryEditSteps.dismissSaveToast(driver());
  }

  private long dialogClosedMillis = -1;
  private java.util.List<String> rowsAfterDelete;
  private String riwayatTextAfterDelete;

  @Given("^Signed in with two transactions, Riwayat open$")
  public void signedInWithTwoTransactionsRiwayatOpen() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    saveDirect("12000", "Makan");
    saveDirect("8000", "Transport");
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> history.rowAmounts().containsAll(java.util.List.of("-12.000", "-8.000")));
  }

  @When("^Go offline and delete one transaction through the confirmation dialog$")
  public void goOfflineAndDeleteOne() {
    HistoryPage history = new HistoryPage(driver());
    NetworkSimulator.goOffline(driver());
    history.clickRowWithAmount("-12.000");
    history.waitForDetailModal();
    history.clickDeleteInDetailModal();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until((d) -> history.isDeleteConfirmDialogShown());
    long t0 = System.currentTimeMillis();
    new WebDriverWait(driver(), Duration.ofSeconds(5)).until(ExpectedConditions.elementToBeClickable(HAPUS_CONFIRM)).click();
    try {
      new WebDriverWait(driver(), Duration.ofSeconds(3)).pollingEvery(Duration.ofMillis(100))
          .until(ExpectedConditions.invisibilityOfElementLocated(HAPUS_DIALOG));
      dialogClosedMillis = System.currentTimeMillis() - t0;
    } catch (org.openqa.selenium.TimeoutException e) {
      dialogClosedMillis = -1;
    }
    try {
      new WebDriverWait(driver(), Duration.ofSeconds(3)).until((d) -> !history.rowAmounts().contains("-12.000"));
    } catch (org.openqa.selenium.TimeoutException ignored) {
      // asserted below with the observed rows
    }
    rowsAfterDelete = history.rowAmounts();
    riwayatTextAfterDelete = history.listText();
  }

  @Then("^The confirmation dialog closes right away and the row disappears from the list$")
  public void dialogClosesAndRowGone() {
    String overlay = driver().findElements(HAPUS_DIALOG).isEmpty() ? "(none)" : driver().findElement(By.xpath("//div[contains(@class,'z-60')]")).getText().replace("\n", "|");
    Assertions.assertTrue(dialogClosedMillis >= 0, "Dialog 'Hapus Transaksi?' still open 3 s after clicking Hapus while offline; overlay: " + overlay);
    Assertions.assertFalse(rowsAfterDelete.contains("-12.000"), "Row -12.000 still listed offline: " + rowsAfterDelete + " text=" + riwayatTextAfterDelete);
    Assertions.assertTrue(rowsAfterDelete.contains("-8.000"), "Remaining row -8.000 missing: " + rowsAfterDelete);
    System.out.println("HIST-20 evidence: dialog closed after " + dialogClosedMillis + " ms; rows=" + rowsAfterDelete);
  }

  @When("^Go back online and wait for the offline delete to sync$")
  public void backOnlineAndWait() throws InterruptedException {
    NetworkSimulator.goOnline(driver());
    Thread.sleep(10000);
  }

  @Then("^The transaction stays deleted after a reload$")
  public void staysDeletedAfterReload() {
    driver().get(Config.baseUrl());
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> new HomePage(d).isDisplayed());
    HistoryPage history = new HistoryPage(driver());
    history.openViaNav();
    new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> history.transactionRowCount() >= 1);
    try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    java.util.List<String> rows = history.rowAmounts();
    System.out.println("HIST-20 evidence after reload: Riwayat rows=" + rows);
    Assertions.assertEquals(java.util.List.of("-8.000"), rows, "After reload Riwayat must hold only the remaining 8.000 transaction");
  }

  @And("^Ringkasan total equals the sum of the remaining transactions in Riwayat$")
  public void ringkasanTotalEqualsRiwayatSum() {
    long sum = new HistoryPage(driver()).rowAmounts().stream().mapToLong(MonthlyPage::parseRupiah).sum();
    MonthlyPage monthly = new MonthlyPage(driver());
    monthly.openViaNav();
    try {
      new WebDriverWait(driver(), Duration.ofSeconds(10)).until((d) -> new MonthlyPage(d).totalValue() == sum);
    } catch (org.openqa.selenium.TimeoutException ignored) {
      // asserted below
    }
    long total = monthly.totalValue();
    // observation only (BUG-020): category rows incl. Rp 0 leftovers
    System.out.println("HIST-20 evidence Ringkasan: total=" + total + " riwayatSum=" + sum + " categories=" + monthly.categoryNames() + monthly.categoryAmounts());
    Assertions.assertEquals(sum, total, "Ringkasan total differs from the Riwayat sum");
  }
}
