package com.uncs.support;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evidence gathered at the moment a wait gives up.
 *
 * A timeout names the locator it was watching and nothing else, which is the least useful
 * half of the problem: it says what did not happen and nothing at all about why. This
 * collects the evidence at the moment of the failure, so the next occurrence is diagnosed
 * rather than theorised about.
 *
 * Everything here is best effort. A diagnostic that throws while reporting a failure
 * replaces the real error with its own, which is worse than saying nothing.
 */
public final class BrowserDiagnostics {

	private BrowserDiagnostics() {
	}

	/**
	 * Console output from the page, which is where a failed request surfaces.
	 *
	 * Read from {@link ConsoleLog} rather than from the driver. Selenium accumulated console
	 * output and handed it over on request; Playwright fires it as an event, so it has to
	 * have been listened for from the moment the page opened. BrowserFactory does that.
	 */
	public static String consoleLog(Page page) {

		try {
			List<String> all = ConsoleLog.messages(page);

			if (all.isEmpty()) {
				return "console: (nothing logged)";
			}

			// The tail is what matters - earlier entries belong to page load.
			List<String> tail = all.size() > 12 ? all.subList(all.size() - 12, all.size()) : all;

			return "console (last " + tail.size() + " of " + all.size() + "):\n      "
					+ tail.stream()
					.map(entry -> trim(entry, 200))
					.collect(Collectors.joining("\n      "));

		} catch (Exception e) {
			return "console: unavailable (" + e.getMessage() + ")";
		}
	}

	/**
	 * What the thread looks like around a message the test expected to be gone.
	 *
	 * Answers the questions the timeout leaves open: did the deletion placeholder arrive
	 * at all, is the text still in a real bubble, and is more than one element matching
	 * the locator the wait was watching.
	 */
	public static String messageState(Page page, String message) {

		StringBuilder out = new StringBuilder();

		try {
			List<Locator> matches = page.locator(
					"xpath=//p[normalize-space()=" + xpathLiteral(message) + "]").all();

			out.append("matches for the message text: ").append(matches.size());

			for (Locator match : matches) {
				out.append("\n      displayed=").append(safeDisplayed(match));

				try {
					Locator bubble = match.locator(
							"xpath=./ancestor::div[contains(@class,'rounded-2xl')][1]").first();
					out.append(" bubble=\"")
							.append(trim(bubble.innerText().replace('\n', ' '), 120))
							.append('"');

				} catch (Exception e) {
					out.append(" bubble=(not found)");
				}
			}

			int tombstones = page.locator(
					"xpath=//*[normalize-space()='Message was deleted!']").count();

			out.append("\n      deletion placeholders in thread: ").append(tombstones);

			int confirmOpen = page.locator("xpath=//button[normalize-space()='yes']").count();

			out.append("\n      confirmation still open: ").append(confirmOpen > 0);

		} catch (Exception e) {
			out.append("message state unavailable (").append(e.getMessage()).append(')');
		}

		return out.toString();
	}

	/** The most recent calls the page made to the API, from the Performance API. */
	public static String recentApiCalls(Page page) {

		try {
			Object result = page.evaluate(
					"() => performance.getEntriesByType('resource')"
							+ ".filter(function(r){ return r.name.indexOf('/api/') !== -1; })"
							+ ".slice(-8)"
							+ ".map(function(r){ return r.name.split('/api/')[1]"
							+ " + ' dur=' + Math.round(r.duration) + 'ms'"
							+ " + ' size=' + (r.transferSize || 0); })");

			if (!(result instanceof List) || ((List<?>) result).isEmpty()) {
				return "recent api calls: (none recorded)";
			}

			return "recent api calls:\n      " + ((List<?>) result).stream()
					.map(String::valueOf)
					.collect(Collectors.joining("\n      "));

		} catch (Exception e) {
			return "recent api calls: unavailable (" + e.getMessage() + ")";
		}
	}

	/** Everything at once, for attaching to a failure message. */
	public static String report(Page page, String context, String message) {

		return "\n--- browser diagnostics: " + context + " ---"
				+ "\n      " + messageState(page, message)
				+ "\n      " + recentApiCalls(page)
				+ "\n      " + consoleLog(page)
				+ "\n--- end diagnostics ---";

	}

	private static boolean safeDisplayed(Locator element) {

		try {
			return element.isVisible();

		} catch (Exception e) {
			return false;
		}
	}

	private static String trim(String value, int limit) {

		if (value == null) {
			return "";
		}
		return value.length() <= limit ? value : value.substring(0, limit) + "...";
	}

	/**
	 * Wraps a value for use inside an XPath expression.
	 *
	 * The messages under test carry timestamps with colons and could carry quotes, and a
	 * naive concatenation would break the expression rather than fail the assertion.
	 */
	private static String xpathLiteral(String value) {

		if (!value.contains("'")) {
			return "'" + value + "'";
		}
		if (!value.contains("\"")) {
			return "\"" + value + "\"";
		}
		return "concat('" + value.replace("'", "',\"'\",'") + "')";
	}
}
