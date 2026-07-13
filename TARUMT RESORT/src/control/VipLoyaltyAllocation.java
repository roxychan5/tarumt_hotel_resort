package control;

import boundary.VipLoyaltyAllocationUI;
import utility.MessageUI;

/**
 * Control class stub for VIP & Loyalty Tier Priority Room Allocation (team member integration point).
 *
 * @author Your Name
 */
public class VipLoyaltyAllocation {

  private final VipLoyaltyAllocationUI vipUI = new VipLoyaltyAllocationUI();

  public void runVipLoyaltyModule() {
    int choice;
    do {
      choice = vipUI.getMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayInfoMessage("Returning to main menu...");
          break;
        case 1:
          vipUI.inputMemberId();
          vipUI.inputGuestName();
          vipUI.inputLoyaltyTier();
          MessageUI.displaySuccessMessage(
              "[Prototype] Priority guest added. (Integrate with team's priority queue/tree ADT)");
          MessageUI.pressEnterToContinue();
          break;
        case 2:
          vipUI.displayNextGuest(
              "  [Prototype] Highest tier guest will appear here after team integration.");
          MessageUI.pressEnterToContinue();
          break;
        case 3:
          vipUI.inputRoomNumber();
          MessageUI.displaySuccessMessage(
              "[Prototype] Room allocation - to be implemented by team member.");
          MessageUI.pressEnterToContinue();
          break;
        case 4:
          vipUI.displayPriorityQueue("");
          MessageUI.pressEnterToContinue();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }
}
