package com.qicau.qa.support;

import org.openqa.selenium.WebDriver;

/**
 * Shares the current scenario's WebDriver between Hooks (which creates/closes it) and the step
 * definition classes (which use it). No DI container is pulled in for a project this size - a
 * ThreadLocal keeps it correct if scenarios are ever run in parallel later.
 */
public final class DriverContext {

  private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

  private DriverContext() {
  }

  public static void set(WebDriver driver) {
    DRIVER.set(driver);
  }

  public static WebDriver get() {
    WebDriver driver = DRIVER.get();
    if (driver == null) {
      throw new IllegalStateException("No WebDriver for this scenario - was Hooks.before() run?");
    }
    return driver;
  }

  public static void clear() {
    DRIVER.remove();
    TAGS.remove();
  }

  // Tags of the running scenario (set by Hooks) - lets a step shared by several scenarios with
  // identical generated Gherkin text (e.g. SAVE-10's and HIST-18's "Edit modal open with changes
  // made") pick the right setup path without changing the generated .feature files.
  private static final ThreadLocal<java.util.Collection<String>> TAGS = new ThreadLocal<>();

  public static void setTags(java.util.Collection<String> tags) {
    TAGS.set(tags);
  }

  public static boolean hasTag(String tag) {
    java.util.Collection<String> t = TAGS.get();
    return t != null && t.contains(tag);
  }
}
