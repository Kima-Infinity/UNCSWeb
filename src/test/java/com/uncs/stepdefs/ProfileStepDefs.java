package com.uncs.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.uncs.pages.HomePage;
import com.uncs.pages.ProfilePage;
import com.uncs.pages.SettingsPage;
import com.uncs.utility.BaseClass;
import com.uncs.utility.ExcelDataProvider;
import com.uncs.utility.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The Settings drawer: the profile at the top of it, and the Block List, Devices and
 * Language panels reached from it.
 *
 * Everything here reads rather than writes. The one control that would change anything
 * outside the browser - Terminate All Other Sessions, which signs out every other device
 * on the account - is asserted to be present and never pressed.
 */
public class ProfileStepDefs {

	ProfilePage profilePage;
	SettingsPage settingsPage;
	HomePage homePage;
	ExcelDataProvider excel;

	private HomePage home() {
		if (homePage == null) {
			homePage = new HomePage(BaseClass.driver);
		}
		return homePage;
	}

	private SettingsPage settings() {
		if (settingsPage == null) {
			settingsPage = new SettingsPage(BaseClass.driver);
		}
		return settingsPage;
	}

	@When("I open my profile from Settings")
	public void iOpenMyProfile() {

		home().goToSettings();

		profilePage = new ProfilePage(BaseClass.driver);

		assertTrue(profilePage.isProfileOpen(), "The Settings panel did not open");

		BaseClass.logger.pass("Opened the profile from Settings");

	}

	@Then("the profile should show the details in {string} of {string} of {string}")
	public void theProfileShouldShow(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String expectedName = excel.getStringData(sheetName, row, 1);
		String expectedEmail = excel.getStringData(sheetName, row, 2);

		// The name is rendered through a capitalise rule, so the browser can report a
		// different case from the one stored against the account.
		assertEquals(profilePage.getDisplayName().toLowerCase(), expectedName.toLowerCase(),
				"The profile shows a different display name");

		assertEquals(profilePage.getEmail(), expectedEmail,
				"The profile shows a different email address");

		BaseClass.logger.pass("The profile shows " + expectedName + " and " + expectedEmail);

	}

	@Then("the profile should offer the privacy toggles")
	public void theProfileShouldOfferPrivacyToggles() {

		for (String toggle : new String[]{"Show Online Status", "Show Last Seen", "Allow Notification"}) {
			assertTrue(settings().hasPrivacyToggle(toggle),
					"The profile is missing the " + toggle + " toggle");
		}

		BaseClass.logger.pass("The profile offers all three privacy toggles");

	}

	@When("I open the {string} settings panel")
	public void iOpenTheSettingsPanel(String panel) {

		settings().openItem(panel);

		BaseClass.logger.pass("Opened the " + panel + " panel");

	}

	@Then("the blocked list should be empty")
	public void theBlockedListShouldBeEmpty() {

		assertTrue(settings().isPanelShowing("Blocked List"), "The Blocked List panel did not open");

		assertTrue(settings().isBlockListEmpty(),
				"The blocked list is not empty. It showed: " + settings().getPanelText());

		BaseClass.logger.pass("The blocked list is empty");

	}

	@Then("the devices panel should list this device and offer to terminate the others")
	public void theDevicesPanelShouldListThisDevice() {

		assertTrue(settings().isDeviceListShowing(),
				"The devices panel is missing 'This Device' or the terminate action. It showed: "
						+ settings().getPanelText());

		BaseClass.logger.pass("The devices panel lists this device and offers to terminate the others");

	}

	@Then("the language panel should show the language in {string} of {string} of {string}")
	public void theLanguagePanelShouldShow(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String expected = excel.getStringData(sheetName, row, 3);

		assertTrue(settings().isPanelShowing("Change Language"), "The Change Language panel did not open");

		assertEquals(settings().getSelectedLanguage(), expected,
				"The account is set to a different language");

		BaseClass.logger.pass("The account language is " + expected);

	}

	/**
	 * Clears the block list before a scenario that blocks somebody.
	 *
	 * Blocking is the one action on the chat menu that stops other scenarios working: a
	 * blocked contact cannot be messaged. A run that dies between the block and the
	 * unblock leaves a real account blocked, so the next run repairs it rather than
	 * inheriting it.
	 */
	@Given("nobody is blocked")
	public void nobodyIsBlocked() {

		home().goToSettings();

		settings().openItem("Block List");

		settings().isPanelShowing("Blocked List");

		settings().ensureNobodyBlocked();

		BaseClass.logger.pass("The block list is empty to begin with");

	}

	@Then("the blocked list should contain that chat")
	public void theBlockedListShouldContainThatChat() {

		home().goToSettings();

		settings().openItem("Block List");

		settings().isPanelShowing("Blocked List");

		assertTrue(settings().isUserBlocked(ScenarioContext.blockedChat),
				"The blocked list does not contain " + ScenarioContext.blockedChat
						+ ". It showed: " + settings().getPanelText());

		BaseClass.logger.pass(ScenarioContext.blockedChat + " is on the blocked list");

	}

	@When("I unblock that chat from the blocked list")
	public void iUnblockThatChat() {

		settings().unblockUser(ScenarioContext.blockedChat);

		BaseClass.logger.pass("Unblocked " + ScenarioContext.blockedChat);

	}

	@Then("the blocked list should be empty again")
	public void theBlockedListShouldBeEmptyAgain() {

		assertTrue(settings().isBlockListEmpty(),
				"The blocked list is not empty. It showed: " + settings().getPanelText());

		BaseClass.logger.pass("The blocked list is empty again");

	}

	@When("I go back from the settings panel")
	public void iGoBackFromTheSettingsPanel() {

		settings().goBack();

		BaseClass.logger.pass("Went back to the profile panel");

	}
}
