package com.uncs.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.uncs.pages.HomePage;
import com.uncs.pages.MerchantsPage;
import com.uncs.utility.BaseClass;
import com.uncs.utility.ExcelDataProvider;
import com.uncs.utility.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The Merchants directory, its category filter and search, and a merchant's own page.
 *
 * Following a merchant is the one write in here, and the scenario that does it unfollows
 * again in the same step so the account is left as it was found.
 */
public class MerchantStepDefs {

	MerchantsPage merchantsPage;
	HomePage homePage;
	ExcelDataProvider excel;

	private MerchantsPage merchants() {
		if (merchantsPage == null) {
			merchantsPage = new MerchantsPage(BaseClass.driver);
		}
		return merchantsPage;
	}

	private HomePage home() {
		if (homePage == null) {
			homePage = new HomePage(BaseClass.driver);
		}
		return homePage;
	}

	@When("I navigate to Merchants")
	public void iNavigateToMerchants() {

		home().goToMerchants();

		BaseClass.logger.pass("Opened the Merchants module");

	}

	@Then("the merchant directory should be displayed")
	public void theMerchantDirectoryShouldBeDisplayed() {

		assertTrue(merchants().isDirectoryOpen(), "The merchant directory never appeared");

		BaseClass.logger.pass("The merchant directory is displayed");

	}

	@When("I filter the merchants by the category in {string} of {string} of {string}")
	public void iFilterTheMerchants(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		ScenarioContext.filteredCategory = excel.getStringData(sheetName, row, 1);

		merchants().filterByCategory(ScenarioContext.filteredCategory);

		BaseClass.logger.pass("Filtered the merchants by " + ScenarioContext.filteredCategory);

	}

	@Then("every merchant listed should belong to that category")
	public void everyMerchantShouldBelongToTheCategory() {

		assertTrue(merchants().areAllMerchantsInCategory(ScenarioContext.filteredCategory),
				"The directory is either empty or still listing merchants outside "
						+ ScenarioContext.filteredCategory);

		BaseClass.logger.pass("Every merchant listed is tagged " + ScenarioContext.filteredCategory);

	}

	@When("I search the merchants for the term in {string} of {string} of {string}")
	public void iSearchTheMerchants(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String term = excel.getStringData(sheetName, row, 2);
		ScenarioContext.openedMerchant = excel.getStringData(sheetName, row, 3);

		merchants().searchMerchant(term);

		BaseClass.logger.pass("Searched the merchants for " + term);

	}

	@Then("only merchants matching the search should be listed")
	public void onlyMatchingMerchantsShouldBeListed() {

		// Wait for the searched merchant before counting, or an unfinished search reads as
		// an empty result set.
		merchants().waitForMerchant(ScenarioContext.openedMerchant);

		List<String> names = merchants().getMerchantNames();

		assertTrue(names.contains(ScenarioContext.openedMerchant),
				"The search did not return " + ScenarioContext.openedMerchant + ". It returned: " + names);

		assertEquals(names.size(), 1,
				"The search returned more than the expected merchant: " + names);

		BaseClass.logger.pass("The search returned exactly " + ScenarioContext.openedMerchant);

	}

	@When("I open that merchant")
	public void iOpenThatMerchant() {

		merchants().openMerchant(ScenarioContext.openedMerchant);

		BaseClass.logger.pass("Opened the merchant page for " + ScenarioContext.openedMerchant);

	}

	@Then("the merchant page should offer Follow and Chat")
	public void theMerchantPageShouldOfferItsActions() {

		assertTrue(merchants().isMerchantPageOpen(),
				"The browser is not on a merchant page: " + BaseClass.driver.url());

		assertEquals(merchants().getFollowButtonLabel(), "Follow",
				"The merchant page did not open in the not-following state");

		assertTrue(merchants().hasChatAction(), "The merchant page has no Chat action");

		BaseClass.logger.pass("The merchant page offers Follow and Chat");

	}

	/**
	 * Follows and then unfollows, in one step on purpose.
	 *
	 * Leaving the account following a merchant would change what the next run sees, so
	 * the restore belongs in the same step as the change rather than in a separate one a
	 * failure could skip past.
	 */
	@When("I follow and unfollow that merchant")
	public void iFollowAndUnfollowThatMerchant() {

		merchants().followMerchant();

		assertEquals(merchants().getFollowButtonLabel(), "Following",
				"Following the merchant did not flip the button to Following");

		merchants().unfollowMerchant();

		BaseClass.logger.pass("Followed the merchant, saw the button flip, then unfollowed again");

	}

	@Then("the merchant should be back to not followed")
	public void theMerchantShouldBeBackToNotFollowed() {

		assertEquals(merchants().getFollowButtonLabel(), "Follow",
				"The account is still following the merchant");

		BaseClass.logger.pass("The merchant is back to not followed");

	}
}
