Feature: UNCS chats

  As a UNCS user
  I want to message a contact and manage what I have sent
  So that a conversation behaves the way a messenger is expected to

  # THESE SCENARIOS WRITE INTO A REAL CONVERSATION
  #
  # Every message sent here goes to the contact named in Chats_TestData.xlsx on the test
  # environment. Each scenario deletes what it sent before it finishes, so a completed run
  # leaves only the "Message was deleted!" placeholders behind rather than a growing pile
  # of automation chatter. A scenario that fails part way through can leave a message in
  # place - that is the trade for testing the real thing.
  #
  # The text is stamped with the time it was sent. That makes a run obvious to a human
  # reading the thread later, and it stops an assertion from passing on a bubble that an
  # earlier run left behind.
  #
  # THE ACTION MENU IS A RIGHT CLICK
  #
  # Neither a chat row nor a message has a three dot button or a hover affordance. The
  # only way into either menu is a context click, which is why the page objects use
  # Actions.contextClick rather than a plain click.
  #
  # WHAT IS DELIBERATELY ABSENT
  #
  # The two buttons in the conversation header place a voice call, and they carry no
  # label, title or aria-label to tell them apart from anything else. They are left out
  # entirely - an unattended suite must never be able to ring a real person.

  Background:
    Given I am on the UNCS login page
    When I log into UNCS with the email credentials in "1" of "Sheet1" of "Login_TestData.xlsx"
    Then I should land on the UNCS home page

  @chats
  Scenario: A message is sent and then removed again
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the chat window should be open
    And the Send button should be disabled while the composer is empty
    When I send the message in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the message should appear in the chat thread
    When I delete the message
    Then the message should be replaced by the deleted placeholder

  @chats
  Scenario: A sent message can be reacted to
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I send the message in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the message should appear in the chat thread
    When I react to the message with the emoji in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the reaction in "1" of "Sheet1" of "Chats_TestData.xlsx" should be shown on the message
    When I delete the message
    Then the message should be replaced by the deleted placeholder

  @chats
  Scenario: A reply quotes the message it answers
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I send the message in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the message should appear in the chat thread
    When I start a reply to the message
    Then the composer should quote the message being replied to
    When I send the reply
    Then the reply should carry the quoted message
    When I delete the reply
    And I delete the message
    Then the message should be replaced by the deleted placeholder

  @chats
  Scenario: A sent message can be edited and is marked as such
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I send the message in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the message should appear in the chat thread
    When I edit the message with the suffix in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the message should be marked as edited
    When I delete the message
    Then the message should be replaced by the deleted placeholder

  # THESE THREE WERE ONE SCENARIO ONCE
  #
  # Searching, reading the menu and pinning used to run as a single scenario, and each part
  # handed the next one an open menu and a changed chat. It failed on six consecutive runs
  # for six different reasons, none of which were the product: a menu left open by the step
  # before, a stale menu read as if it were fresh, a chat still pinned by a run that died
  # halfway. Splitting them means each one sets up what it needs and nothing carries over.
  #
  # The pin check also stopped asking the menu whether the chat is pinned and started
  # reading the badge in the chat's own row instead. Asking the menu means closing one,
  # opening another and trusting the answer came from the new one; the row just says.

  @chats
  Scenario: The chat list can be searched
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the chat window should be open
    When I search the chat list for the chat name
    Then the chat list should show the matching chat

  @chats
  Scenario: A chat menu offers every action
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I open the chat menu for that chat
    Then the chat menu should offer Star, Mute, Pin, Block User, Report and Delete

  @chats
  Scenario: A chat can be pinned and unpinned
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And the chat starts out unpinned
    And I pin that chat
    Then the chat should show as pinned
    When I unpin that chat
    Then the chat should no longer show as pinned

  @chats
  Scenario: A chat can be starred and unstarred
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I star that chat
    Then the chat should show as starred
    When I unstar that chat
    Then the chat should no longer show as starred

  # Muting is two steps: choosing Mute opens a chooser - one day, one week, one month, or
  # until I change it - and nothing is muted until Done is pressed. Unmuting is immediate.
  @chats
  Scenario: A chat can be muted and unmuted
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I mute that chat for the duration in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the chat should show as muted
    When I unmute that chat
    Then the chat should no longer show as muted

  # BLOCKING IS AIMED SOMEWHERE ELSE ON PURPOSE
  #
  # This uses the BlockChatName column, never the ChatName the rest of the file works with.
  # A blocked contact cannot be messaged, so pointing this at the conversation every other
  # scenario depends on would take the whole suite down for as long as the block stood.
  #
  # Block takes effect the instant the menu item is clicked - there is no confirmation,
  # unlike Delete on the same menu - so the block and the unblock are one scenario, and it
  # starts by clearing anything a previous failed run left behind.
  @chats
  Scenario: A user can be blocked and unblocked again
    Given nobody is blocked
    When I block the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the blocked list should contain that chat
    When I unblock that chat from the blocked list
    Then the blocked list should be empty again

  @chats
  Scenario: Reporting a user offers every reason and can be closed
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I open the report dialog for that chat
    Then the report dialog should offer every reason
    When I close the report dialog without submitting
    Then the chat should still be listed

  # THIS ONE FILES A REAL REPORT
  #
  # It goes all the way through: reason, description, attachment, Submit. A submitted
  # report lands in the moderation queue and there is no way to withdraw it, so this is
  # tagged separately and left out of every unattended run. Launch it deliberately:
  #
  #   mvn test -Dcucumber.filter.tags="@report"
  #
  # Two things keep the damage contained. It is filed against the BlockChatName account,
  # the one the suite already sets aside for actions it cannot take back, never the
  # conversation the rest of the file depends on. And the description is stamped with the
  # time it was sent, so anyone reading the queue can tell an automated report from a real
  # one at a glance.
  #
  # SUCCESS IS NOT A TOAST
  #
  # Submitting does not close the dialog and raises no toast. The dialog swaps its own
  # contents for a "Report Submitted" panel with a Done button. Waiting for a toast, or for
  # the dialog to disappear, times out on a submission that worked - which is exactly what
  # happened the first time this was driven by hand.
  @report
  Scenario: A report is filed with a reason, a description and an attachment
    When I open the report dialog for the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the report dialog should offer every reason
    When I choose the report reason in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I describe the report using "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I attach the file in "1" of "Sheet1" of "Chats_TestData.xlsx"
    Then the attachment should be accepted
    When I submit the report
    Then the report should be confirmed as submitted
    When I dismiss the report confirmation
    Then I should see the chat list

  # THE CHAT IS NEVER ACTUALLY DELETED
  #
  # Every other scenario in this file talks to this conversation, so confirming would take
  # the rest of the suite with it, and a deleted chat is not something a run can put back.
  # What is worth testing is that the product asks first - and it does. The page object has
  # no method that answers this confirmation with Delete.
  @chats
  Scenario: Deleting a chat asks for confirmation and can be cancelled
    When I open the chat named in "1" of "Sheet1" of "Chats_TestData.xlsx"
    And I open the delete confirmation for that chat
    Then the delete confirmation should ask before removing the chat
    When I cancel the delete confirmation
    Then the chat should still be listed
