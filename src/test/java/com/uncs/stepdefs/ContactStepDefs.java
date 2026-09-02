package com.uncs.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.uncs.pages.ContactsPage;
import com.uncs.pages.HomePage;
import com.uncs.utility.BaseClass;
import com.uncs.utility.ExcelDataProvider;
import com.uncs.utility.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.testng.Assert.assertTrue;

/**
 * The Contacts module: listing, searching and starting a conversation from a contact.
 *
 * Nothing here writes anything to the account, so these scenarios are safe to run as
 * often as you like.
 */
public class ContactStepDefs {

	ContactsPage contactsPage;
	HomePage homePage;
	ExcelDataProvider excel;

	private ContactsPage contacts() {
		if (contactsPage == null) {
			contactsPage = new ContactsPage(BaseClass.driver);
		}
		return contactsPage;
	}

	private HomePage home() {
		if (homePage == null) {
			homePage = new HomePage(BaseClass.driver);
		}
		return homePage;
	}

	@When("I navigate to Contacts")
	public void iNavigateToContacts() {

		home().goToContacts();

		BaseClass.logger.pass("Opened the Contacts module");

	}

	@Then("the contacts list should be displayed")
	public void theContactsListShouldBeDisplayed() {

		assertTrue(contacts().isContactsOpen(), "The contacts list never appeared");

		BaseClass.logger.pass("The contacts list is displayed");

	}

	@When("I search for the contact in {string} of {string} of {string}")
	public void iSearchForTheContact(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String searchTerm = excel.getStringData(sheetName, row, 1);
		ScenarioContext.searchedContact = excel.getStringData(sheetName, row, 2);

		contacts().searchContact(searchTerm);

		BaseClass.logger.pass("Searched the contacts for " + searchTerm);

	}

	@Then("the searched contact should be listed")
	public void theSearchedContactShouldBeListed() {

		assertTrue(contacts().isContactListed(ScenarioContext.searchedContact),
				"The search did not list the contact " + ScenarioContext.searchedContact);

		BaseClass.logger.pass(ScenarioContext.searchedContact + " is listed in the search results");

	}

	@When("I open the searched contact")
	public void iOpenTheSearchedContact() {

		contacts().openContact(ScenarioContext.searchedContact);

		BaseClass.logger.pass("Opened the conversation with " + ScenarioContext.searchedContact);

	}
}
