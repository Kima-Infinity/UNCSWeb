Feature: UNCS contacts

  As a UNCS user
  I want to find a contact and start talking to them
  So that I can reach somebody without hunting through the chat list

  # These scenarios only read. Opening a contact starts or resumes a conversation and
  # sends nothing, so nothing here needs cleaning up afterwards.
  #
  # Contacts replaces the chat list inside the same panel rather than moving to its own
  # address, so nothing in the URL says the module is open. The search box is what tells
  # them apart: the chat list uses search-group-header and Contacts uses search-contact.

  Background:
    Given I am on the UNCS login page
    When I log into UNCS with the email credentials in "1" of "Sheet1" of "Login_TestData.xlsx"
    Then I should land on the UNCS home page

  @contacts
  Scenario: A contact is found by name and the conversation opens
    When I navigate to Contacts
    Then the contacts list should be displayed
    When I search for the contact in "1" of "Sheet1" of "Contacts_TestData.xlsx"
    Then the searched contact should be listed
    When I open the searched contact
    Then the chat window should be open
