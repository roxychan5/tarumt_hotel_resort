package utility;

/**
 * Common console messages and formatting helpers.
 *
 * @author Your Name
 */
public class MessageUI {

  public static void displayInvalidChoiceMessage() {
    System.out.println("\n*** Invalid choice. Please try again. ***");
  }

  public static void displayExitMessage() {
    System.out.println("\nThank you for using TARUMT Resorts System. Goodbye!");
  }

  public static void displaySuccessMessage(String message) {
    System.out.println("\n>> " + message);
  }

  public static void displayErrorMessage(String message) {
    System.out.println("\n!! " + message);
  }

  public static void displayInfoMessage(String message) {
    System.out.println("\n-- " + message);
  }

  public static void pressEnterToContinue() {
    System.out.print("\nPress ENTER to continue...");
    try {
      System.in.read();
      while (System.in.available() > 0) {
        System.in.read();
      }
    } catch (Exception ex) {
      // Ignore input errors in prototype
    }
  }
}
