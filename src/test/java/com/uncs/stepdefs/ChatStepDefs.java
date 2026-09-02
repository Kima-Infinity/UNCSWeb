package com.uncs.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.uncs.pages.ChatPage;
import com.uncs.pages.HomePage;
import com.uncs.utility.BaseClass;
import com.uncs.utility.ExcelDataProvider;
import com.uncs.utility.Helper;
import com.uncs.utility.ScenarioContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Everything a conversation can do: opening it, sending, reacting, replying, editing and
 * deleting, plus the chat list's own right click menu.
 *
 * Scenarios here send real messages into a real conversation, so each one deletes what it
 * sent before it finishes. The text carries a timestamp, which serves two purposes: it
 * makes a run's traffic obvious to a human reading the thread later, and it stops an
 * assertion from passing on a bubble an earlier run left behind.
 */
public class ChatStepDefs {

	ChatPage chatPage;
	HomePage homePage;
	ExcelDataProvider excel;

	private ChatPage chat() {
		if (chatPage == null) {
			chatPage = new ChatPage(BaseClass.driver);
		}
		return chatPage;
	}

	private HomePage home() {
		if (homePage == null) {
			homePage = new HomePage(BaseClass.driver);
		}
		return homePage;
	}

	@When("I open the chat named in {string} of {string} of {string}")
	public void iOpenTheChat(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		ScenarioContext.openedChat = excel.getStringData(sheetName, row, 1);

		chat().openChat(ScenarioContext.openedChat);

		BaseClass.logger.pass("Opened the chat with " + ScenarioContext.openedChat);

	}

	@Then("the chat window should be open")
	public void theChatWindowShouldBeOpen() {

		assertTrue(chat().isChatOpen(), "The chat window did not open");

		BaseClass.logger.pass("The chat window is open at " + BaseClass.driver.url());

	}

	@Then("the Send button should be disabled while the composer is empty")
	public void theSendButtonShouldBeDisabled() {

		chat().clearComposer();

		assertFalse(chat().isSendEnabled(),
				"Send is enabled on an empty composer - it should stay disabled until something is typed");

		BaseClass.logger.pass("Send is disabled while the composer is empty");

	}

	@When("I send the message in {string} of {string} of {string}")
	public void iSendTheMessage(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		// Stamped so two runs never send the same text, which is what stops the assertion
		// below from passing on a bubble left over from an earlier run.
		ScenarioContext.sentMessage = Helper.getUniqueMessage(excel.getStringData(sheetName, row, 2));

		chat().sendMessage(ScenarioContext.sentMessage);

		BaseClass.logger.pass("Sent the message: " + ScenarioContext.sentMessage);

	}

	@Then("the message should appear in the chat thread")
	public void theMessageShouldAppear() {

		assertTrue(chat().isMessageInThread(ScenarioContext.sentMessage),
				"The message never appeared in the thread: " + ScenarioContext.sentMessage);

		BaseClass.logger.pass("The message is in the thread");

	}

	@When("I react to the message with the emoji in {string} of {string} of {string}")
	public void iReactToTheMessage(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String unified = excel.getStringData(sheetName, row, 3);

		chat().reactToMessage(ScenarioContext.sentMessage, unified);

		BaseClass.logger.pass("Reacted to the message with codepoint " + unified);

	}

	@Then("the reaction in {string} of {string} of {string} should be shown on the message")
	public void theReactionShouldBeShown(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String emoji = excel.getStringData(sheetName, row, 4);

		assertTrue(chat().hasReaction(ScenarioContext.sentMessage, emoji),
				"The " + emoji + " reaction is not on the message");

		BaseClass.logger.pass("The message carries the " + emoji + " reaction");

	}

	@When("I start a reply to the message")
	public void iStartAReply() {

		chat().startReplyTo(ScenarioContext.sentMessage);

		BaseClass.logger.pass("Started a reply to the message");

	}

	@Then("the composer should quote the message being replied to")
	public void theComposerShouldQuote() {

		assertTrue(chat().isReplyQuoteShowing(ScenarioContext.sentMessage),
				"The composer is not showing the quoted message");

		BaseClass.logger.pass("The composer quotes the message being replied to");

	}

	@When("I send the reply")
	public void iSendTheReply() {

		ScenarioContext.editedMessage = Helper.getUniqueMessage("Automated reply");

		chat().sendMessage(ScenarioContext.editedMessage);

		BaseClass.logger.pass("Sent the reply: " + ScenarioContext.editedMessage);

	}

