package utility;

/**
 * Common console messages and formatting helpers.
 *
 * @author Your Name
 */
public class MessageUI {

  public static void displayInvalidChoiceMessage() {
    displayErrorMessage("Invalid choice. Please select an option shown in the menu.");
  }

  public static void displayExitMessage() {
    ConsoleUI.displayDetailPanel("SESSION ENDED",
        "Thank you for using TARUMT Resorts Management System.", "Goodbye!");
  }

  public static void displaySuccessMessage(String message) {
    System.out.println();
    System.out.println("  " + ConsoleUI.BOLD + "\u001B[42m\u001B[97m"
        + "  [OK] " + message + "  " + ConsoleUI.RESET);
  }

  public static void displayErrorMessage(String message) {
    System.out.println();
    System.out.println("  " + ConsoleUI.BOLD + "\u001B[41m\u001B[97m"
        + "  [!!] " + message + "  " + ConsoleUI.RESET);
  }

  public static void displayInfoMessage(String message) {
    System.out.println();
    System.out.println("  " + ConsoleUI.BOLD + "\u001B[44m\u001B[97m"
        + "  [i] " + message + "  " + ConsoleUI.RESET);
  }

  /**
   * Waits for the user to press Enter, then clears the screen immediately.
   */
  public static void pressEnterToContinue() {
    System.out.print("\n  Press ENTER to continue... ");
    ConsoleUI.readLine();
    ConsoleUI.clearScreen();
  }

  // ── Private helpers ────────────────────────────────────────────────────

}
