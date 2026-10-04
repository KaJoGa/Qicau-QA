package com.qicau.qa.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads non-secret config from src/test/resources/config/local.properties, with system
 * property overrides (e.g. {@code mvn test -Dbase.url=http://localhost:3001}) taking
 * precedence - matches how L1/stress runs already isolate a PORT (see api-testing/README.md,
 * environments/stubs/README.md).
 */
public final class Config {

  private static final Properties PROPS = new Properties();

  static {
    try (InputStream in = Config.class.getResourceAsStream("/config/local.properties")) {
      if (in != null) {
        PROPS.load(in);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not load config/local.properties", e);
    }
  }

  private Config() {
  }

  public static String get(String key) {
    return System.getProperty(key, PROPS.getProperty(key));
  }

  public static String baseUrl() {
    return get("base.url");
  }

  /** True when scenarios should run in Chrome on a real Android phone through Appium
   * ({@code -Dbrowser.target=android}). */
  public static boolean android() {
    return "android".equalsIgnoreCase(get("browser.target"));
  }

  public static String appiumUrl() {
    String v = get("appium.url");
    return v != null ? v : "http://127.0.0.1:4723";
  }

  /** adb serial of the phone; optional (Appium picks the only connected device if unset). */
  public static String androidUdid() {
    return get("android.udid");
  }

  public static boolean headless() {
    return Boolean.parseBoolean(get("browser.headless"));
  }

  public static String emulatorAuthHost() {
    return get("emulator.auth.host");
  }

  public static int emulatorAuthPort() {
    return Integer.parseInt(get("emulator.auth.port"));
  }

  public static String emulatorFirestoreHost() {
    return get("emulator.firestore.host");
  }

  public static int emulatorFirestorePort() {
    return Integer.parseInt(get("emulator.firestore.port"));
  }

  public static String emulatorProjectId() {
    return get("emulator.project.id");
  }
}