	@Then("the reply should carry the quoted message")
	public void theReplyShouldCarryTheQuote() {

		assertTrue(chat().isReplyTo(ScenarioContext.editedMessage, ScenarioContext.sentMessage),
				"The reply bubble does not contain the message it was replying to");

		BaseClass.logger.pass("The reply carries the quoted message");

	}

	@When("I edit the message with the suffix in {string} of {string} of {string}")
	public void iEditTheMessage(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String suffix = excel.getStringData(sheetName, row, 5);

		String updated = ScenarioContext.sentMessage + " " + suffix;

		chat().editMessage(ScenarioContext.sentMessage, updated);

		// Everything after this point works on the new text, including the delete that
		// cleans up, so the context has to follow the rename.
		ScenarioContext.sentMessage = updated;

		BaseClass.logger.pass("Edited the message to: " + updated);

	}

	@Then("the message should be marked as edited")
	public void theMessageShouldBeMarkedEdited() {

		assertTrue(chat().isMarkedEdited(ScenarioContext.sentMessage),
				"The edited message is not marked as edited");

		BaseClass.logger.pass("The message is marked as edited");

	}

	@When("I delete the message")
	public void iDeleteTheMessage() {

		chat().deleteMessage(ScenarioContext.sentMessage);

		BaseClass.logger.pass("Deleted the message");

	}

	@When("I delete the reply")
	public void iDeleteTheReply() {

		chat().deleteMessage(ScenarioContext.editedMessage);

		BaseClass.logger.pass("Deleted the reply");

	}

	@Then("the message should be replaced by the deleted placeholder")
	public void theMessageShouldBeReplacedByPlaceholder() {

		assertTrue(chat().isDeletionTombstoneShown(),
				"The thread does not show the 'Message was deleted!' placeholder");

		BaseClass.logger.pass("The deleted message left the expected placeholder");

	}

	@When("I search the chat list for the chat name")
	public void iSearchTheChatList() {

		home().searchChat(ScenarioContext.openedChat);

		BaseClass.logger.pass("Searched the chat list for " + ScenarioContext.openedChat);

	}

	@Then("the chat list should show the matching chat")
	public void theChatListShouldShowTheMatch() {

		assertTrue(home().isChatListed(ScenarioContext.openedChat),
				"The chat list search did not return " + ScenarioContext.openedChat);

		BaseClass.logger.pass("The chat list search returned " + ScenarioContext.openedChat);

	}

	@When("I open the chat menu for that chat")
	public void iOpenTheChatMenu() {

		home().openChatMenu(ScenarioContext.openedChat);

		BaseClass.logger.pass("Opened the chat menu for " + ScenarioContext.openedChat);

	}

	@Then("the chat menu should offer Star, Mute, Pin, Block User, Report and Delete")
	public void theChatMenuShouldOfferItsActions() {

		List<String> items = home().getChatMenuItems();

		for (String expected : List.of("Mute", "Block User", "Report", "Delete")) {
			assertTrue(items.contains(expected),
					"The chat menu is missing " + expected + ". It offered: " + items);
		}

		// Star and Pin read as Unstar and Unpin when they are already on, so the check
		// has to accept either wording rather than the switched off label alone.
		assertTrue(items.contains("Star") || items.contains("Unstar"),
				"The chat menu offers neither Star nor Unstar. It offered: " + items);

		assertTrue(items.contains("Pin") || items.contains("Unpin"),
				"The chat menu offers neither Pin nor Unpin. It offered: " + items);

		// Leave the menu closed. A step that hands the next one an open menu makes that
		// step's behaviour depend on this one having run first, which is exactly the kind
		// of coupling that made this scenario fail for a different reason on every run.
		home().closeChatMenu();

		BaseClass.logger.pass("The chat menu offers every action: " + items);

	}

	/**
	 * Puts the chat back to unpinned before the pin test starts.
	 *
	 * A run that dies between the pin and the unpin leaves the chat pinned for good, and
	 * every later run then finds the opposite of what it expected - an old failure being
	 * reported as a new one. Normalising first means the scenario can only fail for its
	 * own reasons.
	 */
	@When("the chat starts out unpinned")
	public void theChatStartsOutUnpinned() {

		home().ensureChatUnpinned(ScenarioContext.openedChat);

		BaseClass.logger.pass("The chat is unpinned to begin with");

	}

