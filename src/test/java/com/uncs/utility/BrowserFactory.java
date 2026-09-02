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
	 * The size a headless run gets.
	 *
	 * Headless has no window to maximise and no screen to measure, and a build agent must
	 * render the same page every time or a scenario can pass on a desk and fail in CI for a
	 * reason nobody can see. So headless is always this, whatever the machine underneath it
	 * happens to have. It is also what a headed run falls back to if the screen cannot be
	 * measured for the warning below.
	 */
	private static final int HEADLESS_WIDTH = 1920;

	private static final int HEADLESS_HEIGHT = 1080;

	/**
	 * The width below which the site stops being the one the suite was written against.
	 *
	 * Somewhere around here it changes to its mobile layout and the locators stop matching.
	 * A maximised window is whatever the screen is, so this cannot be enforced - but a run
	 * on a screen narrower than this is worth a word before the failures start arriving,
	 * because the cause is the machine rather than the application.
	 */
	private static final int NARROW_SCREEN_WIDTH = 1280;

	/** Whether the narrow-screen warning has already been given this run. */
	private static boolean warned;

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
	 * How the browser window is asked for.
	 *
	 * A headed run is maximised, so it fills the screen it is actually on and nothing hangs
	 * off the edge. That matters on a scaled display: a 1920x1080 panel at 125% scaling
	 * offers applications 1536x864, so the fixed 1920 this used to ask for was wider than
	 * the screen had to give and the remainder spilled onto whatever monitor sat beside it.
	 * Maximising asks the window manager for the answer instead of working it out here, and
	 * the window manager is never wrong about its own screen.
	 *
	 * Headless has no window to maximise, so there the size is stated outright.
	 */
	private static List<String> launchArgs() {

		return isHeadless()
				? List.of("--window-size=" + HEADLESS_WIDTH + "," + HEADLESS_HEIGHT)
				: List.of("--start-maximized");
	}

	/**
	 * Says so when the screen is too narrow for the layout the suite expects.
	 *
	 * Once per run, and only a warning: the run is still worth attempting and the machine is
	 * not something the suite can change. But a locator that has always matched suddenly not
	 * matching is a confusing thing to debug, and "this screen is narrower than the desktop
	 * layout needs" is the sentence that saves the hour.
	 */
	private static synchronized void warnIfScreenIsNarrow() {

		if (warned || isHeadless()) {
			return;
		}

		warned = true;

		int width = screenWidth();

		if (width < NARROW_SCREEN_WIDTH) {
			System.out.println("This screen is " + width + " logical pixels wide, under the "
					+ NARROW_SCREEN_WIDTH + " the desktop layout needs. The site may render "
					+ "its mobile layout, in which case locators will not match.");
		}
	}

	/**
	 * The primary screen's width in logical pixels - the unit Chromium sizes windows in.
	 *
	 * The default screen device rather than getMaximumWindowBounds, which is documented to
	 * be allowed to return the whole virtual desktop on a multi-screen machine, and would
	 * therefore call a narrow laptop wide as soon as a second monitor was plugged in.
	 */
	private static int screenWidth() {

		try {
			if (java.awt.GraphicsEnvironment.isHeadless()) {
				return HEADLESS_WIDTH;
			}

			return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration().getBounds().width;

		} catch (Throwable noScreen) {
			// A machine with no display, or one that will not say. Assume it is fine rather
			// than warn about something that was never measured.
			return HEADLESS_WIDTH;
		}
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
					.setArgs(launchArgs()));
		}

		warnIfScreenIsNarrow();

		// A headed run gets no viewport at all, which is what lets the page fill the
		// maximised window. Playwright's default is to fix the viewport at 1280x720 and
		// leave the rest of the window blank, so the setting has to be turned off rather
		// than merely left alone - setViewportSize(null) is how that is spelled.
		Browser.NewContextOptions options = new Browser.NewContextOptions();

		if (isHeadless()) {
			options.setViewportSize(HEADLESS_WIDTH, HEADLESS_HEIGHT);
		} else {
			options.setViewportSize(null);
		}

		BrowserContext context = browser.newContext(options);

		context.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MILLIS);

		OPEN_CONTEXTS.add(context);

		Page page = context.newPage();

		// Console output is an event in Playwright, not a buffer that can be asked for
		// afterwards the way Selenium's was, so the diagnostics have to start listening
		// before anything happens on the page.
		com.uncs.support.ConsoleLog.attach(page);

		// Responses are events too, and for the same reason the listener has to be in place
		// before the first navigation: the calls that set the page up are never seen
		// otherwise, and those are often the ones that explain a failure.
		com.uncs.support.ApiLog.attach(page);

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
