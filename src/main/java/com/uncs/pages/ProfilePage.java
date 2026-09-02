package com.uncs.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The panel Settings opens on the right: the signed in account's name, id and contact
 * details, followed by the privacy toggles.
 *
 * The details are laid out as a label beside its value, so each value is reached through
 * the label next to it. Reading the value's own element instead would mean depending on
 * a utility class shared with half the panel.
 */
public class ProfilePage {

	Page page;
	
		private final Locator displayName;


		private final Locator email;


		private final Locator phone;


	public ProfilePage(Page ldriver) {

		this.page = ldriver;
		this.displayName = page.locator("xpath=//span[contains(@class,'text-xl') and contains(@class,'font-semibold')]");
		this.email = page.locator("xpath=//span[normalize-space()='Email']/following-sibling::span[1]");
		this.phone = page.locator("xpath=//span[normalize-space()='Phone']/following-sibling::span[1]");
	}

	public boolean isProfileOpen() {

		return displayName.isVisible();

	}

	public String getDisplayName() {

		return displayName.innerText().trim();

	}

	public String getEmail() {

		return email.innerText().trim();

	}

	public String getPhone() {

		return phone.innerText().trim();

	}
}
