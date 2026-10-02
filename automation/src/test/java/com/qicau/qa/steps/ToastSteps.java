package com.qicau.qa.steps;

import com.qicau.qa.pages.ManualInputModal;
import com.qicau.qa.support.Config;
import com.qicau.qa.support.DriverContext;
import com.qicau.qa.support.SignInHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Glue for features/toast.feature. Only TOAST-02 is wired up - the save-success toast (SAVE-*)
 * is the one concretely reachable "global toast" from here. TOAST-01 ("Sync Reset install
 * success and failure from different tabs, including the login page") isn't attempted: Sync/
 * Reset Ekspor are L5 and deliberately out of L4's scope entirely (test-plan.md), and a real
 * install success/failure toast depends on the native browser install-prompt flow, which isn't
 * reliably triggerable from Selenium (see PWA-06's own note in automation/README.md) - forcing
 * either would mean guessing at a toast trigger mechanism the UI doesn't otherwise expose.
 */
public class ToastSteps {

  private WebDriver driver() {
    return DriverContext.get();
  }

  @Given("^A global toast is currently shown$")
  public void aGlobalToastIsCurrentlyShown() {
    driver().get(Config.baseUrl());
    SignInHelper.signIn(driver());
    new ManualInputModal(driver()).quickSaveWithPriceOnly("8000");
    new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> !d.findElements(By.xpath("//p[starts-with(normalize-space(.), 'Tersimpan:')]")).isEmpty());
  }

  @When("^Trigger a second global toast while the first is still visible$")
  public void triggerASecondGlobalToastWhileFirstStillVisible() {
    new ManualInputModal(driver()).quickSaveWithPriceOnly("9000");
  }

  @Then("^The old toast is replaced by the new one$")
  public void theOldToastIsReplacedByTheNewOne() {
    boolean exactlyOne = new WebDriverWait(driver(), Duration.ofSeconds(5))
        .until((d) -> d.findElements(By.xpath("//p[starts-with(normalize-space(.), 'Tersimpan:')]")).size() == 1);
    Assertions.assertTrue(exactlyOne, "Expected exactly one Tersimpan: toast on screen after triggering a second save while the first toast was still visible");
  }
}
