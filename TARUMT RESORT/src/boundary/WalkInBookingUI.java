package boundary;

import java.util.Scanner;
import utility.ConsoleUI;

/**
 * Boundary class for the Walk-In Registrations & Standard Booking module (team member stub).
 *
 * @author Your Name
 */
public class WalkInBookingUI {

  private final Scanner scanner = new Scanner(System.in);

  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("WALK-IN & STANDARD BOOKING MODULE");
    System.out.println("  1. Register Walk-In Guest");
    System.out.println("  2. Create Standard Booking");
    System.out.println("  3. View Booking Queue (Chronological)");
    System.out.println("  4. Process Next Booking in Queue");
    System.out.println("  5. Cancel Booking");
    System.out.println("  0. Back to Main Menu");
    System.out.print("\nEnter choice: ");
    int choice = scanner.nextInt();
    scanner.nextLine();
    return choice;
  }

  public String inputGuestName() {
    System.out.print("Enter guest name: ");
    return scanner.nextLine().trim();
  }

  public String inputContactNumber() {
    System.out.print("Enter contact number: ");
    return scanner.nextLine().trim();
  }

  public String inputRoomType() {
    System.out.print("Enter preferred room type (Standard/Deluxe/Suite): ");
    return scanner.nextLine().trim();
  }

  public int inputNumberOfNights() {
    System.out.print("Enter number of nights: ");
    int nights = scanner.nextInt();
    scanner.nextLine();
    return nights;
  }

  public String inputBookingId() {
    System.out.print("Enter booking ID: ");
    return scanner.nextLine().trim().toUpperCase();
  }

  public void displayBookingQueue(String output) {
    ConsoleUI.displaySubHeader("STANDARD BOOKING QUEUE");
    System.out.println(output.isEmpty() ? "  (Queue empty - awaiting team integration)" : output);
  }

  public void displayBookingConfirmation(String message) {
    ConsoleUI.displaySubHeader("BOOKING CONFIRMATION");
    System.out.println(message);
  }
}
