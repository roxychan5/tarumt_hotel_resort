package boundary;

import java.util.Scanner;
import utility.ConsoleUI;

/**
 * Boundary class for the Front-Desk Service module (team member stub).
 *
 * @author 
 */
public class FrontDeskServiceUI {

  private final Scanner scanner = new Scanner(System.in);

  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("FRONT-DESK SERVICE MODULE");
    System.out.println("  1. Search Guest by Confirmation No. (8-digit)");
    System.out.println("  2. Check Room Availability");
    System.out.println("  3. View Guest Billing Details");
    System.out.println("  4. List All Guest Records");
    System.out.println("  0. Back to Main Menu");
    System.out.print("\nEnter choice: ");
    int choice = scanner.nextInt();
    scanner.nextLine();
    return choice;
  }

  public String inputConfirmationNumber() {
    System.out.print("Enter 8-digit confirmation number: ");
    return scanner.nextLine().trim();
  }

  public String inputRoomNumber() {
    System.out.print("Enter room number: ");
    return scanner.nextLine().trim().toUpperCase();
  }

  public void displaySearchResult(String result) {
    ConsoleUI.displaySubHeader("GUEST SEARCH RESULT");
    System.out.println(result);
  }

  public void displayGuestList(String output) {
    ConsoleUI.displaySubHeader("GUEST RECORDS");
    System.out.println(output.isEmpty() ? "  (No records - awaiting team integration)" : output);
  }
}
