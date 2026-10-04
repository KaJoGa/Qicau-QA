package com.qicau.qa.hooks;

import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import com.qicau.qa.support.DriverFactory;

/**
 * Scenario lifecycle: fresh browser + a clean Firestore emulator per scenario (test-plan.md
 * section 6.2 - "Reset between runs"), so scenarios never depend on leftover state from a
 * previous one. Uses the Firestore emulator's own documented REST endpoint for clearing data -
 * not a call into Qicau's own code.
 */
public class Hooks {

  @Before
  public void before(Scenario scenario) {
    clearFirestoreEmulator();
    WebDriver driver = DriverFactory.create(fakeAudioFileFor(scenario));
    DriverContext.set(driver);
    DriverContext.setTags(scenario.getSourceTagNames());
    driver.get(Config.baseUrl());
  }

  /** Picks a fake-microphone .wav fixture for VOICE-* scenarios based on their spec-ID tag, so
   * recording actually has deterministic content instead of silence/whatever Chrome defaults to.
   * Everything else gets null (no mic interaction needed). */
  private java.nio.file.Path fakeAudioFileFor(Scenario scenario) {
    var tags = scenario.getSourceTagNames();
    String audioDir = "src/test/resources/audio/";
    if (tags.contains("@VOICE-03")) {
      return java.nio.file.Path.of(audioDir + "silence.wav");
    }
    if (tags.contains("@VOICE-04")) {
      return java.nio.file.Path.of(audioDir + "long-over-60s.wav");
    }
    if (tags.contains("@VOICE-05")) {
      return java.nio.file.Path.of(audioDir + "short-under-0.8s.wav");
    }
    if (tags.contains("@VOICE-09") || tags.contains("@LOWC-04")) {
      return java.nio.file.Path.of(audioDir + "noisy.wav");
    }
    if (tags.stream().anyMatch((t) -> t.startsWith("@VOICE-"))) {
      return java.nio.file.Path.of(audioDir + "clean.wav");
    }
    return null;
  }

  @After
  public void after(Scenario scenario) {
    WebDriver driver = DriverContext.get();
    if (scenario.isFailed() && driver instanceof TakesScreenshot) {
      byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
      scenario.attach(png, "image/png", scenario.getName());
      try {
        java.nio.file.Path dir = java.nio.file.Path.of("target", "fail-shots");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.write(dir.resolve(scenario.getName().replaceAll("[^A-Za-z0-9]+", "-") + ".png"), png);
      } catch (java.io.IOException ignored) {
        // screenshot file is a debugging aid only
      }
    }
    driver.quit();
    DriverContext.clear();
  }

  private void clearFirestoreEmulator() {
    String url = String.format(
        "http://%s:%d/emulator/v1/projects/%s/databases/(default)/documents",
        Config.emulatorFirestoreHost(), Config.emulatorFirestorePort(), Config.emulatorProjectId());
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      HttpRequest request = HttpRequest.newBuilder(URI.create(url)).DELETE().build();
      client.send(request, HttpResponse.BodyHandlers.discarding());
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(
          "Could not reach the Firestore emulator at " + url + " - is `npm run emulators` running?", e);
    }
  }
}
