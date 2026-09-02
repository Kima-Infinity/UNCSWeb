package com.uncs.support;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Every call the page made to the API, so a failure can say what the server actually
 * answered rather than only what the screen failed to show.
 *
 * This is the one thing the Selenium suite could not do without standing a proxy in front
 * of the browser. Playwright reports each response as it arrives, so the body of the call
 * that went wrong is available at the moment it goes wrong - which is the difference
 * between "the Transfer hub did not open" and "POST /api/v2/transfer/list answered 500 with
 * {\"message\":\"upstream timeout\"}".
 *
 * Two decisions worth stating:
 *
 * Only bodies of calls that went wrong are kept. Reading a response body costs a round trip
 * to the browser and holding every one would grow without bound; a 200 that the test was
 * happy with explains nothing anyway.
 *
 * The buffer is bounded and keyed weakly, so a long scenario cannot grow it without limit
 * and a closed page does not keep its log alive.
 */
public final class ApiLog {

	/** How many calls to remember per page. Enough to see the run-up to a failure. */
	private static final int KEPT = 60;

	/** A body longer than this is trimmed; the useful part of an error is at the front. */
	private static final int BODY_LIMIT = 2000;

	/** Field names whose values must never reach a report. */
	private static final List<String> SECRET_FIELDS =
			List.of("password", "newPassword", "confirm_password", "pin", "token",
					"captcha", "otp", "otpVerifyCode");

	private static final Map<Page, Deque<Call>> LOGS =
			Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * One call to the API.
	 *
	 * @param method       GET, POST and so on
	 * @param url          the address called
	 * @param status       the HTTP status it answered with
	 * @param code         the application's own error code, when it gives one
	 * @param requestBody  what was sent, for the calls that send anything
	 * @param responseBody what came back, kept only when the call went wrong
	 */
	public record Call(String method, String url, int status, String code,
			String requestBody, String responseBody) {

		/** Whether this is a call worth showing first in a failure report. */
		public boolean failed() {
			return status >= 400;
		}

		@Override
		public String toString() {

			StringBuilder out = new StringBuilder()
					.append(status).append(' ').append(method).append(' ').append(url);

			if (code != null && !code.isBlank()) {
				out.append("\n            code:     ").append(code);
			}

			if (requestBody != null && !requestBody.isBlank()) {
				out.append("\n            sent:     ").append(requestBody);
			}

			if (responseBody != null && !responseBody.isBlank()) {
				out.append("\n            answered: ").append(responseBody);
			}

			return out.toString();
		}
	}

	private ApiLog() {
	}

	/** Starts recording for {@code page}. Called once, when the page is created. */
	public static void attach(Page page) {

		Deque<Call> calls = new ArrayDeque<>();

		LOGS.put(page, calls);

		page.onResponse(response -> {

			if (!isApi(response.url())) {
				return;
			}

			record(calls, toCall(response));
		});
	}

	/** Everything recorded for this page, oldest first. Never null. */
	public static List<Call> calls(Page page) {

		Deque<Call> calls = LOGS.get(page);

		if (calls == null) {
			return List.of();
		}

		synchronized (calls) {
			return List.copyOf(calls);
		}
	}

	/** Only the calls that answered with an error, which is where a failure usually starts. */
	public static List<Call> failures(Page page) {

		return calls(page).stream().filter(Call::failed).toList();
	}

	private static Call toCall(Response response) {

		String requestBody = null;
		String responseBody = null;

		try {
			requestBody = trim(response.request().postData());
		} catch (Exception noBody) {
			// A GET has none, which is not worth reporting.
		}

		if (response.status() >= 400) {
			try {
				responseBody = trim(response.text());
			} catch (Exception cannotRead) {
				// A redirect or a body already consumed. The status still tells a story.
				responseBody = "(body unavailable: " + cannotRead.getMessage() + ")";
			}
		}

		return new Call(response.request().method(), response.url(), response.status(),
				codeIn(responseBody), requestBody, responseBody);
	}

	/**
	 * The application's own error code out of a response body, if it names one.
	 *
	 * The HTTP status says a request failed; the code says which way. A 400 carrying
	 * MESSAGE_RATE_LIMIT is a different morning's work from a 400 carrying a validation
	 * failure, and reading it out of the body by eye is a step nobody should have to take.
	 */
	private static String codeIn(String body) {

		if (body == null) {
			return null;
		}

		java.util.regex.Matcher named = java.util.regex.Pattern
				.compile("\"code\"\\s*:\\s*\"([^\"]+)\"").matcher(body);

		return named.find() ? named.group(1) : null;
	}

	private static void record(Deque<Call> calls, Call call) {

		synchronized (calls) {
			if (calls.size() >= KEPT) {
				calls.removeFirst();
			}
			calls.addLast(call);
		}
	}

	/**
	 * Whether a URL is one of the application's own calls.
	 *
	 * Anything else - fonts, images, analytics, the browser's own chatter - is noise in a
	 * failure report and there is a great deal of it.
	 */
	private static boolean isApi(String url) {

		return url.contains("/api/");
	}

	private static String trim(String value) {

		if (value == null) {
			return null;
		}

		String flat = mask(value.replaceAll("\\s+", " ").trim());

		return flat.length() <= BODY_LIMIT ? flat : flat.substring(0, BODY_LIMIT) + "... (trimmed)";
	}

	/**
	 * Hides secrets before a body is written anywhere.
	 *
	 * This report reaches the console, the Cucumber report, the Extent HTML and the report
	 * email. A sign in posts the account password, so without this the very first failure
	 * report would publish it to a CI log and an inbox - which is exactly what the first run
	 * of this feature did before the masking went in.
	 *
	 * The value is replaced rather than the field removed, because knowing that a password
	 * was sent at all is usually the point of reading the body.
	 */
	private static String mask(String body) {

		String masked = body;

		for (String field : SECRET_FIELDS) {

			// form encoded: field=value
			masked = masked.replaceAll("(?i)(\\b" + field + "=)[^&\\s]*", "$1***");

			// json: "field": "value"
			masked = masked.replaceAll("(?i)(\"" + field + "\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
		}

		return masked;
	}
}
