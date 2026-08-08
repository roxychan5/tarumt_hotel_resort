package boundary;

import entity.GuestRecord;
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

  public void displayGuestDetails(GuestRecord guestRecord) {
    ConsoleUI.displaySubHeader("COMPLETE GUEST INFORMATION");
    System.out.println(guestRecord.toFullDetailsString());
  }

  public void displayBillingDetails(GuestRecord guestRecord) {
    ConsoleUI.displaySubHeader("GUEST BILLING DETAILS");
    System.out.println(guestRecord.toBillingString());
  }

  public void displayRoomAvailability(String output) {
    ConsoleUI.displaySubHeader("ROOM AVAILABILITY");
    System.out.println(output);
  }

  public void displayGuestList(String output) {
    ConsoleUI.displaySubHeader("GUEST RECORDS");
    if (output.isEmpty()) {
      System.out.println("  (No guest records found)");
    } else {
      ConsoleUI.displayTableHeader(
          String.format("%-12s %-20s %-10s %-10s %s\n",
              "Confirm No.", "Guest Name", "Room", "Type", "Outstanding"));
      System.out.println(output);
    }
  }
}
