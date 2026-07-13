package boundary;

import java.util.Scanner;
import utility.ConsoleUI;

/**
 * Main system menu for TARUMT Resorts console application.
 *
 * @author Your Name
 */
public class TarumtResortsUI {

  private final Scanner scanner = new Scanner(System.in);

  public void displayWelcomeBanner() {
    ConsoleUI.displayHeader("TARUMT RESORTS MANAGEMENT SYSTEM");
    System.out.println("  Luxury Hospitality Chain - Console Prototype");
    System.out.println("  BMCS2063 Data Structures & Algorithms Assignment");
  }

  public int getMainMenuChoice() {
    ConsoleUI.displaySubHeader("MAIN MODULE MENU");
    System.out.println("  1. Walk-In & Standard Booking       [Linear ADT]");
    System.out.println("  2. VIP & Loyalty Tier Allocation    [Non-Linear ADT]");
    System.out.println("  3. Front-Desk Service               [Non-Linear ADT & Searching]");
    System.out.println("  4. Housekeeping & Task Log          [Linear ADT]");
    System.out.println("  0. Exit System");
    System.out.print("\nEnter choice: ");
    int choice = scanner.nextInt();
    scanner.nextLine();
    return choice;
  }
}
