Feature: UNCS settings and profile

  As a UNCS user
  I want to see my own details and the settings that govern my account
  So that I know what the product is showing about me and to whom

  # EVERYTHING HERE READS, NOTHING WRITES
  #
  # These scenarios open panels and assert on what they show. No toggle is flipped, no
  # language is changed and no session is terminated, so they are safe to run as often as
  # you like against the shared account.
  #
  # ONE CONTROL IS CHECKED BUT NEVER PRESSED
  #
  # Terminate All Other Sessions signs out every other device on the account, which would
  # include whatever the person running the suite has open. The devices scenario asserts
  # the button is there and stops.
  #
  # THE PANELS SHARE ONE DRAWER
  #
  # Block List, Devices and Language slide over the profile inside the same drawer rather
  # than moving to their own addresses, so nothing in the URL says which one is open. Each
  # is identified by its heading, and the unlabelled chevron in the top left goes back.

  Background:
    Given I am on the UNCS login page
    When I log into UNCS with the email credentials in "1" of "Sheet1" of "Login_TestData.xlsx"
    Then I should land on the UNCS home page

  @settings
  Scenario: The profile shows the signed in account and its privacy toggles
    When I open my profile from Settings
    Then the profile should show the details in "1" of "Sheet1" of "Profile_TestData.xlsx"
    And the profile should offer the privacy toggles

  @settings
  Scenario: The blocked list starts empty
    When I open my profile from Settings
    And I open the "Block List" settings panel
    Then the blocked list should be empty

  @settings
  Scenario: The devices panel names this device
    When I open my profile from Settings
    And I open the "Devices" settings panel
    Then the devices panel should list this device and offer to terminate the others

  @settings
  Scenario: The account language can be read back
    When I open my profile from Settings
    And I open the "Language" settings panel
    Then the language panel should show the language in "1" of "Sheet1" of "Profile_TestData.xlsx"
    When I go back from the settings panel
    Then the profile should show the details in "1" of "Sheet1" of "Profile_TestData.xlsx"
