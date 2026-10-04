package com.qicau.qa.runners;

import com.qicau.qa.pages.BottomNav;
import com.qicau.qa.pages.HistoryPage;
import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverFactory;
import com.qicau.qa.support.NetworkSimulator;
import com.qicau.qa.support.SignInHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Sprint 3 DOM-capture tool (not part of the Cucumber suite). Run: mvn test -Dtest=ExplorerSprint3Test */
public class ExplorerSprint3Test {

  @Test
  void dump() throws Exception {
    Path dir = Path.of("dump", "s3");
    Files.createDirectories(dir);
    WebDriver d = DriverFactory.create();
    try {
      d.get(Config.baseUrl());
      SignInHelper.signIn(d);
      ManualInputModal m = new ManualInputModal(d);
      ManualInputModal.open(d);
      m.switchToFormulirLangsung();
      Thread.sleep(300);
      dump(d, dir, "01-direct-form");
      m.setPlatform("Warung");
      m.setPrice("12000");
      m.saveDirectForm();
      Thread.sleep(1500);
      dump(d, dir, "02-home-after-save");
      new BottomNav(d).clickRiwayat();
      Thread.sleep(1500);
      dump(d, dir, "03-riwayat");
      new HistoryPage(d).clickFirstRow();
      Thread.sleep(700);
      dump(d, dir, "04-detail-modal");
      tryClick(d, By.xpath("//button[contains(normalize-space(.), 'Edit Transaksi')]"));
      Thread.sleep(700);
      dump(d, dir, "05-edit-modal-from-riwayat");
      tryClick(d, By.xpath("//h3[contains(text(), 'Edit Transaksi')]/following-sibling::button[1]"));
      Thread.sleep(500);
      new BottomNav(d).clickRingkasan();
      Thread.sleep(1500);
      dump(d, dir, "06-ringkasan");
      new BottomNav(d).clickRiwayat();
      Thread.sleep(800);
      dump(d, dir, "07-riwayat-buttons");
      tryClick(d, By.xpath("//button[contains(normalize-space(.), 'Reset')]"));
      Thread.sleep(600);
      dump(d, dir, "08-reset-dialog");
      Thread.sleep(1500);
      dump(d, dir, "08b-reset-dialog-after-1500ms");
      tryClick(d, By.xpath("//button[contains(normalize-space(.), 'Batal')]"));
      Thread.sleep(500);
      tryClick(d, By.xpath("//button[contains(normalize-space(.), 'Sync')]"));
      Thread.sleep(600);
      dump(d, dir, "09-sync-dialog-first-time");
      tryClick(d, By.xpath("//button[contains(normalize-space(.), 'Batal')]"));
      Thread.sleep(500);
      NetworkSimulator.goOffline(d);
      Thread.sleep(1500);
      dump(d, dir, "10-offline-riwayat");
    } finally {
      d.quit();
    }
  }

  @Test
  void dumpAfterEditAndDelete() throws Exception {
    Path dir = Path.of("dump", "s3");
    Files.createDirectories(dir);
    WebDriver d = DriverFactory.create();
    try {
      d.get(Config.baseUrl());
      SignInHelper.signIn(d);
      ManualInputModal m = new ManualInputModal(d);
      m.quickSaveWithPriceOnly("20000");
      new BottomNav(d).clickRiwayat();
      Thread.sleep(1200);
      new HistoryPage(d).clickFirstRow();
      Thread.sleep(600);
      tryClick(d, By.xpath("//button[contains(normalize-space(.), 'Edit Transaksi')]"));
      Thread.sleep(600);
      var em = new com.qicau.qa.pages.EditTransactionModal(d);
      em.setHarga("35000");
      em.setKategori("Transport");
      em.save();
      Thread.sleep(1500);
      new BottomNav(d).clickRingkasan();
      Thread.sleep(2500);
      dump(d, dir, "11-ringkasan-after-edit-category-change");
      new BottomNav(d).clickRiwayat();
      Thread.sleep(1200);
      new HistoryPage(d).clickFirstRow();
      Thread.sleep(600);
      new HistoryPage(d).clickDeleteInDetailModal();
      new HistoryPage(d).confirmDelete();
      Thread.sleep(1000);
      new BottomNav(d).clickRingkasan();
      Thread.sleep(2500);
      dump(d, dir, "12-ringkasan-after-delete-only-txn");
    } finally {
      d.quit();
    }
  }

  private void tryClick(WebDriver d, By by) {
    try {
      d.findElement(by).click();
    } catch (Exception e) {
      System.err.println("tryClick failed: " + by + " " + e.getMessage().split("\n")[0]);
    }
  }

  private void dump(WebDriver d, Path dir, String name) throws Exception {
    Files.writeString(dir.resolve(name + ".html"), d.getPageSource());
  }
}
