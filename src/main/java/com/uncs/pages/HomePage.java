package com.uncs.pages;

import com.uncs.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;
import java.util.List;

/**
 * The shell the whole application lives in: the left navigation rail and the chat list
 * beside it. Every module is reached from here, so the other page objects assume a
 * navigation click from this class has already happened.
 *
 * The markup carries no ids or test attributes outside the two search boxes, so the rail
 * is addressed through the label each button wraps. That label is the one part of a nav
 * button that is not a generated utility class, which makes it the most stable handle
 * available.
 */
public class HomePage {

	Page page;
	
		private final Locator chatsButton;


		private final Locator contactsButton;


		private final Locator merchantsButton;


		private final Locator settingsButton;


	/**
	 * The rail's Logout, not the one at the foot of the Settings panel. Only the rail
	 * button wraps its label in a span, which is what keeps this from matching both.
	 */
		private final Locator logoutButton;


	/**
	 * The confirmation the rail's Logout raises. The label is stored lowercase and
	 * shown capitalised by a stylesheet, so the XPath has to ask for 'yes'.
	 */
		private final Locator logoutConfirmButton;


		private final Locator chatSearch;


	public HomePage(Page ldriver) {

		this.page = ldriver;
		this.chatsButton = page.locator("xpath=//li/button[.//span[normalize-space()='Chats']]");
		this.contactsButton = page.locator("xpath=//li/button[.//span[normalize-space()='Contacts']]");
		this.merchantsButton = page.locator("xpath=//li/button[.//span[normalize-space()='Merchants']]");
		this.settingsButton = page.locator("xpath=//li/button[.//span[normalize-space()='Settings']]");
		this.logoutButton = page.locator("xpath=//li/button[.//span[normalize-space()='Logout']]");
		this.logoutConfirmButton = page.locator("xpath=//button[normalize-space()='yes']");
		this.chatSearch = page.locator("#search-group-header");
		this.REPORT_HEADING = page.locator("xpath=" + "//*[normalize-space()='Report User']");
		this.REPORT_ATTACHMENT = page.locator("#input-attachment");
		this.REPORT_SUBMIT = page.locator("xpath=" + "//button[normalize-space()='Submit Report']");
		this.REPORT_SUBMITTED = page.locator("xpath=" + "//p[normalize-space()='Report Submitted']");
		this.REPORT_DESCRIPTION = page.locator("xpath=" + "//textarea[@placeholder='Describe the reasons of reporting this person']");
		this.REPORT_REATTACH = page.locator("xpath=" + "//*[normalize-space()='Re-Upload Attachment']");
		this.DELETE_PROMPT = page.locator("xpath=" + "//*[normalize-space()='Are you sure you want to delete this chat?']");
		this.CHAT_MENU_MARKER = page.locator("xpath=" + "//ul//button[.//span[normalize-space()='Report']]");
	}

	/** True once the session has left the auth pages, which is where a signed in user lands. */
	public boolean isOnHomePage() {

		// Asked of the navigation rail rather than the address, because this application
		// changes its screen well before - and sometimes without ever - changing its URL.
		// Waiting on the address alone reported "still on an auth page" for a sign in that
		// had visibly worked, and it did so for most of the suite: every scenario signs in,
		// so every scenario failed here while the server answered every call with a 200.
		//
		// The navigation rail only exists once a session does, which makes it the honest
		// question. The URL is still accepted when it does change, since that is the
		// clearer signal on the occasions it arrives.
		return Wait.until(() -> !page.url().contains("/auth/") || chatsButton.count() > 0, 15);

	}

	/** The chat list panel is the proof the shell finished loading, not just the URL changing. */
	public boolean isChatListDisplayed() {

		return chatSearch.isVisible();

	}

	public boolean isNavigationDisplayed() {

		return chatsButton.isVisible() && contactsButton.isVisible()
				&& merchantsButton.isVisible() && settingsButton.isVisible()
				&& logoutButton.isVisible();

	}

	public void goToChats() {

		chatsButton.click();

	}

	public void goToContacts() {

		contactsButton.click();

	}

	public void goToMerchants() {

		merchantsButton.click();

	}

	public void goToSettings() {

		settingsButton.click();

	}

	public void searchChat(String term) {

		chatSearch.waitFor();

		Locator search = chatSearch;
		search.fill("");
		search.fill(term);

	}

	/** The chat rows currently listed, which is what a search is judged by. */
	public List<Locator> getChatRows() {

		return page.locator("xpath=" + "//input[@id='search-group-header']"
				+ "/ancestor::div[2]//li[.//span[contains(@class,'font-semibold')]]").all();

	}

