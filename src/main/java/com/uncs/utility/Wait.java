package com.uncs.utility;

import java.util.function.BooleanSupplier;

/**
 * Waits for something the driver cannot wait for on its own.
 *
 * Playwright waits before every action, and a Locator can wait to be visible, hidden,
 * attached or detached, which covers almost everything the Selenium suite used an explicit
 * wait for. What it does not cover is a condition about the page rather than about one
 * element: two pickers that must both be filled in, a checkbox that must read as ticked, a
 * balance that must have fallen. Selenium spelled those {@code wait.until(d -> ...)}; this
 * is where they live now.
 *
 * Deliberately small. Anything that can be said with a Locator's own wait should be said
 * that way instead - it reports better and it does not poll.
 */
public final class Wait {

    /** How often to re-ask. Short enough to feel immediate, long enough not to spin. */
    private static final long POLL_MILLIS = 250;

    private Wait() {
        // Static holder; there is nothing to construct.
    }

    /**
     * Polls {@code condition} until it holds or the time runs out.
     *
     * A condition that throws counts as not yet true rather than as a failure: these are
     * asked of a page that is often mid-render, where reading an element that has just been
     * replaced is normal and means only that the answer is not ready.
     *
     * @return whether the condition held before the timeout
     */
    public static boolean until(BooleanSupplier condition, int timeoutSeconds) {

        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {

            try {
                if (condition.getAsBoolean()) {
                    return true;
                }
            } catch (Exception notReadyYet) {
                // Look again on the next turn.
            }

            sleep(POLL_MILLIS);
        }

        return false;
    }

    /**
     * The same, but says so when it gives up.
     *
     * For the callers that treated a timeout as the end of the scenario. The message is the
     * caller's, because only the caller knows what it was waiting for.
     */
    public static void untilOrFail(BooleanSupplier condition, int timeoutSeconds, String what) {

        if (!until(condition, timeoutSeconds)) {
            throw new IllegalStateException(what + " within " + timeoutSeconds + " seconds");
        }
    }

    public static void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Every element the locator matches, once at least one of them exists.
     *
     * This is the one place the port had to add something rather than remove it. Selenium's
     * findElements was covered by a five second implicit wait, so a list read a moment too
     * early was quietly retried until the page caught up. Playwright has no implicit wait
     * and Locator.all() answers immediately, so the same call returns an empty list and the
     * caller decides the dialog is empty - which is exactly how the language switcher and
     * the country list first failed after the port.
     *
     * Waiting for the first match restores the old behaviour honestly: an empty list now
     * means the page really has none, not that nobody waited.
     *
     * @return the matches, or an empty list if none arrived in time
     */
    public static java.util.List<com.microsoft.playwright.Locator> all(
            com.microsoft.playwright.Locator locator, int timeoutSeconds) {

        try {
            locator.first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
                    .setTimeout(timeoutSeconds * 1000L));
        } catch (Exception noneArrived) {
            return java.util.List.of();
        }

        return locator.all();
    }

    /** The same, with the five seconds Selenium's implicit wait used to give. */
    public static java.util.List<com.microsoft.playwright.Locator> all(
            com.microsoft.playwright.Locator locator) {

        return all(locator, 5);
    }

    /**
     * Whether {@code locator} turns up within a few seconds.
     *
     * The port's sharpest edge. Selenium's isDisplayed() ran under a five second implicit
     * wait, so asking "is the dialog there?" the instant after a page loaded quietly waited
     * for it. Playwright answers immediately and truthfully - not yet - and the caller walks
     * on. That is how the 2FA prompt came to be missed: the check said no prompt, navigation
     * went ahead, and the dialog then rendered its full screen backdrop over everything, so
     * every click for the rest of the scenario landed on the overlay instead.
     *
     * Use this wherever the question is "has it arrived", and plain isVisible() where the
     * question is "is it there right now".
     */
    public static boolean appears(com.microsoft.playwright.Locator locator, int timeoutSeconds) {

        try {
            locator.first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                    .setTimeout(timeoutSeconds * 1000L));
            return true;
        } catch (Exception neverArrived) {
            return false;
        }
    }

    /** The same, with the five seconds Selenium's implicit wait used to give. */
    public static boolean appears(com.microsoft.playwright.Locator locator) {

        return appears(locator, 5);
    }
}
