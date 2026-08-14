package boundary;

import java.util.Scanner;

import entity.WalkInGuest;
import utility.ConsoleUI;

/**
 * Boundary class for the Walk-In Registrations & Standard Booking module.
 *
 * @author Your Name
 */
public class WalkInBookingUI {

    private final Scanner scanner = new Scanner(System.in);

    /**
     * Displays the main module menu.
     *
     * @return valid menu choice
     */
    public int getMenuChoice() {

        ConsoleUI.displaySubHeader("WALK-IN & STANDARD BOOKING MODULE");

        System.out.println("  1. Register Walk-In Guest");
        System.out.println("  2. Create Standard Booking");
        System.out.println("  3. View Booking Queue (Chronological)");
        System.out.println("  4. Process Next Booking in Queue");
        System.out.println("  5. Cancel Booking");
        System.out.println("  0. Back to Main Menu");

        while (true) {

            System.out.print("\nEnter choice: ");

            String input = scanner.nextLine().trim();

            try {

                int choice = Integer.parseInt(input);

                if (choice >= 0 && choice <= 5) {
                    return choice;
                }

                System.out.println(
                        "Invalid choice. Please enter a number from 0 to 5.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a number from 0 to 5.");
            }
        }
    }

    /**
     * Gets guest name.
     *
     * @return valid guest name
     */
    public String inputGuestName() {

        while (true) {

            System.out.print("Enter guest name: ");

            String name = scanner.nextLine().trim();

            if (!name.isEmpty() && name.matches("[a-zA-Z ]+")) {
                return name;
            }

            System.out.println(
                    "Invalid name. Please enter letters and spaces only.");
        }
    }

    /**
     * Gets contact number.
     *
     * @return valid contact number
     */
    public String inputContactNumber() {

        while (true) {

            System.out.print("Enter contact number: ");

            String contactNumber = scanner.nextLine().trim();

            if (contactNumber.matches("01\\d{8,9}")) {
                return contactNumber;
            }

            System.out.println(
                    "Invalid contact number. Please enter a valid Malaysian "
                    + "phone number starting with 01.");
        }
    }

    /**
     * Displays room types and gets user's selection.
     *
     * @return selected room type
     */
    public String inputRoomType() {

        System.out.println("\nSelect Room Type:");
        System.out.println("  1. Standard");
        System.out.println("  2. Deluxe");
        System.out.println("  3. Suite");

        while (true) {

            System.out.print("Enter room type choice: ");

            String input = scanner.nextLine().trim();

            try {

                int choice = Integer.parseInt(input);

                switch (choice) {

                    case 1:
                        return "Standard";

                    case 2:
                        return "Deluxe";

                    case 3:
                        return "Suite";

                    default:
                        System.out.println(
                                "Invalid choice. Please select 1, 2, or 3.");
                }

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter 1, 2, or 3.");
            }
        }
    }

    /**
     * Gets number of nights.
     *
     * @return valid number of nights
     */
    public int inputNumberOfNights() {

        while (true) {

            System.out.print("Enter number of nights: ");

            String input = scanner.nextLine().trim();

            try {

                int nights = Integer.parseInt(input);

                if (nights > 0 && nights <= 365) {
                    return nights;
                }

                System.out.println(
                        "Number of nights must be between 1 and 365.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a whole number.");
            }
        }
    }

    /**
     * Gets booking ID.
     *
     * @return booking ID
     */
    public String inputBookingId() {

        while (true) {

            System.out.print("Enter booking ID: ");

            String bookingId = scanner.nextLine().trim().toUpperCase();

            if (bookingId.matches("B\\d{3,}")) {
                return bookingId;
            }

            System.out.println(
                    "Invalid booking ID. Example: B001.");
        }
    }

    /**
     * Displays booking queue.
     *
     * @param output queue output
     */
    public void displayBookingQueue(String output) {

        ConsoleUI.displaySubHeader("STANDARD BOOKING QUEUE");

        if (output == null || output.isEmpty()) {
            System.out.println("  No standard bookings in queue.");
        } else {
            System.out.println(output);
        }
    }

    /**
     * Displays walk-in guest list.
     *
     * @param output walk-in guest output
     */
    public void displayWalkInGuest(String output) {

        ConsoleUI.displaySubHeader("WALK-IN GUEST REGISTRATION");

        if (output == null || output.isEmpty()) {
            System.out.println("  No walk-in guests registered.");
        } else {
            System.out.println(output);
        }
    }

    /**
     * Displays booking confirmation.
     *
     * @param message confirmation message
     */
    public void displayBookingConfirmation(String message) {

        ConsoleUI.displaySubHeader("BOOKING CONFIRMATION");

        System.out.println(message);
    }

    /**
     * Displays walk-in registration confirmation.
     *
     * @param message confirmation message
     */
    public void displayWalkInConfirmation(WalkInGuest guest) {

        ConsoleUI.displaySubHeader(
                "WALK-IN REGISTRATION CONFIRMATION"
        );

        System.out.println(
                "Walk-in guest registered successfully.\n"
        );

        System.out.println(
                "----------------------------------------------------------------------"
        );

        System.out.printf(
                "%-8s %-18s %-18s %-12s %-8s%n",
                "ID",
                "Guest Name",
                "Contact Number",
                "Room Type",
                "Nights"
        );

        System.out.println(
                "----------------------------------------------------------------------"
        );

        System.out.printf(
                "%-8s %-18s %-18s %-12s %-8d%n",
                guest.getWalkInId(),
                guest.getGuestName(),
                guest.getContactNumber(),
                guest.getRoomType(),
                guest.getNumberOfNights()
        );

        System.out.println(
                "----------------------------------------------------------------------"
        );
    }
}