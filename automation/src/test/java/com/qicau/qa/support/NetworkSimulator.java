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
    if (!(driver instanceof ChromiumDriver)) {
      setAppiumOffline(driver, true);
      return;
    }
    ChromiumNetworkConditions conditions = new ChromiumNetworkConditions();
    conditions.setOffline(true);
    ((ChromiumDriver) driver).setNetworkConditions(conditions);
  }

  public static void goOnline(WebDriver driver) {
    if (!(driver instanceof ChromiumDriver)) {
      setAppiumOffline(driver, false);
      return;
    }
    ChromiumNetworkConditions conditions = new ChromiumNetworkConditions();
    conditions.setOffline(false);
    ((ChromiumDriver) driver).setNetworkConditions(conditions);
  }

  /** Android (Appium) path: Chrome DevTools Protocol through chromedriver's goog/cdp/execute, which
   * Appium proxies (the chromium/network_conditions route is NOT proxied: 404, checked 2026-10-05).
   * A browser-level block, so it also works when the app is reached over `adb reverse` (turning the
   * phone's wifi off would not cut that USB tunnel). */
  private static void setAppiumOffline(WebDriver driver, boolean offline) {
    cdp(driver, "{\"cmd\":\"Network.enable\",\"params\":{}}");
    cdp(driver, "{\"cmd\":\"Network.emulateNetworkConditions\",\"params\":{\"offline\":" + offline
        + ",\"latency\":0,\"downloadThroughput\":-1,\"uploadThroughput\":-1}}");
  }

  private static void cdp(WebDriver driver, String body) {
    String sid = ((org.openqa.selenium.remote.RemoteWebDriver) driver).getSessionId().toString();
    String url = Config.appiumUrl() + "/session/" + sid + "/goog/cdp/execute";
    try {
      java.net.http.HttpResponse<String> r = java.net.http.HttpClient.newHttpClient().send(
          java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
              .header("Content-Type", "application/json")
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build(),
          java.net.http.HttpResponse.BodyHandlers.ofString());
      if (r.statusCode() >= 300) {
        throw new IllegalStateException("Appium CDP call failed: " + r.statusCode() + " " + r.body());
      }
    } catch (java.io.IOException | InterruptedException e) {
      throw new IllegalStateException("Could not set network conditions through Appium", e);
    }
  }
}
