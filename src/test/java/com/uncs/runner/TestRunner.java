package com.uncs.runner;

import com.uncs.utility.BaseClass;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.DataProvider;

@CucumberOptions(
		features = "src/test/resources",
		glue = {"com.uncs.stepdefs", "com.uncs.utility"},
		// Two groups are held back from an unattended run.
		//
		// @e2e walks the same ground the per-module features already cover, so running both
		// would send the same traffic through the same conversation twice for one build.
		// Run the journey on its own with:  mvn test -Dcucumber.filter.tags="@e2e"
		//
		// @lockout deliberately locks nami@gmail.com and leaves it locked until somebody
		// clears it by hand, which is not something a nightly should do to itself. Run it
		// when you are ready to unlock afterwards:
		//   mvn test -Dcucumber.filter.tags="@lockout"
		//
		// @report files a real report into the moderation queue, and a submitted report
		// cannot be withdrawn. Running it nightly would post one every night:
		//   mvn test -Dcucumber.filter.tags="@report"
		tags = "not @e2e and not @lockout and not @report",
		plugin = {"pretty", "html:target/cucumber-reports.html", "json:target/cucumber.json", "junit:target/cucumber.xml"},
		publish = false
)
public class TestRunner extends AbstractTestNGCucumberTests {

	@Override
	@DataProvider(parallel = false)
	public Object[][] scenarios() {
		return super.scenarios();
	}

	/**
	 * Emails the report when a TestNG suite finishes.
	 *
	 * Paired with BaseClass's Cucumber AfterAll, which covers the case this one cannot: a
	 * feature launched straight from the IDE runs Cucumber without ever creating a TestNG
	 * suite. Both call the same guarded method, so a run that fires both still sends one
	 * email. Being suite scoped, this also covers the standalone LoginTest sharing the
	 * suite with the Cucumber runner.
	 */
	@AfterSuite(alwaysRun = true)
	public void tearDownSuite() {
		BaseClass.sendReportEmail();
	}
}
