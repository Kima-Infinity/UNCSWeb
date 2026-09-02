package com.uncs.pages;

import com.uncs.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;
import java.util.List;

/**
 * The panel Settings opens on the right, and the sub panels reached from it.
 *
 * Everything here lives in the same drawer: choosing Block List, Language or Devices
 * replaces the profile inside it rather than moving to a new address or stacking a second
 * panel on top, so nothing in the URL says which one is open and there is only ever one
 * drawer in the DOM. Each panel is identified by its own heading, and the chevron in the
 * top left is what goes back.
 *
 * A panel's heading renders before its contents. Every read below therefore waits for the
 * text it is looking for rather than taking one snapshot - the first version of this class
 * asked for the device list the instant the panel opened and got back the word "Devices"
 * and nothing else.
 *
 * WHAT IS DELIBERATELY NOT HERE
 *
 * Terminate All Other Sessions signs out every other device on the account, including
 * whatever the person running the suite has open. It is read but never pressed.
 */
public class SettingsPage {

	Page page;
	
	public SettingsPage(Page ldriver) {

		this.page = ldriver;
	}

	/** The drawer, which is whatever the last click put in it. */
	private Locator panel() {

		List<Locator> drawers = page.locator("xpath=//aside[contains(@class,'fixed')]").all();

		if (drawers.isEmpty()) {
			throw new IllegalStateException("The settings drawer is not open");
		}
		return drawers.get(drawers.size() - 1);

	}

	public void openItem(String label) {

		Locator item = page.locator("xpath=" + "//button[.//span[normalize-space()='" + label + "']]");

		item.click();

	}

	/**
	 * Goes back one panel.
	 *
	 * The chevron is the drawer's first button and carries no label of any kind, so its
	 * position is the only handle. It is stable because the header always renders the
	 * back control before anything else.
	 */
	public void goBack() {

		panel().locator("button").first().click();

	}

	public String getPanelText() {

		return panel().innerText().trim();

	}

	/** Waits for a panel to carry the text it is known by, rather than reading once. */
	public boolean isPanelShowing(String heading) {

		return Wait.until(() -> panel().innerText().contains(heading), 15);

	}

	/**
	 * The blocked list, which starts out empty on a fresh account.
	 *
	 * Asserting on the empty state rather than on a count keeps the check honest: a list
	 * that failed to load also has no rows in it.
	 */
	public boolean isBlockListEmpty() {

		return Wait.until(() -> panel().innerText().contains("No blocked contact yet"), 15);

	}

	public boolean isDeviceListShowing() {

		return Wait.until(() -> {
			String text = panel().innerText();
			return text.contains("This Device") && text.contains("Terminate All Other Sessions");
		}, 15);

	}

	/** The language the account is set to, read off the Change Language panel. */
	public String getSelectedLanguage() {

		Locator selected = page.locator("xpath=//aside[contains(@class,'fixed')]"
				+ "//button[contains(@class,'rounded-xl')]").first();

		selected.waitFor();

		return selected.innerText().trim();

	}

	/**
	 * A privacy switch, found by the name it announces to assistive technology.
	 *
	 * These are divs with a switch role rather than buttons, which is what an earlier
	 * version of this locator assumed and why it never found any of them. The aria-label
	 * carries the setting's name and is the only stable handle on the control itself.
	 */
	private Locator privacySwitch(String label) {

		return page.locator("xpath=" + "//*[@role='switch'][@aria-label='" + label + "']");

	}

	public boolean isUserBlocked(String name) {

		return getPanelText().contains(name);

	}

	/**
	 * Unblocks whoever is listed, and waits for the list to empty.
	 *
	 * Unblock takes effect immediately, with no confirmation, exactly like the Block that
	 * put them there.
	 */
	public void unblockUser(String name) {

		page.locator("xpath=" + "//button[normalize-space()='Unblock']").first().click();

		Wait.untilOrFail(() -> panel().innerText().contains("No blocked contact yet"), 15, "the page never settled");

	}

	/**
	 * Clears the block list before a scenario that blocks somebody.
	 *
	 * A run that dies between the block and the unblock leaves a real account blocked, and
	 * blocking is the one action on that menu that stops other scenarios working at all -
	 * a blocked contact cannot be messaged. Normalising first means the damage from a
	 * failed run is repaired by the next one rather than compounding.
	 */
	public void ensureNobodyBlocked() {

		if (getPanelText().contains("No blocked contact yet")) {
			return;
		}

		System.out.println("The block list was not empty; clearing it before the scenario runs");

		while (!page.locator("xpath=//button[normalize-space()='Unblock']").all().isEmpty()) {
			unblockUser("");
		}
	}

	public boolean hasPrivacyToggle(String label) {

		try {
			privacySwitch(label).first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			return true;

		} catch (com.microsoft.playwright.TimeoutError e) {
			return false;
		}
	}

	public boolean isPrivacyToggleOn(String label) {

		privacySwitch(label).first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

				Locator toggle = privacySwitch(label).first();

		return "true".equals(toggle.getAttribute("aria-checked"));

	}
}
