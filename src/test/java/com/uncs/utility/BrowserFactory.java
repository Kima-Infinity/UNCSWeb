package com.uncs.utility;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts and stops the browser, the Playwright way.
 *
 * Selenium handed the suite one object, a WebDriver, that was both the browser and the page.
 * Playwright separates them:
 *
 *   Playwright      the connection to the driver process. One per run.
 *   Browser         the browser process itself. One per run.
 *   BrowserContext  an isolated profile - its own cookies and storage. Cheap to make.
 *   Page            a tab.
 *
 * The Selenium version started a whole browser per scenario and quit it in the Cucumber
 * hooks. That isolation is worth keeping - a scenario should not inherit the session of
 * whatever ran before it, and this suite signs in as different accounts on purpose - but
 * paying for a browser launch to get it is not. So the Playwright and Browser are opened
 * once per run and each scenario gets a fresh context instead.
 *
 * The registry and the shutdown hook are carried over deliberately. The Selenium version
 * grew them after an interrupted run left thirty seven Chrome processes behind, and
 * Playwright is no safer: a JVM that exits without tearing down leaves its driver process
 * and browser running just the same.
 */
public class BrowserFactory {

	/**
	 * The viewport every run gets.
	 *
	 * Selenium maximised a visible window and asked for 1920x1080 when headless. Playwright
	 * has no window to maximise, so both modes get the same size and the desktop layout is
	 * what the locators always meet.
	 */
	private static final int VIEWPORT_WIDTH = 1920;

	private static final int VIEWPORT_HEIGHT = 1080;

	/**
	 * How long a page load may take.
	 *
	 * Playwright's own default is 30 seconds; the Selenium suite set no page load timeout at
	 * all, so a slow page simply took as long as it took. Ninety seconds keeps the old
	 * behaviour in practice while still ending a run that is truly stuck.
	 */
	private static final double NAVIGATION_TIMEOUT_MILLIS = 90_000;

	/**
	 * Every context this factory has opened and not yet closed.
	 *
	 * A context is added the moment it is created and removed the moment it is closed, so in
	 * a healthy run this is empty between scenarios.
	 */
	private static final Set<BrowserContext> OPEN_CONTEXTS = ConcurrentHashMap.newKeySet();

	private static Playwright playwright;

	private static Browser browser;

	static {
		/*
		 * A last line of defence, not the usual route.
		 *
		 * Scenarios close their own context in the After hook, and that is what should be
		 * relied on. This exists for the runs that never reach a tear down at all: a
		 * scenario that dies in a way TestNG cannot recover from, a suite stopped with
		 * Ctrl+C, a JVM that exits early.
		 *
		 * A hard kill still cannot be caught. Nothing can be done about that from inside
		 * the JVM, so it is not attempted.
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(BrowserFactory::quitAllBrowsers, "browser-cleanup"));
	}

	/**
	 * Whether the run should start the browser without a visible window.
	 *
	 * Checked in this order, so a single run can be switched without editing a file:
	 *   -Dheadless=true on the command line   (mvn test -Dheadless=true)
	 *   headless=true in Config/config.properties
	 *   false
	 *
	 * Anything other than "true" (ignoring case) counts as false.
	 */
	public static boolean isHeadless() {

		String override = System.getProperty("headless");

		if (override != null && !override.isBlank()) {
			return Boolean.parseBoolean(override.trim());
		}

		return new ConfigDataProvider().isHeadless();
	}

	/**
	 * Opens a tab at {@code url} and hands back the page everything else works with.
	 *
	 * There is no implicit wait to set. Selenium needed one because findElement asked the
	 * page a question the instant it was called; every Playwright action waits for the
	 * element to be there, visible, stable and able to receive the event before it acts.
	 */
	public static synchronized Page startBrowser(String url) {

		if (browser == null) {
			playwright = Playwright.create();

			browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
					.setHeadless(isHeadless())
					// Chromium's own default window would put the site in its mobile layout
					// before the viewport below is applied.
					.setArgs(List.of("--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT)));
		}

		BrowserContext context = browser.newContext(new Browser.NewContextOptions()
				.setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));

		context.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MILLIS);

		OPEN_CONTEXTS.add(context);

		Page page = context.newPage();

		// Console output is an event in Playwright, not a buffer that can be asked for
		// afterwards the way Selenium's was, so the diagnostics have to start listening
		// before anything happens on the page.
		com.uncs.support.ConsoleLog.attach(page);

		page.navigate(url);

		return page;
	}

	/**
	 * Ends a scenario's session, and never throws while doing it.
	 *
	 * Called from tear down blocks whose whole job is to run no matter what already went
	 * wrong, so a page that is already gone, or a context that has died on its own, must not
	 * turn into a second failure on top of the first. A null page is accepted for the same
	 * reason: a scenario that failed before the browser opened still runs its tear down.
	 *
	 * The browser itself stays up for the next scenario; {@link #quitAllBrowsers} closes it.
	 */
	public static void quitBrowser(Page page) {

		if (page == null) {
			return;
		}

		BrowserContext context = null;

		try {
			context = page.context();

			if (!page.isClosed()) {
				page.close();
			}

		} catch (Exception e) {
			System.out.println("The page could not be closed cleanly: " + e.getMessage());
		}

		if (context == null) {
			return;
		}

		try {
			context.close();

		} catch (Exception e) {
			System.out.println("The browser context could not be closed cleanly: " + e.getMessage());

		} finally {
			OPEN_CONTEXTS.remove(context);
		}
	}

	/** Closes anything still open, including the browser. Runs on JVM shutdown. */
	public static synchronized void quitAllBrowsers() {

		if (!OPEN_CONTEXTS.isEmpty()) {
			System.out.println("Closing " + OPEN_CONTEXTS.size() + " browser context(s) still open at shutdown");

			for (BrowserContext context : Set.copyOf(OPEN_CONTEXTS)) {
				try {
					context.close();
				} catch (Exception e) {
					System.out.println("A context could not be closed cleanly: " + e.getMessage());
				} finally {
					OPEN_CONTEXTS.remove(context);
				}
			}
		}

		closeQuietly(browser, "browser");
		browser = null;

		closeQuietly(playwright, "Playwright");
		playwright = null;
	}

	/** How many contexts this factory currently believes are open. */
	public static int openBrowserCount() {

		return OPEN_CONTEXTS.size();

	}

	private static void closeQuietly(AutoCloseable closeable, String what) {

		if (closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (Exception cannotClose) {
			System.out.println("Could not close the " + what + ": " + cannotClose.getMessage());
		}
	}
}
