package boundary;

import utility.ConsoleUI;

/** Boundary class for VIP and loyalty tier room allocation. */
public class VipLoyaltyAllocationUI {

  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("VIP & LOYALTY TIER ALLOCATION MODULE");
    System.out.println("  1. Add Priority Guest to Allocation Queue");
    System.out.println("  2. View Next Priority Guest (Highest Tier)");
    System.out.println("  3. Allocate Available Room to Next Guest");
    System.out.println("  4. View Full Priority Queue");
    System.out.println("  5. Generate Priority Waiting List Report");
    System.out.println("  6. Generate Allocation Performance Report");
    System.out.println("  0. Back to Main Menu");
    return readInt("\nEnter choice: ");
  }

  public String inputMemberId() { return readText("Enter member ID: ").toUpperCase(); }
  public String inputGuestName() { return readText("Enter guest name: "); }

  public int inputLoyaltyTier() {
    System.out.println("Loyalty Tiers: 1=Silver, 2=Gold, 3=Platinum, 4=Diamond, 5=Elite");
    return readInt("Enter tier: ");
  }

  public String inputRequestedRoomType() {
    return readText("Enter requested room type (e.g., Deluxe, Suite): ");
  }

  public String inputRoomNumber() { return readText("Enter available room number: ").toUpperCase(); }

  public int inputMinimumTier() {
    System.out.println("Minimum tier filter: 1=Silver, 2=Gold, 3=Platinum, 4=Diamond, 5=Elite");
    return readInt("Enter minimum tier: ");
  }

  public String inputRoomTypeFilter() {
    return readText("Requested room type filter (press ENTER for all): ");
  }

  public void displayPriorityQueue(String output) {
    ConsoleUI.displaySubHeader("PRIORITY ALLOCATION QUEUE");
    System.out.println(output);
  }

  public void displayNextGuest(String details) {
    ConsoleUI.displaySubHeader("NEXT PRIORITY GUEST");
    System.out.println(details);
  }

  public void displayReport(String title, String content) {
    ConsoleUI.displayHeader(title);
    System.out.println(content);
  }

  private int readInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = ConsoleUI.readLine().trim();
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException ex) {
        System.out.println("Please enter a whole number.");
      }
    }
  }

  private String readText(String prompt) {
    System.out.print(prompt);
    return ConsoleUI.readLine().trim();
  }
}
