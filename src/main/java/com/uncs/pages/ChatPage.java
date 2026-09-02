package com.uncs.pages;

import com.uncs.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.uncs.support.BrowserDiagnostics;

import java.time.Duration;

/**
 * A single conversation: opening it from the chat list, sending a message, and everything
 * the right click menu on a message offers.
 *
 * Opening a chat moves the browser to /c/{chatId}, which is the one URL in the product
 * that says which conversation is on screen. The id is generated, so the tests wait on
 * the path prefix rather than a whole address.
 *
 * WHAT IS DELIBERATELY NOT HERE
 *
 * The two buttons in the conversation header place a voice call. They carry no label,
 * no title and no aria-label, so nothing in the markup distinguishes them from an info
 * or a search control - which is how they got clicked by accident during exploration and
 * rang a real contact. They are left out on purpose: an unattended suite must never be
 * able to call somebody.
 */
public class ChatPage {

	Page page;
	
		private final Locator messageBox;


	/**
	 * The composer's only submit button. The attachment, voice and emoji buttons beside
	 * it are all type=button, so asking for the submit picks out Send without depending
	 * on an icon or a generated class.
	 */
		private final Locator sendButton;


		private final Locator composerForm;


	/**
	 * The confirmation shared by every destructive action in the product. The label is
	 * stored lowercase and shown capitalised by a stylesheet, so the XPath asks for 'yes'.
	 */
	private final Locator CONFIRM_YES;
	public ChatPage(Page ldriver) {

		this.page = ldriver;
		this.messageBox = page.locator("#message");
		this.sendButton = page.locator("xpath=//textarea[@id='message']/ancestor::form//button[@type='submit']");
		this.composerForm = page.locator("xpath=//textarea[@id='message']/ancestor::form");
		this.CONFIRM_YES = page.locator("xpath=" + "//button[normalize-space()='yes']");
	}

	/**
	 * Opens a conversation by the name shown in the list.
	 *
	 * A chat row is a list item holding the name in a semibold span, which is the only
	 * part of the row that is not an avatar, a timestamp or a message preview.
	 */
	public void openChat(String chatName) {

		Locator chatRow = page.locator("xpath=" + "//li[.//span[normalize-space()='" + chatName + "']]");

		chatRow.click();

		Wait.until(() -> page.url().contains("/c/"), 15);

		System.out.println("Opened the chat with " + chatName + " at " + page.url());

	}

	public boolean isChatOpen() {

		return page.url().contains("/c/")
				&& messageBox.isVisible();

	}

	/** Send is greyed out until the composer holds something, which is the empty message guard. */
	public boolean isSendEnabled() {

		return sendButton.isEnabled();

	}

	public void typeMessage(String message) {

		messageBox.fill(message);

	}

	public void clearComposer() {

		messageBox.fill("");

	}

	public void sendMessage(String message) {

		typeMessage(message);

		// Send stays disabled while the composer is empty, so wait for enabled rather than visible.
		sendButton.click();

	}

	/**
	 * Waits for the message to appear as a bubble in the thread.
	 *
	 * The body of a bubble is a paragraph holding the text on its own, with the
	 * timestamp and the delivery tick in sibling elements, so an exact match on the
	 * paragraph cannot be satisfied by a partially rendered bubble.
	 */
	public boolean isMessageInThread(String message) {

		return Wait.appears(messageBody(message).first());

	}

	private Locator messageBody(String message) {

		return page.locator("xpath=" + "//p[normalize-space()='" + message + "']");

	}

	/** The whole bubble around a message: quoted reply, reactions, timestamp and all. */
	private Locator bubbleOf(String message) {

		Locator bubble = page.locator("xpath=//p[normalize-space()='" + message + "']"
				+ "/ancestor::div[contains(@class,'rounded-2xl')][1]").first();

		bubble.waitFor();

		return bubble;

	}

	public String getBubbleText(String message) {

		return bubbleOf(message).innerText().trim();

	}

	/**
	 * Opens the action menu for a message.
	 *
	 * The menu is bound to a right click on the bubble. There is no hover affordance and
	 * no three dot button, so a context click is the only way in.
	 */
	public void openMessageMenu(String message) {

		ContextMenus.open(page, messageBody(message),
				page.locator("xpath=" + "//ul//button[.//span[normalize-space()='Reply']]"));

	}

	private void clickMenuItem(String label) {

		Locator item = page.locator("xpath=" + "//ul//button[.//span[normalize-space()='" + label + "']]");

		item.click();

	}

	/**
	 * Reacts to a message with one of the seven shortcuts above the menu.
	 *
	 * The emoji comes from emoji-picker-react, whose buttons carry the codepoint in
	 * data-unified - a far steadier handle than the glyph itself, which arrives through
	 * a font. The same code appears again in the full picker further down the menu, so
	 * the first match is the shortcut row.
	 */
	public void reactToMessage(String message, String unifiedCode) {

		openMessageMenu(message);

		Locator quickReaction = page.locator("xpath=" + "(//button[@data-unified='" + unifiedCode + "'])[1]");

		quickReaction.click();

	}

