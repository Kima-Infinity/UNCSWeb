package com.uncs.pages;

import com.uncs.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * Opening the right click menus the application uses for chats and for messages.
 *
 * Neither a chat row nor a message bubble has a three dot button or a hover affordance,
 * so a context click is the only way into either menu. Three things make that unreliable
 * enough to be worth a shared helper rather than a bare right click, and all three cost
 * the suite scenarios before they were handled:
 *
 *   - A context click goes to the middle of the element. A message near the bottom of a
 *     long thread can be scrolled far enough out of view that the middle is off screen,
 *     and the click then lands on whatever is there instead. Scrolling the element to the
 *     centre of the viewport first is what fixes it.
 *
 *   - The list re-renders under the test. Sending a message rebuilds the thread and
 *     searching rebuilds the chat list, so an element captured a moment earlier is
 *     detached by the time it is clicked. That is why this takes a locator rather than a
 *     Locator: the target is found again on every attempt, inside the retry, so a
 *     re-render costs one attempt instead of the whole scenario.
 *
 *   - The first click is occasionally swallowed while the thread is still settling after
 *     a send, and no menu appears at all. A single extra attempt turns that from a failed
 *     run into a passed one.
 */
final class ContextMenus {

	private ContextMenus() {
	}

	/**
	 * Right clicks a target and waits for a menu to prove it opened.
	 *
	 * @param target locator for the thing to right click, resolved fresh on each attempt
	 * @param marker something only the open menu contains, so a menu that failed to
	 *               appear is reported as such rather than silently passing
	 */
	static void open(Page page, Locator target, Locator marker) {


		for (int attempt = 1; attempt <= 3; attempt++) {

			try {
				// Get rid of any menu that is already showing before opening a new one.
				//
				// The marker below only asks whether a menu is present, which a leftover
				// one satisfies - the call then returns without opening anything and the
				// caller reads the state from before its own click. Waiting for the old
				// menu to disappear on its own is not enough either: a previous step can
				// legitimately leave one open with nothing about to close it, and the wait
				// then burns every attempt without ever right clicking. So dismiss it.
				dismiss(page, marker);

				target.first().waitFor(new Locator.WaitForOptions().setTimeout(20 * 1000));

				Locator element = target.first();

				page.evaluate(
						"arguments[0].scrollIntoView({block:'center', inline:'center'});", element);

				element.click(new Locator.ClickOptions().setButton(
						com.microsoft.playwright.options.MouseButton.RIGHT));

				marker.first().waitFor(new Locator.WaitForOptions().setTimeout(5 * 1000));
				return;

			} catch (Exception e) {

				if (attempt == 3) {
					throw new AssertionError(
							"The context menu for " + target + " did not open after three attempts."
									+ " Expected to find " + marker, e);
				}
				System.out.println("The context menu did not open on attempt " + attempt + ", trying again");
			}
		}
	}

	/**
	 * Closes whatever menu is on screen, so the next one opened is known to be fresh.
	 *
	 * Escape handles it in almost every case. The click on the body is there for the times
	 * it does not - a floating menu that has taken focus stays put until something outside
	 * it is clicked. Neither is treated as required: if the menu is still there afterwards
	 * the caller's own context click usually replaces it, and failing loudly here would
	 * turn a recoverable situation into a failed scenario.
	 */
	private static void dismiss(Page page, Locator marker) {

		if (Wait.all(marker).isEmpty()) {
			return;
		}


		page.keyboard().press("Escape");

		try {
			marker.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
			return;

		} catch (com.microsoft.playwright.TimeoutError ignored) {
			// fall through to the click below
		}

		page.evaluate(
				"document.body.dispatchEvent(new MouseEvent('mousedown', {bubbles:true}));"
						+ "document.body.click();");

		try {
			marker.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

		} catch (com.microsoft.playwright.TimeoutError ignored) {
			System.out.println("A previous menu is still on screen; opening over the top of it");
		}
	}
}
