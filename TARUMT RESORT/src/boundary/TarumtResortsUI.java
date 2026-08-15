package boundary;

import utility.ConsoleUI;

/**
 * Main system menu for TARUMT Resorts console application.
 *
 * @author Your Name
 */
public class TarumtResortsUI {

  private static final String[] MAIN_MENU_OPTIONS = {
      "VIP & Loyalty Tier Priority Room Allocation",
      "Housekeeping & Task Log",
      "Front-Desk Service",
      "Loyalty & Rewards Service"
  };

  public void displayMainMenuScreen() {
    ConsoleUI.clearScreen();
    ConsoleUI.displayGlitchBanner();
    ConsoleUI.displayNeonMenuBox("TARUMT RESORTS SYSTEM", MAIN_MENU_OPTIONS, "Exit");
  }

  public int getMainMenuChoice() {
    displayMainMenuScreen();
    return ConsoleUI.readMenuChoice(
        ConsoleUI.centeredPrompt(ConsoleUI.SKY_BLUE + "choice (0-4): " + ConsoleUI.RESET));
  }
}
