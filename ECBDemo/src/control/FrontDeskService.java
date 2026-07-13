package control;

import boundary.FrontDeskServiceUI;
import utility.MessageUI;

/**
 * Control class stub for Front-Desk Service module (team member integration point).
 *
 * @author Your Name
 */
public class FrontDeskService {

  private final FrontDeskServiceUI frontDeskUI = new FrontDeskServiceUI();

  public void runFrontDeskModule() {
    int choice;
    do {
      choice = frontDeskUI.getMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayInfoMessage("Returning to main menu...");
          break;
        case 1:
          String confirmationNo = frontDeskUI.inputConfirmationNumber();
          frontDeskUI.displaySearchResult(
              "  [Prototype] Searching for confirmation: " + confirmationNo
              + "\n  (Integrate with team's Non-Linear ADT search here)");
          MessageUI.pressEnterToContinue();
          break;
        case 2:
          String roomNo = frontDeskUI.inputRoomNumber();
          frontDeskUI.displaySearchResult(
              "  [Prototype] Checking availability for room: " + roomNo
              + "\n  (Integrate with room collection from housekeeping module)");
          MessageUI.pressEnterToContinue();
          break;
        case 3:
          frontDeskUI.displaySearchResult(
              "  [Prototype] Billing details module - to be implemented by team member.");
          MessageUI.pressEnterToContinue();
          break;
        case 4:
          frontDeskUI.displayGuestList("");
          MessageUI.pressEnterToContinue();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }
}
