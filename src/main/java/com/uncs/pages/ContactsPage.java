package com.uncs.pages;

import com.uncs.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The Contacts module.
 *
 * The module replaces the chat list in the same panel rather than moving to its own
 * address, so nothing about the URL says Contacts is open. The search box is what tells
 * the two apart: the chat list's is search-group-header and this one is search-contact.
 */
public class ContactsPage {

	Page page;
	
		private final Locator searchBox;


	public ContactsPage(Page ldriver) {

		this.page = ldriver;
		this.searchBox = page.locator("#search-contact");
	}

	public boolean isContactsOpen() {

		return searchBox.isVisible();

	}

	public void searchContact(String term) {

		searchBox.waitFor();

		Locator search = searchBox;
		search.fill("");
		search.fill(term);

	}

	public boolean isContactListed(String contactName) {

		Locator contactRow = page.locator("xpath=" + "//li[.//span[normalize-space()='" + contactName + "']]");

		return Wait.appears(contactRow.first());

	}

	/** Opening a contact starts or resumes the conversation with them. */
	public void openContact(String contactName) {

		Locator contactRow = page.locator("xpath=" + "//li[.//span[normalize-space()='" + contactName + "']]");

		contactRow.click();

		Wait.until(() -> page.url().contains("/c/"), 15);

		System.out.println("Opened a chat with the contact " + contactName + " at " + page.url());

	}
}