	/**
	 * Whether a chat is in the list, waiting for the list to catch up first.
	 *
	 * Called straight after typing in the search box, and the filtering is a round trip,
	 * so a single read answers about the list as it was before the search rather than
	 * after it.
	 */
	public boolean isChatListed(String chatName) {

		try {
			chatRow(chatName).first().waitFor();
			return true;

		} catch (com.microsoft.playwright.TimeoutError e) {
			return false;
		}
	}

	private Locator chatRow(String chatName) {

		return page.locator("xpath=" + "//li[.//span[normalize-space()='" + chatName + "']]");

	}

	/**
	 * Opens the per chat menu, which is bound to a right click on the row. There is no
	 * three dot button anywhere in the list, so a context click is the only way in.
	 */
	public void openChatMenu(String chatName) {

		ContextMenus.open(page, chatRow(chatName), CHAT_MENU_MARKER);

	}

	/**
	 * The labels the chat menu is currently offering.
	 *
	 * Star and Pin flip to Unstar and Unpin once they are on, so reading the labels back
	 * is how a test tells the new state without depending on an icon.
	 */
	public List<String> getChatMenuItems() {

		return page.locator("xpath=//ul//button/span[contains(@class,'text-sm')]").all()
				.stream().map(e -> e.innerText().trim()).toList();

	}

	/**
	 * The pin badge a pinned chat carries in its row.
	 *
	 * Whether a chat is pinned is read from the row rather than from the menu, and that
	 * choice is the whole reason this scenario became stable. Reading it from the menu
	 * means closing the old menu, opening a new one and trusting that what came back is
	 * the new one - three chances to be wrong about a question the row answers directly.
	 * Six consecutive runs failed on that, every time for a different reason, and never
	 * because the pin itself had not worked.
	 *
	 * The badge is matched on the start of its path data. There is no class, title or
	 * label to go on, but the glyph is fixed and no other icon in a row begins this way.
	 */
	/**
	 * A badge in a chat's row, matched on the start of its icon path.
	 *
	 * local-name() rather than a plain svg/path name test: SVG elements live in their own
	 * XML namespace, and an unprefixed name test in XPath only matches elements with no
	 * namespace. On this page //svg matches nothing at all while //*[local-name()='svg']
	 * matches forty six elements, so the obvious spelling can never be true.
	 *
	 * There is no class, title or label on any of these icons to go on. The path data is
	 * fixed per glyph and the three badges are unmistakably different from each other:
	 *
	 *   pin   m237.66 106.35   (viewBox 0 0 256 256)
	 *   star  M12.206 7.21     (viewBox 0 0 32 32)
	 *   mute  M13.5 4.06       (viewBox 0 0 24 24)
	 */
	private Locator rowBadge(String chatName, String pathPrefix) {

		return page.locator("xpath=" + "//li[.//span[normalize-space()='" + chatName + "']]"
				+ "//*[local-name()='svg'][starts-with(*[local-name()='path']/@d,'" + pathPrefix + "')]");

	}

	private Locator pinnedBadge(String chatName) {

		return rowBadge(chatName, PIN_PATH);

	}

	/**
	 * One look at the row, with no waiting.
	 *
	 * Only safe inside {@link #waitForPinnedState}, which polls it. On its own it reports
	 * a pinned chat as unpinned whenever it is called in the gap between the row rendering
	 * and its badge arriving.
	 */
	private boolean isChatPinned(String chatName) {

		return pinnedBadge(chatName).count() > 0;

	}

	/**
	 * Waits for a chat to reach a pinned state, which the server confirms a moment after
	 * the click. Returns false rather than throwing so the caller can say what it expected.
	 */
	public boolean waitForPinnedState(String chatName, boolean pinned, Duration timeout) {

		return waitForBadgeState(rowBadge(chatName, PIN_PATH), pinned, timeout);

	}

	public boolean waitForStarredState(String chatName, boolean starred, Duration timeout) {

		return waitForBadgeState(rowBadge(chatName, STAR_PATH), starred, timeout);

	}

	public boolean waitForMutedState(String chatName, boolean muted, Duration timeout) {

		return waitForBadgeState(rowBadge(chatName, MUTE_PATH), muted, timeout);

	}

	/**
	 * Waits for a badge to appear or to go away.
	 *
	 * Counted rather than asked about visibility: "no such element" is the state being
	 * measured half the time, and count() answers that without throwing.
	 */
	private boolean waitForBadgeState(Locator badge, boolean present, Duration timeout) {

		return Wait.until(() -> (badge.count() > 0) == present, (int) timeout.getSeconds());
	}

	/**
	 * Muting is two steps, not one.
	 *
	 * Choosing Mute opens a chooser - one day, one week, one month, or until I change it -
	 * and nothing is muted until Done is pressed. Unmute, by contrast, is immediate.
	 */
	public void muteChat(String chatName, String duration) {

		selectChatMenuItem(chatName, "Mute");

		Locator option = page.locator("xpath=" + "//li[normalize-space()='" + duration + "']");

		option.click();

		page.locator("xpath=" + "//button[normalize-space()='Done']").first().click();

	}

