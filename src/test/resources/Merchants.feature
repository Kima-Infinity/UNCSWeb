Feature: UNCS merchant directory

  As a UNCS user
  I want to browse, filter and search merchants and open one
  So that I can find a business and start dealing with it

  # THE DIRECTORY IS SHARED DATA
  #
  # The category and search assertions depend on what the test environment actually holds.
  # "Food & Dining" returned two cards and "Apple" returned one when this was written; if
  # somebody adds or removes a merchant, these go red for a data reason rather than a
  # product one. Adjust Merchants_TestData.xlsx rather than loosening the assertion - a
  # filter test that accepts any number of results is not testing the filter.
  #
  # ONE ACTION WRITES, AND PUTS ITSELF BACK
  #
  # Following a merchant is the only change these scenarios make to the account, and the
  # step that follows also unfollows. That restore lives in the same step as the change on
  # purpose: a separate cleanup step is one a failure can skip straight past, leaving the
  # account following a merchant it was never meant to follow.
  #
  # A MERCHANT PAGE RENDERS ITS BUTTONS TWICE
  #
  # Follow and Chat are laid out once for narrow screens and once for wide, and both
  # copies sit in the DOM with only one displayed. The page object filters for the visible
  # one; a plain findElement would hit the hidden copy about half the time.

  Background:
    Given I am on the UNCS login page
    When I log into UNCS with the email credentials in "1" of "Sheet1" of "Login_TestData.xlsx"
    Then I should land on the UNCS home page

  @merchants
  Scenario: The directory filters down to one category
    When I navigate to Merchants
    Then the merchant directory should be displayed
    When I filter the merchants by the category in "1" of "Sheet1" of "Merchants_TestData.xlsx"
    Then every merchant listed should belong to that category

  @merchants
  Scenario: A merchant can be found by name and opened
    When I navigate to Merchants
    Then the merchant directory should be displayed
    When I search the merchants for the term in "1" of "Sheet1" of "Merchants_TestData.xlsx"
    Then only merchants matching the search should be listed
    When I open that merchant
    Then the merchant page should offer Follow and Chat

  @merchants
  Scenario: A merchant can be followed and unfollowed again
    When I navigate to Merchants
    And I search the merchants for the term in "1" of "Sheet1" of "Merchants_TestData.xlsx"
    And I open that merchant
    Then the merchant page should offer Follow and Chat
    When I follow and unfollow that merchant
    Then the merchant should be back to not followed
