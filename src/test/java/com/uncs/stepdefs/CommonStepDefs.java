package com.uncs.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.uncs.pages.HomePage;
import com.uncs.pages.LoginPage;
import com.uncs.utility.BaseClass;
import com.uncs.utility.ExcelDataProvider;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Signing in, moving between modules, and signing out.
 *
 * These steps are shared by every feature file, which is why they live in their own class
 * rather than in whichever one happened to need them first. Cucumber matches a step by
 * its text across all glue classes, so a step may only be defined once anywhere in the
 * package - defining the same sentence twice fails the whole run before a browser opens.
 *
 * The page objects are built lazily rather than in a constructor because Cucumber makes a
 * fresh instance of this class per scenario while the page is opened by BaseClass's
 * Before hook, and a field initialised too early would capture a page that does not
 * exist yet.
 */
public class CommonStepDefs {

	LoginPage loginPage;
	HomePage homePage;
	ExcelDataProvider excel;

	private LoginPage login() {
		if (loginPage == null) {
			loginPage = new LoginPage(BaseClass.driver);
		}
		return loginPage;
	}

	private HomePage home() {
		if (homePage == null) {
			homePage = new HomePage(BaseClass.driver);
		}
		return homePage;
	}

	@Given("I am on the UNCS login page")
	public void iAmOnTheLoginPage() {

		if (BaseClass.logger == null) {
			BaseClass.logger = BaseClass.report.createTest("UNCS");
		}

		assertTrue(BaseClass.driver.url().contains("/auth/login"),
				"The browser did not open on the login page: " + BaseClass.driver.url());

		BaseClass.logger.pass("Landed on the UNCS login page");

	}

	@Then("the login page should show all of its controls")
	public void theLoginPageShouldShowItsControls() {

		assertTrue(login().isLoginPageDisplayed(),
				"The login page is missing its title, the forgot password link or the create account link");

		BaseClass.logger.pass("The login page shows its title and both links");

	}

	@Then("the Login button should be disabled")
	public void theLoginButtonShouldBeDisabled() {

		assertFalse(login().isLoginButtonEnabled(),
				"Login is enabled on an empty form - it should stay greyed out until both fields are filled");

		BaseClass.logger.pass("Login is disabled while the form is empty");

	}

	@When("I log into UNCS with the email credentials in {string} of {string} of {string}")
	public void iLogIn(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String email = excel.getStringData(sheetName, row, 1);
		String password = BaseClass.config.resolvePassword(excel.getStringData(sheetName, row, 2));

		assertNotNull(password, "No password available. Either put one in " + fileName
				+ " or set the UNCS_PASSWORD environment variable.");

		login().loginToUNCS(email, password);

		BaseClass.logger.pass("Signed in as " + email);

	}

	/**
	 * A sign in that is expected to be refused.
	 *
	 * Deliberately separate from the step above: that one waits for the browser to leave
	 * the login page, which is precisely what must not happen here. It would burn its
	 * whole timeout on a navigation that never comes, and the error toast - which lives
	 * about four seconds - would be long gone by the time anything looked for it.
	 */
	@When("I attempt to log into UNCS with the credentials in {string} of {string} of {string}")
	public void iAttemptToLogIn(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String email = excel.getStringData(sheetName, row, 1);
		String password = excel.getStringData(sheetName, row, 2);

		login().attemptLogin(email, password);

		BaseClass.logger.info("Attempted a sign in as " + email + " that is expected to be refused");

	}

	@Then("the sign in should be refused with the message in {string} of {string} of {string}")
	public void theSignInShouldBeRefused(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String expected = excel.getStringData(sheetName, row, 3);

		String actual = login().getErrorMessage();

		assertTrue(actual.contains(expected),
				"Expected the error to contain \"" + expected + "\" but the toast said \"" + actual + "\"");

		assertTrue(BaseClass.driver.url().contains("/auth/login"),
				"The sign in was refused but the browser left the login page anyway");

		BaseClass.logger.pass("The sign in was refused with: " + actual);

		// The lockout scenario submits three times in a row. Without waiting for this
		// toast to clear, the next attempt would read the one still on screen and report
		// a pass for a message the product never showed that time.
		login().waitForErrorToClear();

	}

	@Then("I should land on the UNCS home page")
	public void iShouldLandOnHome() {

		assertTrue(home().isOnHomePage(), "Still on an auth page - the sign in did not go through");

		BaseClass.logger.pass("Landed on the UNCS home page");

	}

	@Then("I should see the chat list")
	public void iShouldSeeTheChatList() {

		assertTrue(home().isChatListDisplayed(), "The chat list panel never appeared");

		BaseClass.logger.pass("The chat list is displayed");

	}

	@Then("the navigation rail should offer every module")
	public void theNavigationRailShouldOfferEveryModule() {

		assertTrue(home().isNavigationDisplayed(),
				"The navigation rail is missing one of Chats, Contacts, Merchants, Settings or Logout");

		BaseClass.logger.pass("The navigation rail offers every module");

	}

	@When("I switch the interface language to {string}")
	public void iSwitchTheLanguage(String language) {

		login().selectLanguage(language);

		BaseClass.logger.pass("Switched the interface language to " + language);

	}

	@Then("the login page heading should read {string}")
	public void theHeadingShouldRead(String expected) {

		assertEquals(login().getHeadingText(expected), expected,
				"The heading did not follow the language change");

		BaseClass.logger.pass("The heading reads " + expected);

	}

	@When("I navigate to Chats")
	public void iNavigateToChats() {

		home().goToChats();

		BaseClass.logger.pass("Opened the Chats module");

	}

	@Then("I should be able to successfully log out")
	public void iShouldLogOut() {

		home().logout();

		assertTrue(home().isLoggedOut(), "The session did not return to the login page");

		BaseClass.logger.pass("Signed out and back on the login page");

	}
}