	/**
	 * Blocks a chat other than the one every other scenario uses.
	 *
	 * Deliberately not the chat in the ChatName column: blocking stops that contact being
	 * messaged, so aiming this at the conversation the rest of the suite depends on would
	 * take the whole suite down if the unblock ever failed to run.
	 */
	@When("I block the chat named in {string} of {string} of {string}")
	public void iBlockTheChat(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		ScenarioContext.blockedChat = excel.getStringData(sheetName, row, 6);

		home().goToChats();

		home().blockUser(ScenarioContext.blockedChat);

		BaseClass.logger.pass("Blocked " + ScenarioContext.blockedChat);

	}

	@When("I star that chat")
	public void iStarThatChat() {

		home().selectChatMenuItem(ScenarioContext.openedChat, "Star");

		BaseClass.logger.pass("Starred the chat");

	}

	@Then("the chat should show as starred")
	public void theChatShouldShowAsStarred() {

		assertTrue(home().waitForStarredState(ScenarioContext.openedChat, true, Duration.ofSeconds(20)),
				"The chat never picked up the starred badge in its row");

		BaseClass.logger.pass("The chat row shows the starred badge");

	}

	@When("I unstar that chat")
	public void iUnstarThatChat() {

		home().selectChatMenuItem(ScenarioContext.openedChat, "Unstar");

		BaseClass.logger.pass("Unstarred the chat");

	}

	@Then("the chat should no longer show as starred")
	public void theChatShouldNoLongerShowAsStarred() {

		assertTrue(home().waitForStarredState(ScenarioContext.openedChat, false, Duration.ofSeconds(20)),
				"The chat still carries the starred badge in its row");

		BaseClass.logger.pass("The chat row no longer shows the starred badge");

	}

	@When("I mute that chat for the duration in {string} of {string} of {string}")
	public void iMuteThatChat(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String duration = excel.getStringData(sheetName, row, 7);

		home().muteChat(ScenarioContext.openedChat, duration);

		BaseClass.logger.pass("Muted the chat: " + duration);

	}

	@Then("the chat should show as muted")
	public void theChatShouldShowAsMuted() {

		assertTrue(home().waitForMutedState(ScenarioContext.openedChat, true, Duration.ofSeconds(20)),
				"The chat never picked up the muted badge in its row");

		BaseClass.logger.pass("The chat row shows the muted badge");

	}

	@When("I unmute that chat")
	public void iUnmuteThatChat() {

		home().unmuteChat(ScenarioContext.openedChat);

		BaseClass.logger.pass("Unmuted the chat");

	}

	@Then("the chat should no longer show as muted")
	public void theChatShouldNoLongerShowAsMuted() {

		assertTrue(home().waitForMutedState(ScenarioContext.openedChat, false, Duration.ofSeconds(20)),
				"The chat still carries the muted badge in its row");

		BaseClass.logger.pass("The chat row no longer shows the muted badge");

	}

	/**
	 * Opens the report dialog against the reportable chat rather than the main one.
	 *
	 * Uses the same column as the block scenario. A submitted report cannot be withdrawn,
	 * so it is filed against the account the suite has already set aside for actions it
	 * cannot take back, never the conversation every other scenario depends on.
	 */
	@When("I open the report dialog for the chat named in {string} of {string} of {string}")
	public void iOpenTheReportDialogFor(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		ScenarioContext.reportedChat = excel.getStringData(sheetName, row, 6);

		home().goToChats();

		home().openReportDialog(ScenarioContext.reportedChat);

		BaseClass.logger.pass("Opened the report dialog for " + ScenarioContext.reportedChat);

	}

	@When("I choose the report reason in {string} of {string} of {string}")
	public void iChooseTheReportReason(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String reason = excel.getStringData(sheetName, row, 8);

		home().selectReportReason(reason);

		BaseClass.logger.pass("Chose the report reason: " + reason);

	}

	@When("I describe the report using {string} of {string} of {string}")
	public void iDescribeTheReport(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		// Stamped so somebody reading the moderation queue can tell one automated report
		// from another, and tell all of them from a real one.
		String description = Helper.getUniqueMessage(excel.getStringData(sheetName, row, 9));

		home().enterReportDescription(description);

		BaseClass.logger.pass("Described the report: " + description);

	}

	@When("I attach the file in {string} of {string} of {string}")
	public void iAttachTheFile(String rowNumber, String sheetName, String fileName) {

		int row = Integer.parseInt(rowNumber);
		excel = new ExcelDataProvider(fileName, sheetName);

		String path = excel.getStringData(sheetName, row, 10);

		home().attachReportFile(path);

		BaseClass.logger.pass("Attached " + path + " to the report");

	}