	public void unmuteChat(String chatName) {

		selectChatMenuItem(chatName, "Unmute");

	}

	/**
	 * Blocks the other party in a chat.
	 *
	 * This happens the instant the menu item is clicked - there is no confirmation step,
	 * unlike Delete on the same menu. Anything calling this is responsible for unblocking
	 * afterwards, and the scenario that does keeps the two together.
	 */
	public void blockUser(String chatName) {

		selectChatMenuItem(chatName, "Block User");

	}

	/** Opens the report dialog. */
	public void openReportDialog(String chatName) {

		selectChatMenuItem(chatName, "Report");

		REPORT_HEADING.first().waitFor();

	}

	public boolean isReportDialogOpen() {

		return !Wait.all(REPORT_HEADING).isEmpty();

	}

	public boolean reportDialogOffers(String reason) {

		return !page.locator("xpath=//button[normalize-space()='" + reason + "']").all().isEmpty();

	}

	/**
	 * Chooses why the user is being reported.
	 *
	 * The description box and the attachment control do not exist until a reason is
	 * picked - the dialog opens showing only the five reasons.
	 */
	public void selectReportReason(String reason) {

		page.locator("xpath=" + "//button[normalize-space()='" + reason + "']").first().click();

		REPORT_DESCRIPTION.first().waitFor();

	}

	public void enterReportDescription(String description) {

		REPORT_DESCRIPTION.first().waitFor();

		Locator box = REPORT_DESCRIPTION.first();
		box.fill("");
		box.fill(description);

	}

	/**
	 * Attaches a file to the report.
	 *
	 * The visible "Upload Attachment" text is not the control - it triggers a hidden file
	 * input, and clicking it opens the operating system's file chooser, which Page
	 * cannot drive. Sending the path straight to the input is the supported route and
	 * never opens a chooser at all, which is why the input is addressed by its id despite
	 * being invisible.
	 *
	 * The path must be absolute, so a relative one out of the test data is resolved
	 * against the working directory here rather than passed through as it stands.
	 */
	public void attachReportFile(String relativeOrAbsolutePath) {

		java.io.File file = new java.io.File(relativeOrAbsolutePath);

		if (!file.isAbsolute()) {
			file = new java.io.File(System.getProperty("user.dir"), relativeOrAbsolutePath);
		}

		if (!file.exists()) {
			throw new IllegalArgumentException("No attachment to upload at " + file.getAbsolutePath());
		}

		// setInputFiles rather than typing a path into the control: it is what Playwright
		// offers for a file input, and unlike a click it does not need the element visible -
		// this one is a hidden input behind a styled label.
		REPORT_ATTACHMENT.first().setInputFiles(file.toPath());

		// The link relabels itself once a file is held, which is the only thing on screen
		// that says the attachment was taken.
		REPORT_REATTACH.first().waitFor();

	}

	public boolean isAttachmentAccepted() {

		return !Wait.all(REPORT_REATTACH).isEmpty();

	}

	/**
	 * Submits the report and waits for the dialog to confirm it.
	 *
	 * Success is not a toast, and the dialog does not close - it swaps its contents for a
	 * "Report Submitted" panel with a Done button. Waiting for a toast, or for the dialog
	 * to disappear, both time out on a submission that worked perfectly well; that is
	 * exactly what happened the first time this flow was driven by hand.
	 */
	public void submitReport() {

		REPORT_SUBMIT.click();

		REPORT_SUBMITTED.first().waitFor();

	}

	public boolean isReportSubmitted() {

		return !Wait.all(REPORT_SUBMITTED).isEmpty();

	}

	public String getReportConfirmationText() {

		REPORT_SUBMITTED.first().waitFor();

		return REPORT_SUBMITTED.first().innerText().trim();

	}

	/** Dismisses the confirmation panel with its own Done button. */
	public void finishReport() {

		page.locator("xpath=" + "//button[normalize-space()='Done']").first().click();

		REPORT_HEADING.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

	}

	/**
	 * Closes the report dialog without submitting.
	 *
	 * Escape does not dismiss it - the only way out is the unlabelled X in its corner,
	 * which carries no text, title or aria-label, so its icon path is the handle.
	 */
	public void closeReportDialog() {

		Locator close = page.locator("xpath=" + "//button[.//*[local-name()='path'][@d='M6 18 18 6M6 6l12 12']]");

		close.click();

		REPORT_HEADING.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

	}

	/**
	 * Opens the delete confirmation for a chat.
	 *
	 * There is deliberately no method on this class that answers it with Delete. Removing
	 * a conversation from a shared test account is not something an unattended run should
	 * be able to do by accident, and every other scenario in the suite depends on the
	 * chat this one would delete. The confirmation is opened, read and cancelled.
	 */
	public void openDeleteChatDialog(String chatName) {

		selectChatMenuItem(chatName, "Delete");

		DELETE_PROMPT.first().waitFor();

	}

