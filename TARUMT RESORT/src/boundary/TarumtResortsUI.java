package boundary;

import utility.ConsoleUI;

/**
 * Main system menu for TARUMT Resorts console application.
 *
 * @author Your Name
 */
public class TarumtResortsUI {

  public void displayWelcomeBanner() {
    ConsoleUI.displayHeader("TARUMT");
    ConsoleUI.displayDetailPanel("WELCOME", "Luxury Hospitality Chain", "Select a module below to begin.");
  }

  public int getMainMenuChoice() {
    ConsoleUI.displaySubHeader("MAIN MODULE MENU");
    ConsoleUI.displayMenuOption(1, "Walk-In & Standard Booking", "Linear ADT");
    ConsoleUI.displayMenuOption(2, "VIP & Loyalty Tier Allocation", "Non-Linear ADT");
    ConsoleUI.displayMenuOption(3, "Front-Desk Service", "Search & service tools");
    ConsoleUI.displayMenuOption(4, "Housekeeping & Task Log", "Task queue & room statuses");
    System.out.println("  " + "-".repeat(72));
    ConsoleUI.displayMenuOption(0, "Exit System");
    return ConsoleUI.readMenuChoice("\nSelect an option > ");
  }
}
