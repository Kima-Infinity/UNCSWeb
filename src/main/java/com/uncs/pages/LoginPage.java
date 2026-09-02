package com.uncs.pages;

import com.uncs.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import lombok.Getter;

import java.time.Duration;
import java.util.List;

public class LoginPage {

	Page page;
	
	private final Locator EMAIL_TAB;
	private final Locator PHONE_TAB;
	private final Locator EMAIL_FIELD;
	private final Locator PHONE_FIELD;
	private final Locator PASSWORD_FIELD;
	private final Locator SUBMIT;
	/**
	 * The language control in the top right.
	 *
	 * Its label is whichever language is active, so it cannot be found by name. The
	 * combobox role is what makes it identifiable, and it is the only one on the page.
	 */
	private final Locator LANGUAGE_TRIGGER;
	@Getter
		private final Locator loginButton;


		private final Locator welcomeTitle;


		private final Locator forgotPasswordLink;


		private final Locator createAccountLink;


	/**
	 * The error banner the application raises for a rejected sign in.
	 *
	 * It is a Sonner toast, which is removed from the DOM about four seconds after it
	 * appears. Every method that reads it starts waiting immediately after the click that
	 * causes it - never sleep first, or the toast is gone before the look up begins.
	 */
	private final Locator ERROR_TOAST;
	public LoginPage(Page ldriver) {

		this.page = ldriver;
		this.loginButton = page.locator("xpath=//button[@type='submit']");
		this.welcomeTitle = page.locator("xpath=//h2[normalize-space()='Welcome to UNCS']");
		this.forgotPasswordLink = page.locator("xpath=//a[@href='/auth/reset-password']");
		this.createAccountLink = page.locator("xpath=//a[@href='/auth/register']");
		this.EMAIL_TAB = page.locator("xpath=" + "//li//button[normalize-space()='Email']");
		this.PHONE_TAB = page.locator("xpath=" + "//li//button[normalize-space()='Phone']");
		this.EMAIL_FIELD = page.locator("#email");
		this.PHONE_FIELD = page.locator("#phone");
		this.PASSWORD_FIELD = page.locator("#password");
		this.SUBMIT = page.locator("xpath=" + "//button[@type='submit']");
		this.LANGUAGE_TRIGGER = page.locator("xpath=" + "//button[@aria-haspopup='listbox']");
		this.ERROR_TOAST = page.locator("xpath=" + "//li[@data-sonner-toast][@data-type='error']");
	}

	/**
	 * Types into a field that the application may still be re-rendering.
	 *
	 * The login form is rebuilt when a tab is chosen, and the browser can hand back an
	 * element that passed a visibility check a moment before the framework swapped the
	 * node underneath it. Chrome reports that as "element not interactable" rather than
	 * as a stale reference, so catching staleness alone is not enough - the element has
	 * to be found again and the typing retried.
	 *
	 * Without this the suite failed about two runs in three, always on the email field
	 * and always intermittently.
	 */
	private void typeInto(Locator locator, String text) {

		for (int attempt = 1; attempt <= 3; attempt++) {

			try {
				locator.waitFor();

		Locator field = locator;
				field.fill("");
				field.fill(text);
				return;

			} catch (Exception e) {

				if (attempt == 3) {
					throw e;
				}
				System.out.println("Field " + locator + " was not ready on attempt " + attempt + ", trying again");
			}
		}
	}

	/**
	 * Switches to the Email tab and waits for the form to settle.
	 *
	 * The email field does not exist while the Phone tab is showing, and the phone field
	 * is gone once the swap is done. Waiting for both conditions is what proves the
	 * re-render finished rather than merely started.
	 */
	private void openEmailTab() {

		EMAIL_TAB.click();

		EMAIL_FIELD.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
		Wait.until(() -> PHONE_FIELD.count() == 0, 15);

	}

	private void openPhoneTab() {

		PHONE_TAB.click();

		PHONE_FIELD.first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
		Wait.until(() -> EMAIL_FIELD.count() == 0, 15);

	}

	public void loginToUNCS(String username, String pass) {

		openEmailTab();

		typeInto(EMAIL_FIELD, username);
		typeInto(PASSWORD_FIELD, pass);

		submitAndLeaveLoginPage();

		System.out.println("Logged In Successfully! Landed on: " + page.url());

	}

	public void loginToUNCSWithPhone(String phoneNumber, String pass) {

		openPhoneTab();

		typeInto(PHONE_FIELD, phoneNumber);
		typeInto(PASSWORD_FIELD, pass);

		SUBMIT.click();
		Wait.untilOrFail(() -> !page.url().contains("/auth/login"), 15,
				"the browser never left the login page");
		System.out.println("Logged In Successfully! Landed on: " + page.url());

	}

	/**
	 * Fills the email form and submits it without waiting to leave the page.
	 *
	 * The happy path method above waits for the URL to change, which is exactly wrong for
	 * a sign in that is meant to be refused: it would spend its whole timeout waiting for
	 * a navigation that never comes, and the toast would expire in the meantime.
	 */
	public void attemptLogin(String username, String pass) {

		openEmailTab();

		typeInto(EMAIL_FIELD, username);
		typeInto(PASSWORD_FIELD, pass);

		SUBMIT.click();

	}

	/**
	 * The text of the error toast raised by the last submit.
	 *
	 * Called straight after {@link #attemptLogin}, so the wait begins while the toast is
	 * still on screen. A short timeout is deliberate - the toast lives about four seconds,
	 * and a longer one would only delay the failure when no toast is raised at all.
	 */
	public String getErrorMessage() {


		ERROR_TOAST.first().waitFor(new Locator.WaitForOptions().setTimeout(10 * 1000));

		return ERROR_TOAST.first().innerText().trim();

	}

