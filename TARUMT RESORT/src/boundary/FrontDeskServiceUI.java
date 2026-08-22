package boundary;

import entity.GuestRecord;
import utility.ConsoleUI;

/**
 * Boundary class for the Front-Desk Service module (team member stub).
 *
 * @author 
 */
public class FrontDeskServiceUI {

  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("FRONT-DESK SERVICE MODULE");
    System.out.println("  1. Search Guest by Confirmation No. (8-digit)");
    System.out.println("  2. Check Room Availability");
    System.out.println("  3. View Guest Billing Details");
    System.out.println("  4. List All Guest Records");
    System.out.println("  5. Generate Guests Billing Report");
    System.out.println("  6. Generate Guests Room Availability Report");
    System.out.println("  0. Back to Main Menu");
    return ConsoleUI.readMenuChoice("\nEnter choice: ");
  }

  public String inputConfirmationNumber() {
    System.out.print("Enter 8-digit confirmation number: ");
    return ConsoleUI.readLine().trim();
  }

  public String inputRoomNumber() {
    System.out.print("Enter room number: ");
    return ConsoleUI.readLine().trim().toUpperCase();
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

  public void displayReport(String title, String content) {
    ConsoleUI.displayHeader(title);
    System.out.println(content);
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
