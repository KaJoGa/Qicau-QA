package com.qicau.qa.support;

import java.nio.file.Path;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Builds a Chrome WebDriver wired for L4 (test-plan.md section 3.3): a fake microphone device
 * so VOICE-* scenarios get deterministic audio input instead of a real mic, and mic permission
 * pre-granted so the permission prompt never blocks a scenario that isn't testing VOICE-06.
 *
 * Uses Selenium Manager (built into Selenium 4.6+) to resolve chromedriver automatically - no
 * separate driver binary to manage.
 */
public final class DriverFactory {

  private DriverFactory() {
  }

  public static WebDriver create() {
    return create(null);
  }

  /**
   * @param fakeAudioFile absolute path to a .wav fixture (see src/test/resources/audio/), or
   *     null for scenarios that don't touch the microphone at all.
   */
  public static WebDriver create(Path fakeAudioFile) {
    if (Config.android()) {
      return createAndroid();
    }
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--use-fake-ui-for-media-stream"); // auto-grant the mic permission prompt
    options.addArguments("--use-fake-device-for-media-stream");
    if (fakeAudioFile != null) {
      options.addArguments("--use-file-for-fake-audio-capture=" + fakeAudioFile.toAbsolutePath());
    }
    // Browser console capture for MON-09 (no permission-denied errors in the console).
    java.util.logging.Level all = java.util.logging.Level.ALL;
    org.openqa.selenium.logging.LoggingPreferences logPrefs = new org.openqa.selenium.logging.LoggingPreferences();
    logPrefs.enable(org.openqa.selenium.logging.LogType.BROWSER, all);
    options.setCapability("goog:loggingPrefs", logPrefs);
    if (Config.headless()) {
      options.addArguments("--headless=new");
    }
    ChromeDriver driver = new ChromeDriver(options);
    // The default headless window is short enough that content can sit under the app's fixed
    // bottom nav, causing click-intercepted errors on buttons near the bottom of the screen
    // (confirmed 2026-09-29). A generously tall viewport avoids that everywhere. Width matters
    // too: a genuinely mobile-narrow window (e.g. 430px) drops below the app's Tailwind "sm"
    // breakpoint (640px) and CSS-hides `hidden sm:block` desktop-label spans like Riwayat's page
    // title, which selenium's visibility-based waits then correctly (if confusingly) never find -
    // also confirmed 2026-09-29. Staying above that breakpoint keeps every scenario in the same
    // desktop-layout branch of the app's responsive CSS.
    driver.manage().window().setSize(new org.openqa.selenium.Dimension(800, 1000));
    return driver;
  }

  /**
   * Chrome on a real Android phone via an Appium server (UiAutomator2 + auto-downloaded
   * chromedriver). Needs: phone connected with USB debugging, `adb reverse tcp:3000 tcp:3000` (+ 8080,
   * 9099 for the Firebase emulators), and `appium --allow-insecure uiautomator2:chromedriver_autodownload`.
   * See read/06-android-hp-asli-adb-appium.md. No fake microphone: VOICE-* are not run on the phone.
   */
  private static WebDriver createAndroid() {
    org.openqa.selenium.MutableCapabilities caps = new org.openqa.selenium.MutableCapabilities();
    caps.setCapability("platformName", "Android");
    caps.setCapability("appium:automationName", "UiAutomator2");
    caps.setCapability("browserName", "Chrome");
    caps.setCapability("appium:chromedriverAutodownload", true);
    caps.setCapability("appium:noReset", true);
    caps.setCapability("appium:newCommandTimeout", 300);
    if (Config.androidUdid() != null && !Config.androidUdid().isBlank()) {
      caps.setCapability("appium:udid", Config.androidUdid());
    }
    try {
      return new org.openqa.selenium.remote.RemoteWebDriver(new java.net.URL(Config.appiumUrl()), caps);
    } catch (java.net.MalformedURLException e) {
      throw new IllegalStateException("Bad appium.url: " + Config.appiumUrl(), e);
    }
  }
}