	public boolean isDeleteChatDialogOpen() {

		return !Wait.all(DELETE_PROMPT).isEmpty();

	}

	/** The confirmation's own Cancel. Stored lowercase and capitalised by a stylesheet. */
	public void cancelDeleteChatDialog() {

		page.locator("xpath=" + "//button[normalize-space()='cancel']").first().click();

		DELETE_PROMPT.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

	}

	/**
	 * Puts a chat back into its unpinned state, whatever state it is in now.
	 *
	 * A scenario that pins must not assume it starts unpinned. A run that fails between
	 * the pin and the unpin leaves the chat pinned for good, and every later run then
	 * finds Unpin where it expected Pin and times out - which is a leftover from an old
	 * failure being reported as a new one. Normalising first means a scenario can only
	 * fail for its own reasons.
	 */
	public void ensureChatUnpinned(String chatName) {

		// Ask the menu, not the badge.
		//
		// The badge in the row renders a moment after the row itself, so a single read can
		// come back empty for a chat that really is pinned. Acting on that false negative
		// is worse than the delay it saves: the code then clicks Pin on a menu that is
		// offering Unpin and waits out its whole timeout on an item that is not there.
		// The menu is built from the same state and is right the moment it opens.
		openChatMenu(chatName);

		if (!getChatMenuItems().contains("Unpin")) {
			closeChatMenu();
			return;
		}

		System.out.println("The chat " + chatName + " was left pinned by an earlier run, unpinning it first");

		clickChatMenuItem("Unpin");

		if (!waitForPinnedState(chatName, false, Duration.ofSeconds(20))) {
			throw new IllegalStateException("Could not return " + chatName + " to its unpinned state");
		}
	}

	/**
	 * Opens the chat's menu and clicks one of its items.
	 *
	 * Preferred over calling openChatMenu and clickChatMenuItem from separate steps. The
	 * menu is transient - it closes on its own when focus moves - so a step that assumes
	 * the menu a previous step opened is still there fails intermittently, which is
	 * exactly how the pin scenario went red once the run was headless.
	 */
	public void selectChatMenuItem(String chatName, String label) {

		openChatMenu(chatName);

		clickChatMenuItem(label);

	}

	public void clickChatMenuItem(String label) {

		Locator item = page.locator("xpath=" + "//ul//button[.//span[normalize-space()='" + label + "']]");

		item.click();

	}

	private static final String PIN_PATH = "m237.66 106.35";
	private static final String STAR_PATH = "M12.206 7.21";
	private static final String MUTE_PATH = "M13.5 4.06";

	private final Locator REPORT_HEADING;
	private final Locator REPORT_DESCRIPTION;

	/** The file input behind the Upload Attachment link. Hidden, which is why it is addressed by id. */
	private final Locator REPORT_ATTACHMENT;
	/** The link relabels itself to this once a file is held. */
	private final Locator REPORT_REATTACH;

	private final Locator REPORT_SUBMIT;
	private final Locator REPORT_SUBMITTED;
	private final Locator DELETE_PROMPT;

	/** Something only an open chat menu contains, used to tell open from closed. */
	private final Locator CHAT_MENU_MARKER;

	/**
	 * Closes the chat menu and waits until it is really gone.
	 *
	 * Returning while the menu is still on screen is worse than not closing it at all: the
	 * next open sees a menu already there, decides it has nothing to do, and the caller
	 * reads the state from before its own click. Escape usually does it; when it does not,
	 * clicking the search box moves focus out of the menu, which does.
	 */
	public void closeChatMenu() {

		page.keyboard().press("Escape");


		try {
			CHAT_MENU_MARKER.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

		} catch (com.microsoft.playwright.TimeoutError e) {

			page.locator("#search-group-header").all().stream()
					.filter(Locator::isVisible)
					.findFirst()
					.ifPresent(Locator::click);

			CHAT_MENU_MARKER.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
		}
	}

	/**
	 * Signs out and waits for the login page. The confirmation is answered here rather
	 * than in a separate step because a half finished logout leaves the session in a
	 * state no other step knows how to recover from.
	 */
	public void logout() {

		logoutButton.click();

		logoutConfirmButton.click();

		Wait.until(() -> page.url().contains("/auth/login"), 15);

		System.out.println("Logged out. Landed on: " + page.url());

	}

	/** Back on the login page, with the form actually rendered rather than just the URL changed. */
	public boolean isLoggedOut() {

		try {
			Wait.until(() -> page.url().contains("/auth/login"), 15);
			page.locator("#password").first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			return true;

		} catch (com.microsoft.playwright.TimeoutError e) {
			return false;
		}
	}
}