	@Then("the attachment should be accepted")
	public void theAttachmentShouldBeAccepted() {

		assertTrue(home().isAttachmentAccepted(),
				"The dialog never acknowledged the attachment");

		BaseClass.logger.pass("The dialog acknowledged the attachment");

	}

	@When("I submit the report")
	public void iSubmitTheReport() {

		home().submitReport();

		BaseClass.logger.pass("Submitted the report");

	}

	@Then("the report should be confirmed as submitted")
	public void theReportShouldBeConfirmed() {

		assertTrue(home().isReportSubmitted(),
				"The dialog never confirmed the report was submitted");

		BaseClass.logger.pass("The report was confirmed: " + home().getReportConfirmationText());

	}

	@When("I dismiss the report confirmation")
	public void iDismissTheReportConfirmation() {

		home().finishReport();

		BaseClass.logger.pass("Dismissed the report confirmation");

	}

	@When("I open the report dialog for that chat")
	public void iOpenTheReportDialog() {

		home().openReportDialog(ScenarioContext.openedChat);

		BaseClass.logger.pass("Opened the report dialog");

	}

	@Then("the report dialog should offer every reason")
	public void theReportDialogShouldOfferEveryReason() {

		assertTrue(home().isReportDialogOpen(), "The report dialog did not open");

		for (String reason : List.of("Spam", "Harassment", "Inappropriate Content", "Fake Account", "Other")) {
			assertTrue(home().reportDialogOffers(reason),
					"The report dialog is missing the " + reason + " reason");
		}

		BaseClass.logger.pass("The report dialog offers all five reasons");

	}

	/**
	 * Closes the dialog without submitting.
	 *
	 * Submitting would file a real report against a real account on the shared test
	 * environment, and there is no way to withdraw one. The page object has no method
	 * that presses Submit Report, so this cannot happen by accident either.
	 */
	@When("I close the report dialog without submitting")
	public void iCloseTheReportDialog() {

		home().closeReportDialog();

		BaseClass.logger.pass("Closed the report dialog without submitting");

	}

	@When("I open the delete confirmation for that chat")
	public void iOpenTheDeleteConfirmation() {

		home().openDeleteChatDialog(ScenarioContext.openedChat);

		BaseClass.logger.pass("Opened the delete confirmation");

	}

	@Then("the delete confirmation should ask before removing the chat")
	public void theDeleteConfirmationShouldAsk() {

		assertTrue(home().isDeleteChatDialogOpen(),
				"Deleting a chat did not raise a confirmation");

		BaseClass.logger.pass("Deleting a chat asks for confirmation first");

	}

	/**
	 * Cancels rather than confirming.
	 *
	 * Every other scenario in the suite talks to this conversation, so deleting it would
	 * take the rest of the suite with it. The page object deliberately has no method that
	 * answers this confirmation with Delete.
	 */
	@When("I cancel the delete confirmation")
	public void iCancelTheDeleteConfirmation() {

		home().cancelDeleteChatDialog();

		BaseClass.logger.pass("Cancelled the delete confirmation");

	}

	@Then("the chat should still be listed")
	public void theChatShouldStillBeListed() {

		assertTrue(home().isChatListed(ScenarioContext.openedChat),
				"The chat is gone from the list after cancelling the delete");

		BaseClass.logger.pass("The chat is still listed");

	}

	@When("I pin that chat")
	public void iPinThatChat() {

		home().selectChatMenuItem(ScenarioContext.openedChat, "Pin");

		BaseClass.logger.pass("Pinned the chat");

	}

	@Then("the chat should show as pinned")
	public void theChatShouldShowAsPinned() {

		assertTrue(home().waitForPinnedState(ScenarioContext.openedChat, true, Duration.ofSeconds(20)),
				"The chat never picked up the pinned badge in its row");

		BaseClass.logger.pass("The chat row shows the pinned badge");

	}

	@When("I unpin that chat")
	public void iUnpinThatChat() {

		home().selectChatMenuItem(ScenarioContext.openedChat, "Unpin");

		BaseClass.logger.pass("Unpinned the chat");

	}

	@Then("the chat should no longer show as pinned")
	public void theChatShouldNoLongerShowAsPinned() {

		assertTrue(home().waitForPinnedState(ScenarioContext.openedChat, false, Duration.ofSeconds(20)),
				"The chat still carries the pinned badge in its row");

		BaseClass.logger.pass("The chat row no longer shows the pinned badge");

	}
}
