package com.qicau.qa.support;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;

/** Forces the browser offline/online via Chrome's own network-conditions API (CDP), for the
 * many scenarios that need to simulate a dropped connection (VOICE-07, MAN-06, PWA-01/03/04,
 * SYNC-* [out of L4 scope]). No app cooperation needed - this is a real network-layer block. */
public final class NetworkSimulator {

  private NetworkSimulator() {
  }

  public static void goOffline(WebDriver driver) {
    ChromiumNetworkConditions conditions = new ChromiumNetworkConditions();
    conditions.setOffline(true);
    ((ChromiumDriver) driver).setNetworkConditions(conditions);
  }

  public static void goOnline(WebDriver driver) {
    ChromiumNetworkConditions conditions = new ChromiumNetworkConditions();
    conditions.setOffline(false);
    ((ChromiumDriver) driver).setNetworkConditions(conditions);
  }
}
