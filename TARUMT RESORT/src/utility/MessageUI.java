package utility;

/**
 * Common console messages and formatting helpers.
 *
 * @author Your Name
 */
public class MessageUI {

  /** Milliseconds to wait before clearing the screen after Enter. */
  private static final int CLEAR_DELAY_MS = 3000;

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
   * Waits for the user to press Enter, then shows a live countdown
   * and clears the screen once it reaches zero.
   */
  public static void pressEnterToContinue() {
    System.out.print("\n  Press ENTER to continue... ");
    ConsoleUI.readLine();
    clearWithCountdown(CLEAR_DELAY_MS / 1000);
  }

  // ── Private helpers ────────────────────────────────────────────────────

  private static void clearWithCountdown(int seconds) {
    for (int i = seconds; i > 0; i--) {
      // \r rewrites the same line; \033[2K clears the whole line first
      System.out.print("\033[2K\r  " + ConsoleUI.DIM
          + "Clearing in " + i + "s..." + ConsoleUI.RESET);
      System.out.flush();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    ConsoleUI.clearScreen();
  }
}
