Feature: UNCS end to end journey

  As a UNCS user
  I want to sign in, message a contact, browse contacts and merchants, check my profile and sign out
  So that one run proves the whole product hangs together rather than each module alone

  # WHY THIS EXISTS
  #
  # It is deliberately one long scenario: every step after the first runs on the state the
  # previous step left behind, which is the thing a per-module test cannot tell you. The cost
  # is that a single scenario stops at the first failure, so a broken Chats step hides
  # Contacts, Merchants and Profile. Use this to prove the journey, and add per-module
  # feature files when you need to pinpoint one.
  #
  # WHERE THE DATA COMES FROM
  #
  # Every step that needs a value reads it from a workbook under TestData. Row 0 of a sheet is
  # the header, so "1" is the first row of real data. The sheet name is case sensitive.
  #
  # The Login sheet ships with the word ENV in its Password column rather than a real password,
  # so the secret keeps coming from UNCS_PASSWORD and nothing confidential is committed:
  #
  #   PowerShell:  $env:UNCS_PASSWORD = 'yourPassword'
  #
  # WHAT IT COSTS TO RUN
  #
  # It signs into a shared account on the test environment and sends a real message to the
  # contact named in Chats_TestData.xlsx. The message carries a timestamp so each run is
  # distinguishable and the assertion cannot pass on a bubble an earlier run left behind.
  # Test environment only.
  #
  # Run it on its own:  mvn test -Dcucumber.filter.tags="@e2e"

  @e2e
  Scenario: A user signs in, messages a contact, browses the directory and signs out

    # 1 - Sign in
    Given I am on the UNCS login page
    When I log into UNCS with the email credentials in "1" of "Sheet1" of "Login_TestData.xlsx"
    Then I should land on the UNCS home page
    And I should see the chat list

    # 2 - Chats: open a conversation and send a message
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the chat window should be open
    When I send the message in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the message should appear in the chat thread

    # 3 - Contacts: search, then open the conversation from the contact
    When I navigate to Contacts
    Then the contacts list should be displayed
    When I search for the contact in "1" of "Sheet1" of "Contacts_TestData.xlsx"
    Then the searched contact should be listed
    When I open the searched contact
    Then the chat window should be open

    # 4 - Merchants: the directory and its category filter
    When I navigate to Merchants
    Then the merchant directory should be displayed
    When I filter the merchants by the category in "1" of "Sheet1" of "Merchants_TestData.xlsx"
    Then every merchant listed should belong to that category

    # 5 - Profile: the account the run has been using is the one Settings shows
    When I open my profile from Settings
    Then the profile should show the details in "1" of "Sheet1" of "Profile_TestData.xlsx"

    # 6 - Sign out
    Then I should be able to successfully log out
