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
    ConsoleUI.displayDetailPanel("SESSION ENDED", "Thank you for using TARUMT Resorts Management System.", "Goodbye!");
  }

  public static void displaySuccessMessage(String message) {
    System.out.println("\n[OK] " + message);
  }

  public static void displayErrorMessage(String message) {
    System.out.println("\n[ERROR] " + message);
  }

  public static void displayInfoMessage(String message) {
    System.out.println("\n[INFO] " + message);
  }

  public static void pressEnterToContinue() {
    System.out.print("\nPress ENTER to continue... ");
    ConsoleUI.readLine();
  }
}
