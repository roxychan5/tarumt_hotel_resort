package control;

import boundary.TarumtResortsUI;
import utility.ConsoleUI;
import utility.MessageUI;

/**
 * Main control class that integrates all TARUMT Resorts modules.
 */
public class TarumtResortsSystem {

  private final TarumtResortsUI mainUI = new TarumtResortsUI();
  private final VipLoyaltyAllocation vipLoyaltyModule = new VipLoyaltyAllocation();
  private final FrontDeskService frontDeskModule = new FrontDeskService();
  private final HousekeepingTaskLog housekeepingModule = new HousekeepingTaskLog();
  private final LoyaltyRewardsService loyaltyRewardsModule = new LoyaltyRewardsService();

  public void runSystem() {
    ConsoleUI.enableAnsiColors();
    int choice;
    do {
      choice = mainUI.getMainMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayExitMessage();
          break;
        case 1:
          vipLoyaltyModule.runVipLoyaltyModule();
          break;
        case 2:
          housekeepingModule.runHousekeepingModule();
          break;
        case 3:
          frontDeskModule.runFrontDeskModule();
          break;
        case 4:
          loyaltyRewardsModule.runLoyaltyRewardsModule();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  public static void main(String[] ar2gs) {
    TarumtResortsSystem system = new TarumtResortsSystem();
    system.runSystem();
  }
}