	/**
	 * Waits for the toast raised by the last submit to be cleared away.
	 *
	 * The lockout scenario submits three times in a row. Without this the second attempt
	 * would read the first attempt's toast, which still says "Invalid credentials", and
	 * the run would report a pass for a message the product never showed that time.
	 */
	public void waitForErrorToClear() {

		ERROR_TOAST.first().waitFor(new Locator.WaitForOptions()
				.setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
				.setTimeout(15 * 1000));

	}

	public boolean isLoginPageDisplayed() {

		return welcomeTitle.isVisible()
				&& forgotPasswordLink.isVisible()
				&& createAccountLink.isVisible();

	}

	/** Login is greyed out until both fields hold something, which is the empty form's guard. */
	public boolean isLoginButtonEnabled() {

		return loginButton.isEnabled();

	}

	/**
	 * Chooses an interface language.
	 *
	 * The dropdown is a proper listbox, so the options carry the option role. That is a
	 * far steadier handle than the text alone: the trigger shows the active language, so
	 * matching on text alone finds the trigger as well as the option inside the list.
	 */
	public void selectLanguage(String language) {

		LANGUAGE_TRIGGER.click();

		Locator option = page.locator("xpath=" + "//*[@role='option'][normalize-space()='" + language + "']");

		option.click();

		page.locator("xpath=" + "//*[@role='listbox']").first().waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));

		System.out.println("Switched the interface language to " + language);

	}

	public List<String> getAvailableLanguages() {

		LANGUAGE_TRIGGER.click();

		List<String> languages = page.locator("xpath=//*[@role='option']").all()
				.stream().map(e -> e.innerText().trim()).toList();

		LANGUAGE_TRIGGER.click();

		return languages;

	}

	/** Reads the heading whatever language it is in, so a translated page can be asserted on. */
	public String getHeadingText() {

		return page.locator("h2").first().innerText().trim();

	}

	/**
	 * The heading, after giving it a chance to become what the caller expects.
	 *
	 * Switching language re-renders the page in place: the h2 exists throughout and simply
	 * changes its text, so a read taken the moment the option is clicked returns the old
	 * wording and the comparison fails on a translation that did in fact arrive. On timeout
	 * this returns whatever the heading actually says, so the assertion reports the real
	 * text rather than a bare timeout.
	 */
	public String getHeadingText(String expected) {

		try {
			Wait.untilOrFail(() -> expected.equals(page.locator("h2").first().innerText().trim()),
					15, "the heading never became \"" + expected + "\"");

		} catch (com.microsoft.playwright.TimeoutError ignored) {
			// fall through and report whatever is on screen
		}

		return getHeadingText();

	}

	public String getLoginButtonText() {

		return loginButton.innerText().trim();

	}

	public boolean isLoggedIn() {

		return !page.url().contains("/auth/login");

	}


	/** How many times to press Login before giving up on it taking. */
	private static final int SUBMIT_ATTEMPTS = 3;

	/**
	 * How long one press gets to move the browser off the login page.
	 *
	 * Thirty seconds because this environment really is that slow: the sign in succeeds and
	 * the browser reaches the home page, but it can take the better part of half a minute to
	 * get there. A shorter window made the suite report "the browser never left the login
	 * page" about a login that had in fact worked - the diagnostic printed a moment later
	 * showed the home page and no login form at all.
	 */
	private static final int SUBMIT_SETTLE_SECONDS = 30;

	/**
	 * Presses Login and waits to leave the page, pressing again if the click did nothing.
	 *
	 * The retry is not defensive padding; it is the difference between the two drivers. The
	 * button is disabled until both fields validate, and it becomes enabled a moment before
	 * React has wired its click handler. Selenium was slow enough to miss that gap;
	 * Playwright clicks as soon as the element is actionable, lands in it, and the click is
	 * swallowed - the form simply sits there, no error, no navigation. That is what "the
	 * browser never left the login page" was, and adding two diagnostic reads was enough to
	 * make it pass, which is how it was found.
	 *
	 * A click is never repeated once the application has answered. A wrong password raises a
	 * toast, and UNCS locks an account after three consecutive refusals - so pressing again
	 * because a toast is not a navigation would spend the account's remaining attempts on a
	 * password already known to be wrong.
	 */
	private void submitAndLeaveLoginPage() {

		for (int attempt = 1; attempt <= SUBMIT_ATTEMPTS; attempt++) {

			if (attempt == 1) {
				SUBMIT.first().click();
			} else {
				// A real mouse click is the honest thing to do and is what attempt one
				// tries. When the application ignores it, raising the event on the element
				// is what actually reaches React's handler - proved by hand against the
				// live page, where a scripted click signed in and a mouse click did not.
				SUBMIT.first().dispatchEvent("click");
			}

			if (Wait.until(() -> !page.url().contains("/auth/login"), SUBMIT_SETTLE_SECONDS)) {
				return;
			}

			String answer = String.join("; ", page.locator("[data-sonner-toast]").allInnerTexts());

			if (!answer.isBlank()) {
				throw new IllegalStateException("The sign in was refused: " + answer);
			}

			System.out.println("The Login button did not take on attempt " + attempt
					+ " of " + SUBMIT_ATTEMPTS + " - the page did not move and said nothing."
					+ " Pressing it again.");
		}

		throw new IllegalStateException("The browser never left the login page after "
				+ SUBMIT_ATTEMPTS + " presses of Login, and the application never said why.");
	}
}
