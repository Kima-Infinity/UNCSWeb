package com.uncs.utility;

import com.microsoft.playwright.Page;
import com.uncs.support.ApiLog.Call;
import io.cucumber.java.Scenario;

import java.util.List;

/**
 * What a failed scenario should say for itself.
 *
 * The suite already knew how to report that something failed. What it could not answer was
 * the next two questions anybody actually asks: what do I do to see this myself, and what
 * did the server say. This assembles both - the steps from {@link FeatureSteps}, the calls
 * from {@link com.uncs.support.ApiLog} - so a failure arrives explained rather than merely announced.
 *
 * Written as plain text on purpose. It goes into the Cucumber report, the Extent report, the
 * console and the email, and text is the only form all four render the same way.
 */
public final class FailureReport {

	/** How many recent calls to list when none of them actually failed. */
	private static final int RECENT_CALLS = 8;

	private FailureReport() {
	}

	/**
	 * The whole account of a failure.
	 *
	 * @param scenario the scenario Cucumber has just finished
	 * @param page     the tab it was working in, for the calls it made
	 */
	public static String of(Scenario scenario, Page page) {

		return of(scenario, page, null);

	}

	/**
	 * The whole account of a failure, including where the picture of it was saved.
	 *
	 * @param scenario       the scenario Cucumber has just finished
	 * @param page           the tab it was working in, for the calls it made
	 * @param screenshotPath where the screen was saved, or null if it could not be
	 */
	public static String of(Scenario scenario, Page page, String screenshotPath) {

		StringBuilder out = new StringBuilder();

		out.append("========================================================================\n")
				.append("FAILED: ").append(scenario.getName()).append('\n')
				.append("========================================================================\n\n");

		out.append(reproduction(scenario)).append('\n');
		out.append(apiSection(page)).append('\n');
		out.append(screenshotSection(screenshotPath)).append('\n');
		out.append(rerun(scenario));

		return out.toString();
	}


	/**
	 * The account of a failed TestNG test, as opposed to a failed Cucumber scenario.
	 *
	 * Both are failed tests and both deserve the same explanation, but a TestNG test has no
	 * feature file to read its steps from. So the steps become what the test is - its class
	 * and method - and the rerun line becomes the one that runs it on its own. The API and
	 * screenshot sections are the same ones the scenarios get, because the questions a
	 * reader asks do not change with the runner.
	 *
	 * @param testClass      the class the failing test lives in
	 * @param testMethod     the method that failed
	 * @param page           the tab it was working in, for the calls it made
	 * @param screenshotPath where the screen was saved, or null if it could not be
	 */
	public static String ofTest(String testClass, String testMethod, Page page, String screenshotPath) {

		StringBuilder out = new StringBuilder();

		out.append("========================================================================\n")
				.append("FAILED: ").append(testClass).append('.').append(testMethod).append('\n')
				.append("========================================================================\n\n");

		out.append("STEPS TO REPRODUCE\n")
				.append("  A TestNG test rather than a scenario, so the steps are the test itself:\n")
				.append("  ").append(testClass).append('.').append(testMethod).append("\n\n");

		out.append(apiSection(page)).append('\n');
		out.append(screenshotSection(screenshotPath)).append('\n');

		out.append("RUN THIS ONE AGAIN\n")
				.append("  mvn test -Dtest=").append(shortName(testClass)).append('#').append(testMethod)
				.append("\n  Add -Dheadless=false to watch it happen.\n");

		return out.toString();
	}

	private static String shortName(String testClass) {

		int lastDot = testClass.lastIndexOf('.');

		return lastDot < 0 ? testClass : testClass.substring(lastDot + 1);
	}

	/** The steps of the scenario, as a person would follow them. */
	private static String reproduction(Scenario scenario) {

		List<String> steps = FeatureSteps.of(scenario);

		StringBuilder out = new StringBuilder("STEPS TO REPRODUCE\n");

		out.append("  Feature: ")
				.append(scenario.getUri().getPath().replaceAll(".*/", ""))
				.append(", line ").append(scenario.getLine()).append("\n\n");

		if (steps.isEmpty()) {
			out.append("  (the feature file could not be read - see the console)\n");
			return out.toString();
		}

		int number = 1;

		for (String step : steps) {
			out.append("  ").append(number++).append(". ").append(step).append('\n');
		}

		return out.toString();
	}

	/**
	 * The calls the page made, with the ones that answered badly written out in full.
	 *
	 * When nothing failed at the network level that is worth saying too: it means the server
	 * was content and the problem is in front of it, which sends the reader to the screen
	 * rather than to the logs.
	 */
	private static String apiSection(Page page) {

		if (page == null) {
			return "API CALLS\n  (no page - the browser was already closed)\n";
		}

		List<Call> failed = com.uncs.support.ApiLog.failures(page);

		StringBuilder out = new StringBuilder("API CALLS\n");

		if (!failed.isEmpty()) {
			out.append("  Calls that answered with an error:\n");
			for (Call call : failed) {
				out.append("    ").append(call).append('\n');
			}
			return out.toString();
		}

		List<Call> all = com.uncs.support.ApiLog.calls(page);

		if (all.isEmpty()) {
			out.append("  The page made no API calls at all, which is itself worth knowing:\n")
					.append("  nothing was asked of the server, so the failure is in the page.\n");
			return out.toString();
		}

		List<Call> recent = all.size() > RECENT_CALLS
				? all.subList(all.size() - RECENT_CALLS, all.size())
				: all;

		out.append("  Every call succeeded, so the server was content and the problem is in\n")
				.append("  front of it. The last ").append(recent.size()).append(" for context:\n");

		for (Call call : recent) {
			out.append("    ").append(call.status()).append(' ')
					.append(call.method()).append(' ').append(call.url()).append('\n');
		}

		return out.toString();
	}

	/**
	 * Where the picture of the failure was saved.
	 *
	 * Named in the text as well as attached, because the text is what reaches a terminal
	 * and an email, and neither of those can show an image. A path the reader can open is
	 * the next best thing.
	 */
	private static String screenshotSection(String screenshotPath) {

		if (screenshotPath == null || screenshotPath.isBlank()) {
			return "SCREENSHOT\n  (none was taken)\n";
		}

		return "SCREENSHOT\n  " + screenshotPath + "\n";
	}

	/** The command that runs this one scenario again, so the reader does not have to work it out. */
	private static String rerun(Scenario scenario) {

		return "RUN THIS ONE AGAIN\n"
				+ "  mvn test -Dcucumber.filter.name=\"" + scenario.getName() + "\"\n"
				+ "  Add -Dheadless=false to watch it happen.\n";
	}
}
