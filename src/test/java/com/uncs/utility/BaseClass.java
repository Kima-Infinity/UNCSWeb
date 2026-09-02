package com.uncs.utility;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import com.microsoft.playwright.Page;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the one driver, report and config a scenario runs against.
 *
 * The fields are static because the step definition classes are built fresh by Cucumber
 * for every scenario, and they all have to reach the same browser session. Anything a
 * step needs across classes lives here.
 */
public class BaseClass {

	/**
	 * The tab every step works through, in place of Selenium's WebDriver.
	 *
	 * Still called driver: every step definition and page object refers to
	 * BaseClass.driver, and keeping the name makes this a change of engine rather than a
	 * rename spread across two dozen files.
	 */
	public static Page driver;

	public static ConfigDataProvider config;

	public static ExtentReports report;

	public static ExtentTest logger;

	public static String reportPath;

	/**
	 * Opens the browser once per scenario and points it at the login page.
	 *
	 * order = 0 so this runs before any other Before hook a step definition class might
	 * add later, since every one of those needs the driver to exist already.
	 */
	@Before(order = 0)
	public void cucumberSetUp() {

		// Values one step leaves for another are static, so last scenario's leftovers
		// would still be readable here if they were not cleared first.
		ScenarioContext.reset();

		logger = null;

		if (config == null) {
			config = new ConfigDataProvider();
		}

		if (report == null) {
			if (reportPath == null) {
				reportPath = System.getProperty("user.dir") + "/Reports/" + Helper.getCurrentDateTime() + "TestReport.html";
			}
			ExtentSparkReporter extent = new ExtentSparkReporter(new File(reportPath));
			report = new ExtentReports();
			report.attachReporter(extent);
		}

		if (driver == null) {
			driver = BrowserFactory.startBrowser(config.getUrl());
		}
	}

	/**
	 * Ends the scenario and closes the browser it opened.
	 *
	 * The reporting is wrapped so that the quit in the finally block always runs. It used
	 * to sit after the screenshot and the report flush, which meant a driver that had
	 * already died - a lost session, a crashed tab, a network drop mid-scenario - threw on
	 * the screenshot and skipped the quit entirely, leaking a browser and its driver
	 * process for every scenario that failed that way. A run interrupted like that left
	 * dozens of orphaned processes behind.
	 *
	 * Losing a screenshot on an already-broken scenario costs nothing. Losing the quit
	 * costs a process that outlives the run.
	 */
	/** One report per run, however many hooks reach this. */
	private static final java.util.concurrent.atomic.AtomicBoolean REPORT_SENT =
			new java.util.concurrent.atomic.AtomicBoolean(false);

	/**
	 * Emails the run's report when Cucumber finishes.
	 *
	 * This and TestRunner's AfterSuite call the same guarded method on purpose, because
	 * they cover different ways of starting a run and neither covers both:
	 *
	 *   mvn test, or a TestNG run configuration  - both fire
	 *   a .feature launched straight from the IDE - only this one, because IntelliJ runs
	 *                                              Cucumber itself and no TestNG suite
	 *                                              ever exists
	 *
	 * Whichever arrives first sends the complete report; the guard makes the other a
	 * no-op, so a plain run does not send two copies.
	 */
	@AfterAll
	public static void cucumberAfterAll() {
		sendReportEmail();
	}

	/**
	 * Sends the report, at most once per run.
	 *
	 * Nothing in here may throw. It runs after the tests have finished, so losing a run's
	 * result to a mail problem would be worse than not sending the mail.
	 */
	public static void sendReportEmail() {

		if (!REPORT_SENT.compareAndSet(false, true)) {
			return;
		}

		try {
			if (config == null) {
				config = new ConfigDataProvider();
			}

			List<String> attachments = new ArrayList<>();

			if (reportPath != null && new File(reportPath).exists()) {
				attachments.add(reportPath);
			}

			String screenshotPath = System.getProperty("last.screenshot.path");

			if (screenshotPath != null && !screenshotPath.isEmpty() && new File(screenshotPath).exists()) {
				attachments.add(screenshotPath);
			}

			if (attachments.isEmpty()) {
				System.out.println("No report was produced, so there is nothing to email.");
				return;
			}

			String body = "<h3>UNCS Test Automation Report</h3>"
					+ "<p>The attached report covers the run that finished at "
					+ Helper.getCurrentDateTime() + ".</p>";

			if (screenshotPath != null && !screenshotPath.isEmpty()) {
				body += "<p><b>Last screenshot captured:</b><br>"
						+ "<img src='cid:screenshot' width='600'/></p>";
			}

			MailUtils.sendEmail(
					config.getMailHost(),
					config.getMailPort(),
					config.getMailAuth(),
					config.getMailStartTLS(),
					config.getMailFrom(),
					config.getMailPassword(),
					config.getMailTo(),
					"UNCS Test Automation Report - " + Helper.getCurrentDateTime(),
					body,
					attachments);

		} catch (Exception e) {
			System.out.println("The report email could not be prepared: " + e.getMessage());
		}
	}

	/**
	 * Puts the screenshot into the Cucumber report.
	 *
	 * Best effort. A run that cannot read back the file it just wrote is still a run worth
	 * reporting, and the path is named in the text report either way.
	 */
	private static void attachScreenshot(Scenario scenario, String screenshotPath) {

		if (screenshotPath == null || screenshotPath.isBlank()) {
			return;
		}

		try {
			byte[] picture = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(screenshotPath));
			scenario.attach(picture, "image/png", "the screen when it failed");
		} catch (Exception cannotAttach) {
			System.out.println("Could not attach the screenshot to the Cucumber report: "
					+ cannotAttach.getMessage());
		}
	}

	@After
	public void cucumberTearDown(Scenario scenario) {

		try {
			String screenshotPath = Helper.captureScreenShot(driver);

			// Remembered for the report email, which is sent long after this scenario has
			// gone and has no other way to know what the run last saw.
			System.setProperty("last.screenshot.path", screenshotPath == null ? "" : screenshotPath);

			if (scenario.isFailed()) {

				// Built once, before the browser is closed: the calls it made are read off
				// the page, and a closed page has none to give.
				String failureReport = FailureReport.of(scenario, driver, screenshotPath);

				// To the console, so a terminal run shows it without opening anything.
				System.out.println(failureReport);

				// To the Cucumber report and the JSON, so CI carries it too - along with
				// the picture, which otherwise reached the Extent HTML alone.
				scenario.attach(failureReport.getBytes(java.nio.charset.StandardCharsets.UTF_8),
						"text/plain", "how to reproduce, and what the API said");

				attachScreenshot(scenario, screenshotPath);

				if (logger != null) {
					logger.fail("<pre>" + failureReport
							.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
							+ "</pre>");
					logger.fail("Scenario Failed",
							MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
				}

			} else if (logger != null) {
				logger.pass("Scenario Passed",
						MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
			}

		} catch (Exception e) {
			System.out.println("Could not record the end of the scenario: " + e.getMessage());

		} finally {

			try {
				if (report != null) {
					report.flush();
				}
			} catch (Exception e) {
				System.out.println("Could not flush the report: " + e.getMessage());
			}

			BrowserFactory.quitBrowser(driver);
			driver = null;
		}
	}
}
