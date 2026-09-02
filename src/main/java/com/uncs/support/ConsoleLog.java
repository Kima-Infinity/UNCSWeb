package com.uncs.support;

import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Page;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps the last few console messages a page produced, so a failure can be explained.
 *
 * This exists because of a real difference between the two drivers rather than a preference.
 * Selenium accumulated console output for you and handed it over on request
 * ({@code driver.manage().logs().get(LogType.BROWSER)}), so a diagnostic could ask for it
 * after the fact. Playwright has no such buffer: console output is an event, and anything
 * not listening when it fires never sees it. So something has to subscribe when the page is
 * created, which is what {@link #attach} is for, and hold what arrives - which is here.
 *
 * Bounded on purpose. A chatty page over a long scenario would otherwise grow this without
 * limit, and only the tail is ever of interest: earlier entries belong to page load.
 *
 * Keyed weakly so a page that has been closed does not keep its log alive.
 */
public final class ConsoleLog {

	/** How many messages to keep. The diagnostic prints twelve; a little headroom is free. */
	private static final int KEPT = 50;

	private static final Map<Page, Deque<String>> LOGS =
			Collections.synchronizedMap(new WeakHashMap<>());

	private ConsoleLog() {
	}

	/** Starts recording for {@code page}. Called once, when the page is created. */
	public static void attach(Page page) {

		Deque<String> messages = new ArrayDeque<>();

		LOGS.put(page, messages);

		page.onConsoleMessage((ConsoleMessage message) -> {
			synchronized (messages) {
				if (messages.size() >= KEPT) {
					messages.removeFirst();
				}
				messages.addLast("[" + message.type() + "] " + message.text());
			}
		});
	}

	/** What the page has logged so far, oldest first. Never null. */
	public static List<String> messages(Page page) {

		Deque<String> messages = LOGS.get(page);

		if (messages == null) {
			return List.of();
		}

		synchronized (messages) {
			return List.copyOf(messages);
		}
	}
}
