package com.qicau.qa.runners;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Entry point for `mvn test`. Defaults to @smoke only (test-plan.md section 3.3 tiers) - run
 * `mvn test -Dcucumber.filter.tags="@regression"` for the full suite, or e.g.
 * `mvn test -Dcucumber.filter.tags="@AUTH-01"` for a single spec ID.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.qicau.qa.steps,com.qicau.qa.hooks")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-report.html")
public class CucumberTestRunner {
}
