package boundary;

import java.util.Scanner;
import utility.ConsoleUI;

/**
 * Boundary class for the VIP & Loyalty Tier Priority Room Allocation module (team member stub).
 *
 * @author Your Name
 */
public class VipLoyaltyAllocationUI {

  private final Scanner scanner = new Scanner(System.in);

  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("VIP & LOYALTY TIER ALLOCATION MODULE");
    System.out.println("  1. Add Priority Guest to Allocation Queue");
    System.out.println("  2. View Next Priority Guest (Highest Tier)");
    System.out.println("  3. Allocate Room to Priority Guest");
    System.out.println("  4. View Full Priority Queue");
    System.out.println("  0. Back to Main Menu");
    System.out.print("\nEnter choice: ");
    int choice = scanner.nextInt();
    scanner.nextLine();
    return choice;
  }

  public String inputMemberId() {
    System.out.print("Enter member ID: ");
    return scanner.nextLine().trim().toUpperCase();
  }

  public String inputGuestName() {
    System.out.print("Enter guest name: ");
    return scanner.nextLine().trim();
  }

  public int inputLoyaltyTier() {
    System.out.println("Loyalty Tiers: 1=Silver, 2=Gold, 3=Platinum, 4=Diamond, 5=Elite");
    System.out.print("Enter tier: ");
    int tier = scanner.nextInt();
    scanner.nextLine();
    return tier;
  }

  public String inputRoomNumber() {
    System.out.print("Enter room number to allocate: ");
    return scanner.nextLine().trim().toUpperCase();
  }

  public void displayPriorityQueue(String output) {
    ConsoleUI.displaySubHeader("PRIORITY ALLOCATION QUEUE");
    System.out.println(output.isEmpty() ? "  (Queue empty - awaiting team integration)" : output);
  }

  public void displayNextGuest(String details) {
    ConsoleUI.displaySubHeader("NEXT PRIORITY GUEST");
    System.out.println(details);
  }
}
