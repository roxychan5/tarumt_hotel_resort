package control;

import boundary.TarumtResortsUI;
import utility.MessageUI;

/**
 * Main control class that integrates all TARUMT Resorts modules.
 */
public class TarumtResortsSystem {

  private final TarumtResortsUI mainUI = new TarumtResortsUI();
  private final HousekeepingTaskLog housekeepingModule = new HousekeepingTaskLog();
  private final FrontDeskService frontDeskModule = new FrontDeskService();
  private final VipLoyaltyAllocation vipLoyaltyModule = new VipLoyaltyAllocation();
  private final WalkInBooking walkInBookingModule = new WalkInBooking();

  public void runSystem() {
    mainUI.displayWelcomeBanner();
    int choice;
    do {
      choice = mainUI.getMainMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayExitMessage();
          break;
        case 1:
          housekeepingModule.runHousekeepingModule();
          break;
        case 2:
          frontDeskModule.runFrontDeskModule();
          break;
        case 3:
          vipLoyaltyModule.runVipLoyaltyModule();
          break;
        case 4:
          walkInBookingModule.runWalkInBookingModule();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  public static void main(String[] args) {
    TarumtResortsSystem system = new TarumtResortsSystem();
    system.runSystem();
  }
}
