Feature: UNCS sign in

  As a UNCS user
  I want the login page to accept my credentials and refuse everything else
  So that my account is reachable to me and to nobody else

  # THE ERROR TOAST IS SHORT LIVED
  #
  # A refused sign in raises a Sonner toast that is in the DOM for roughly four seconds -
  # measured at about 390ms to appear and gone by 4.6s. Every step that reads it starts
  # waiting the instant the click happens. Never put a sleep in front of one of these
  # steps: the toast will have been removed before the look up begins, and the failure
  # reads as "no error was shown" when the error was shown and missed.
  #
  # WHICH ACCOUNT TO POINT AT
  #
  # UNCS locks an account after roughly three consecutive failed sign ins, and a locked
  # account stays locked until somebody clears it by hand. That makes the choice of account
  # for a negative test a real decision rather than a detail:
  #
  #   Row 2 is an address that does not exist, which answers "User not found!" and can
  #   never be locked no matter how often it runs. That is what the routine negative test
  #   uses, so a nightly can run forever without locking anything.
  #
  #   Rows 3 and 4 use nami@gmail.com and belong to the @lockout scenario alone. An earlier
  #   version of this file pointed the routine test at nami as well, which quietly spent one
  #   of its three attempts on every run and locked the account on the third - the suite then
  #   failed on a message the product was right to show.
  #
  # Neither ever points at luffynew@gmail.com, the shared account every other feature signs
  # in as.

  @login
  Scenario: The login page opens with everything a user needs
    Given I am on the UNCS login page
    Then the login page should show all of its controls
    And the Login button should be disabled

  @login
  Scenario: The interface can be read in Traditional Chinese
    Given I am on the UNCS login page
    When I switch the interface language to "繁體中文"
    Then the login page heading should read "歡迎使用UNCS"
    When I switch the interface language to "English"
    Then the login page heading should read "Welcome to UNCS"

  @login
  Scenario: A valid account signs in and reaches the chat list
    Given I am on the UNCS login page
    When I log into UNCS with the email credentials in "1" of "Sheet1" of "Login_TestData.xlsx"
    Then I should land on the UNCS home page
    And I should see the chat list
    And the navigation rail should offer every module
    Then I should be able to successfully log out

  # Aimed at an address that does not exist, so it can run on every build forever without
  # ever locking anything. See the note above.
  @login
  Scenario: An unknown account is refused and the user stays on the login page
    Given I am on the UNCS login page
    When I attempt to log into UNCS with the credentials in "2" of "Sheet1" of "Login_TestData.xlsx"
    Then the sign in should be refused with the message in "2" of "Sheet1" of "Login_TestData.xlsx"

  # THIS SCENARIO LEAVES AN ACCOUNT LOCKED
  #
  # It is tagged separately from the rest of @login and excluded from every unattended run,
  # because that is exactly what it is for: it drives nami@gmail.com past the lockout
  # threshold on purpose and asserts the product says so rather than repeating "Invalid
  # credentials". Three failed attempts is what tripped it when this was found by hand.
  #
  # After it passes, nami@gmail.com is locked and stays locked until somebody unlocks it.
  # Do not run it as part of a nightly. Run it deliberately, when you are ready to clear
  # the lock afterwards:
  #
  #   mvn test -Dcucumber.filter.tags="@lockout"
  @lockout
  Scenario: An account locks after repeated failed sign ins
    Given I am on the UNCS login page
    When I attempt to log into UNCS with the credentials in "3" of "Sheet1" of "Login_TestData.xlsx"
    Then the sign in should be refused with the message in "3" of "Sheet1" of "Login_TestData.xlsx"
    When I attempt to log into UNCS with the credentials in "3" of "Sheet1" of "Login_TestData.xlsx"
    Then the sign in should be refused with the message in "3" of "Sheet1" of "Login_TestData.xlsx"
    When I attempt to log into UNCS with the credentials in "3" of "Sheet1" of "Login_TestData.xlsx"
    Then the sign in should be refused with the message in "3" of "Sheet1" of "Login_TestData.xlsx"
    When I attempt to log into UNCS with the credentials in "4" of "Sheet1" of "Login_TestData.xlsx"
    Then the sign in should be refused with the message in "4" of "Sheet1" of "Login_TestData.xlsx"