	/**
	 * A reaction shows as a small chip inside the bubble it belongs to.
	 *
	 * The chip arrives after the server acknowledges the reaction, so this waits for it
	 * rather than reading once. Reading once happened to work only while an implicit wait
	 * was quietly retrying every lookup for five seconds; with that gone, the honest
	 * answer to "is the reaction there yet" is a wait.
	 */
	public boolean hasReaction(String message, String emoji) {

		Locator chip = page.locator("xpath=" + "//p[normalize-space()='" + message + "']"
				+ "/ancestor::div[contains(@class,'rounded-2xl')][1]"
				+ "//button//span[normalize-space()='" + emoji + "']");

		return waitForPresence(chip);

	}

	/** True if the locator turns up within the page's timeout, false if it never does. */
	private boolean waitForPresence(Locator locator) {

		try {
			locator.first().waitFor();
			return true;

		} catch (com.microsoft.playwright.TimeoutError e) {
			return false;
		}
	}

	/**
	 * Starts a reply and confirms the composer picked up the quote.
	 *
	 * Replying does not send anything on its own - it puts a quoted preview above the
	 * composer, and the reply is whatever is typed next.
	 */
	public void startReplyTo(String message) {

		openMessageMenu(message);

		clickMenuItem("Reply");

		Wait.untilOrFail(() -> composerForm.innerText().contains(message), 15, "the page never settled");

	}

	public boolean isReplyQuoteShowing(String quotedMessage) {

		return composerForm.innerText().contains(quotedMessage);

	}

	/** A sent reply keeps the quoted original inside its own bubble. */
	public boolean isReplyTo(String replyMessage, String quotedMessage) {

		return getBubbleText(replyMessage).contains(quotedMessage);

	}

	/**
	 * Edits a message that was already sent.
	 *
	 * Choosing Edit loads the existing text back into the composer, so the old text has
	 * to be cleared before the new text is typed or the two would run together.
	 */
	public void editMessage(String message, String newMessage) {

		openMessageMenu(message);

		clickMenuItem("Edit Message");

		Wait.untilOrFail(() -> messageBox.inputValue() != null
				&& messageBox.inputValue().contains(message),
				15, "the edit box never showed the message");

		// clear() empties the DOM value without raising the events the framework listens
		// for, so its own copy of the text survives and the edit is submitted with the old
		// wording still attached. Selecting the text and typing over it goes through the
		// keyboard, which is the only route the composer actually notices.
		messageBox.fill("");
		messageBox.fill(newMessage);

		Wait.untilOrFail(() -> newMessage.equals(messageBox.inputValue()), 15, "the page never settled");

		sendButton.click();

		messageBody(newMessage).first().waitFor();

	}

	/** An edited message is marked as such next to its timestamp. */
	public boolean isMarkedEdited(String message) {

		return getBubbleText(message).toLowerCase().contains("edited");

	}

	public void copyMessageText(String message) {

		openMessageMenu(message);

		clickMenuItem("Copy Text");

	}

	/**
	 * Deletes a message and answers the confirmation.
	 *
	 * Deleting is what keeps a run from leaving a trail through a real conversation, so
	 * the scenarios that send a message finish by calling this on what they sent.
	 */
	public void deleteMessage(String message) {

		openMessageMenu(message);

		clickMenuItem("Delete Message");

		confirmDeletion();

		/*
		 * On failure, say what the browser was actually doing.
		 *
		 * This wait has timed out intermittently on roughly one run in three, always on
		 * the same scenario, and the bare timeout said only that the message was still
		 * there. Three explanations were proposed from that alone and all three were wrong
		 * when finally tested against the running application. The diagnostics attached
		 * here answer the questions the timeout cannot: whether the deletion placeholder
		 * arrived, whether the confirmation was still open, what the page last asked the
		 * API for, and what it logged while doing it.
		 */
		try {
			messageBody(message).first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

		} catch (com.microsoft.playwright.TimeoutError e) {
			throw new AssertionError(
					"The message was still in the thread after 15 seconds"
							+ " after the deletion was confirmed: \"" + message + "\""
							+ BrowserDiagnostics.report(page, "delete did not take effect", message), e);
		}

		System.out.println("Deleted the message: " + message);

	}

	/**
	 * Answers the delete confirmation, and checks the answer was taken.
	 *
	 * elementToBeClickable is satisfied as soon as the button is displayed and enabled,
	 * which happens while the dialog is still sliding into place. A click sent then lands
	 * where the button is about to be rather than where it is, the dialog stays open, and
	 * the wait after this call spends its whole timeout on a deletion that was never
	 * actually requested. By hand the same delete completes in about a third of a second,
	 * so a timeout there never meant the product was slow - it meant the click missed.
	 *
	 * The dialog closing is the only proof the click registered, so that is what is waited
	 * for, and a click that did not register is simply sent again.
	 */
	private void confirmDeletion() {


		for (int attempt = 1; attempt <= 3; attempt++) {

			CONFIRM_YES.click();

			try {
				CONFIRM_YES.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
				return;

			} catch (com.microsoft.playwright.TimeoutError e) {

				if (attempt == 3) {
					throw new AssertionError(
							"The delete confirmation stayed open after three attempts to answer it", e);
				}
				System.out.println("The delete confirmation did not take on attempt " + attempt
						+ ", answering it again");
			}
		}
	}

	/**
	 * A deleted message leaves a tombstone in the thread rather than vanishing, so the
	 * assertion is that the placeholder arrived, not simply that the text has gone.
	 */
	public boolean isDeletionTombstoneShown() {

		return waitForPresence(page.locator("xpath=" + "//*[normalize-space()='Message was deleted!']"));

	}
}
