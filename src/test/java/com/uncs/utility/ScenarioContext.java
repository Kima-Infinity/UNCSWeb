package com.uncs.utility;

/**
 * Values one step produces and a later step asserts on.
 *
 * The step definitions are split by page area, and Cucumber builds a fresh instance of
 * every step definition class for each scenario, so a field on one class cannot be read
 * from another. Anything that has to survive the hop between classes - the message text
 * that was actually sent, the contact that was searched for - is parked here instead.
 *
 * The fields are static for the same reason BaseClass's are, and {@link #reset()} is
 * called from the Before hook so a value can never leak from one scenario into the next.
 */
public class ScenarioContext {

	/** The message as it was actually sent, timestamp included. */
	public static String sentMessage;

	/** The message text after an edit, so the edit assertion knows what to look for. */
	public static String editedMessage;

	public static String openedChat;

	public static String searchedContact;

	public static String filteredCategory;

	public static String openedMerchant;

	/** The chat the block scenario acts on, which is never the one the rest of the suite uses. */
	public static String blockedChat;

	/** The chat the report scenario files against, kept off the conversation the suite uses. */
	public static String reportedChat;

	public static void reset() {

		sentMessage = null;
		editedMessage = null;
		openedChat = null;
		searchedContact = null;
		filteredCategory = null;
		openedMerchant = null;
		blockedChat = null;
		reportedChat = null;

	}
}
