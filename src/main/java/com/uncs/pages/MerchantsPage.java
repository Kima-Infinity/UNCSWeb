package com.uncs.pages;

import com.uncs.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;
import java.util.List;

/**
 * The Merchants directory, its filters, and a merchant's own page.
 *
 * Merchant cards and category options are both list items, which is why the option
 * locator matches on the whole item rather than a descendant: an option holds nothing
 * but the category name, while a card also holds a badge, a name and a description, so
 * only the option can have a normalised text equal to the category on its own.
 */
public class MerchantsPage {

	Page page;
	
		private final Locator directoryHeading;


	/**
	 * The closed filter. Its label becomes the chosen category once a filter is applied,
	 * so this only finds the control while it is still unfiltered - which is the only
	 * time a scenario needs to open it.
	 */
		private final Locator categoryDropdown;


	/**
	 * The directory reuses the Contacts search box rather than having one of its own,
	 * which is why the id says contact on a merchant screen.
	 */
		private final Locator searchBox;


	public MerchantsPage(Page ldriver) {

		this.page = ldriver;
		this.directoryHeading = page.locator("xpath=//span[normalize-space()='All Merchants']");
		this.categoryDropdown = page.locator("xpath=(//span[normalize-space()='All Categories']/parent::div)[1]");
		this.searchBox = page.locator("#search-contact");
		this.MERCHANT_CARD = page.locator("xpath=" + "//h3/ancestor::li[1]");
	}

	public boolean isDirectoryOpen() {

		return directoryHeading.isVisible();

	}

	public void filterByCategory(String category) {

		categoryDropdown.click();

		Locator option = page.locator("xpath=" + "//li[normalize-space()='" + category + "']");

		option.click();

		System.out.println("Filtered the merchant directory by " + category);

	}

	public void searchMerchant(String term) {

		searchBox.waitFor();

		Locator search = searchBox;
		search.fill("");
		search.fill(term);

	}

	private final Locator MERCHANT_CARD;
	/**
	 * Every card on screen, once at least one has rendered.
	 *
	 * Filtering and searching are both round trips. Reading the cards the instant a filter
	 * is applied answers about the list as it was before, which is how an empty result and
	 * a not-yet-loaded result became indistinguishable.
	 */
	public List<Locator> getMerchantCards() {

		waitForCards();

		return Wait.all(MERCHANT_CARD);

	}

	/**
	 * Waits for the directory to hold at least one card.
	 *
	 * Returning quietly on timeout is deliberate: a genuinely empty result is a real
	 * outcome that the callers judge for themselves, and they say more about it than a
	 * com.microsoft.playwright.TimeoutError thrown from here would.
	 */
	private void waitForCards() {

		try {
			MERCHANT_CARD.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

		} catch (com.microsoft.playwright.TimeoutError e) {
			System.out.println("No merchant cards rendered within the timeout");
		}
	}

	/** Waits for a named merchant to appear, which is what a search is judged by. */
	public boolean waitForMerchant(String merchantName) {

		try {
			page.locator("xpath=" + "//h3[normalize-space()='" + merchantName + "']").first().waitFor();
			return true;

		} catch (com.microsoft.playwright.TimeoutError e) {
			return false;
		}
	}

	/**
	 * The name of each merchant currently listed.
	 *
	 * A card holds two h3 elements - the merchant's name and then its description - and
	 * they share the same utility classes, so matching on the class returned the blurb as
	 * a second "name" and made a one-result search look like two. The name is always the
	 * first h3 inside its card, which is what this takes.
	 */
	public List<String> getMerchantNames() {

		return getMerchantCards().stream()
				.map(card -> card.locator("h3").all())
				.filter(headings -> !headings.isEmpty())
				.map(headings -> headings.get(0).innerText().trim())
				.filter(name -> !name.isEmpty())
				.toList();

	}

	/**
	 * True when the filter did something and everything left belongs to the category.
	 *
	 * An empty directory counts as a failure rather than a pass. A filter that returns
	 * nothing would otherwise satisfy "no card from another category" and hide a broken
	 * filter behind a green test.
	 */
	public boolean areAllMerchantsInCategory(String category) {

		List<Locator> cards = getMerchantCards();

		if (cards.isEmpty()) {
			System.out.println("No merchant cards are listed under " + category);
			return false;
		}

		for (Locator card : cards) {

			if (card.locator("xpath=" + ".//span[normalize-space()='" + category + "']").all().isEmpty()) {
				System.out.println("A card outside " + category + " is still listed: " + card.innerText());
				return false;
			}
		}

		System.out.println(cards.size() + " merchant cards listed, all tagged " + category);
		return true;

	}

	/** Opening a card moves to /merchants/{id}, the merchant's own page. */
	public void openMerchant(String merchantName) {

		Locator card = page.locator("xpath=" + "//h3[normalize-space()='" + merchantName + "']/ancestor::li[1]");

		card.click();

		Wait.until(() -> page.url().contains("/merchants/"), 15);

		System.out.println("Opened the merchant page for " + merchantName + " at " + page.url());

	}

	public boolean isMerchantPageOpen() {

		return page.url().contains("/merchants/");

	}

	/**
	 * The visible copy of an action button on a merchant page.
	 *
	 * The page lays the actions out twice, once for narrow screens and once for wide, and
	 * both copies are in the DOM at all times with only one of them displayed. Taking the
	 * first match would hit the hidden one about half the time, so the displayed one has
	 * to be picked out by hand.
	 */
	private Locator visibleButton(String label) {

		Locator locator = page.locator("xpath=" + "//button[normalize-space()='" + label + "']");

		locator.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

		return Wait.all(locator).stream()
				.filter(Locator::isVisible)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"No visible '" + label + "' button on the merchant page"));

	}

	/**
	 * Whether the page currently offers Follow or Following, waiting for one to appear.
	 *
	 * A merchant page renders its actions after its header, so the first read on arrival
	 * finds neither, and an empty answer then means not loaded rather than not following.
	 */
	public String getFollowButtonLabel() {

		try {
			Wait.untilOrFail(() -> !readFollowLabel().isEmpty(), 15, "the page never settled");

		} catch (com.microsoft.playwright.TimeoutError e) {
			return "";
		}

		return readFollowLabel();

	}

	/** One look at the page with no waiting - the building block the waits above poll. */
	private String readFollowLabel() {

		for (String label : List.of("Follow", "Following")) {
			List<Locator> found = page.locator("xpath=//button[normalize-space()='" + label + "']").all();
			if (found.stream().anyMatch(Locator::isVisible)) {
				return label;
			}
		}
		return "";

	}

	public void followMerchant() {

		visibleButton("Follow").click();

		Wait.untilOrFail(() -> "Following".equals(readFollowLabel()), 15, "the page never settled");

	}

	/**
	 * Unfollows and answers the confirmation.
	 *
	 * Pressing Following does not unfollow on its own - it raises a panel whose own
	 * Unfollow button is what actually does it.
	 */
	public void unfollowMerchant() {

		visibleButton("Following").click();

		Locator confirm = page.locator("xpath=" + "//button[normalize-space()='Unfollow']");

		confirm.click();

		Wait.untilOrFail(() -> "Follow".equals(readFollowLabel()), 15, "the page never settled");

	}

	public boolean hasChatAction() {

		try {
			page.locator("xpath=" + "//button[normalize-space()='Chat']").first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			return true;

		} catch (com.microsoft.playwright.TimeoutError e) {
			return false;
		}
	}
}
