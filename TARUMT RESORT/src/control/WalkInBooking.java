package control;

import adt.ArrayList;
import boundary.WalkInBookingUI;
import entity.StandardBooking;
import entity.WalkInGuest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import utility.ConsoleUI;
import utility.MessageUI;

/**
 * Control class for Walk-In Registrations & Standard Booking.
 *
 * @author Your Name
 */
public class WalkInBooking {

    private final WalkInBookingUI bookingUI = new WalkInBookingUI();

    /*
     * Linear ADT for Standard Booking.
     *
     * New bookings are added to the end.
     * The first booking is processed first.
     */
    private final ArrayList<StandardBooking> bookingList
            = new ArrayList<>();

    /*
     * Stores walk-in guest registrations.
     */
    private final ArrayList<WalkInGuest> walkInList
            = new ArrayList<>();

    private static final String BOOKING_FILE
            = "standard_bookings.txt";

    private static final String WALK_IN_FILE
            = "walk_in_guests.txt";

    private int nextBookingNumber = 1;
    private int nextWalkInNumber = 1;

    /**
     * Runs the Walk-In and Standard Booking module.
     */
    public void runWalkInBookingModule() {

        loadBookingsFromFile();
        loadWalkInsFromFile();

        int choice;

        do {

            choice = bookingUI.getMenuChoice();

            switch (choice) {

                case 0:
                    MessageUI.displayInfoMessage(
                            "Returning to main menu...");
                    break;

                case 1:
                    registerWalkInGuest();
                    break;

                case 2:
                    createStandardBooking();
                    break;

                case 3:
                    displayBookingQueue();
                    break;

                case 4:
                    processNextBooking();
                    break;

                case 5:
                    cancelBooking();
                    break;

                default:
                    MessageUI.displayInvalidChoiceMessage();
            }

        } while (choice != 0);
    }

    /**
     * Registers a walk-in guest.
     */
    private void registerWalkInGuest() {

        System.out.println("\n--- Register Walk-In Guest ---");

        String walkInId = generateWalkInId();

        String guestName = bookingUI.inputGuestName();

        String contactNumber = bookingUI.inputContactNumber();

        String roomType = bookingUI.inputRoomType();

        int numberOfNights = bookingUI.inputNumberOfNights();

        WalkInGuest guest = new WalkInGuest(
                walkInId,
                guestName,
                contactNumber,
                roomType,
                numberOfNights
        );

        walkInList.add(guest);

        saveWalkInsToFile();

        bookingUI.displayWalkInConfirmation(guest);

        MessageUI.pressEnterToContinue();
    }

    /**
     * Creates a standard booking.
     */
    private void createStandardBooking() {

        System.out.println("\n--- Create Standard Booking ---");

        String bookingId = generateBookingId();

        String guestName = bookingUI.inputGuestName();

        String contactNumber = bookingUI.inputContactNumber();

        String roomType = bookingUI.inputRoomType();

        int numberOfNights = bookingUI.inputNumberOfNights();

        String status = "Confirmed";

        StandardBooking booking = new StandardBooking(
                bookingId,
                guestName,
                contactNumber,
                roomType,
                numberOfNights,
                status
        );

        /*
         * Add new booking to the end of the Linear ADT.
         * This maintains chronological order.
         */
        bookingList.add(booking);

        saveBookingsToFile();

        bookingUI.displayBookingConfirmation(
                "Standard booking created successfully.\n\n"
                + booking
        );

        MessageUI.pressEnterToContinue();
    }

    /**
     * Displays standard bookings in chronological order.
     */
    private void displayBookingQueue() {

        ConsoleUI.displaySubHeader("STANDARD BOOKING QUEUE");

        if (bookingList.isEmpty()) {
            System.out.println("No standard bookings in queue.");
            MessageUI.pressEnterToContinue();
            return;
        }

        System.out.println(
                "-----------------------------------------------------------------------------------------------"
        );

        System.out.printf(
                "%-5s %-12s %-18s %-14s %-12s %-8s %-12s%n",
                "No.",
                "Booking ID",
                "Guest Name",
                "Contact",
                "Room Type",
                "Nights",
                "Status"
        );

        System.out.println(
                "-----------------------------------------------------------------------------------------------"
        );

        for (int i = 1;
                i <= bookingList.getNumberOfEntries();
                i++) {

            StandardBooking booking =
                    bookingList.getEntry(i);

            System.out.printf(
                    "%-5d %-12s %-18s %-14s %-12s %-8d %-12s%n",
                    i,
                    booking.getBookingId(),
                    booking.getGuestName(),
                    booking.getContactNumber(),
                    booking.getRoomType(),
                    booking.getNumberOfNights(),
                    booking.getStatus()
            );
        }

        System.out.println(
                "-----------------------------------------------------------------------------------------------"
        );

        System.out.println(
                "Total bookings: "
                + bookingList.getNumberOfEntries()
        );

        MessageUI.pressEnterToContinue();
    }

    /**
     * Processes the first booking in the Linear ADT.
     *
     * FIFO:
     * First In, First Out.
     */
    private void processNextBooking() {

        if (bookingList.isEmpty()) {

            MessageUI.displayInfoMessage(
                    "No standard booking available to process.");

        } else {

            /*
             * Position 1 represents the first booking.
             */
            StandardBooking booking = bookingList.remove(1);

            booking.setStatus("Processed");

            saveBookingsToFile();

            MessageUI.displaySuccessMessage(
                    "Next booking processed successfully.\n\n"
                    + booking
            );
        }

        MessageUI.pressEnterToContinue();
    }

    /**
     * Cancels a booking using its booking ID.
     */
    private void cancelBooking() {

        if (bookingList.isEmpty()) {

            MessageUI.displayInfoMessage(
                    "No standard booking available to cancel.");

            MessageUI.pressEnterToContinue();

            return;
        }

        String bookingId = bookingUI.inputBookingId();

        boolean found = false;

        for (int position = 1;
                position <= bookingList.getNumberOfEntries();
                position++) {

            StandardBooking booking
                    = bookingList.getEntry(position);

            if (booking.getBookingId()
                    .equalsIgnoreCase(bookingId)) {

                bookingList.remove(position);

                saveBookingsToFile();

                MessageUI.displaySuccessMessage(
                        "Booking " + bookingId
                        + " cancelled successfully.");

                found = true;

                break;
            }
        }

        if (!found) {

            MessageUI.displayInfoMessage(
                    "Booking ID " + bookingId + " was not found.");
        }

        MessageUI.pressEnterToContinue();
    }

    /**
     * Generates the next booking ID.
     *
     * @return booking ID
     */
    private String generateBookingId() {

        String bookingId = String.format(
                "B%03d",
                nextBookingNumber
        );

        nextBookingNumber++;

        return bookingId;
    }

    /**
     * Generates the next walk-in ID.
     *
     * @return walk-in ID
     */
    private String generateWalkInId() {

        String walkInId = String.format(
                "W%03d",
                nextWalkInNumber
        );

        nextWalkInNumber++;

        return walkInId;
    }

    /**
     * Saves all standard bookings to a text file.
     */
    private void saveBookingsToFile() {

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(BOOKING_FILE))) {

            for (int i = 1;
                    i <= bookingList.getNumberOfEntries();
                    i++) {

                StandardBooking booking
                        = bookingList.getEntry(i);

                writer.write(
                        booking.getBookingId() + "|"
                        + booking.getGuestName() + "|"
                        + booking.getContactNumber() + "|"
                        + booking.getRoomType() + "|"
                        + booking.getNumberOfNights() + "|"
                        + booking.getStatus()
                );

                writer.newLine();
            }

        } catch (IOException e) {

            MessageUI.displayInfoMessage(
                    "Unable to save standard booking data: "
                    + e.getMessage());
        }
    }

    /**
     * Loads standard bookings from the text file.
     */
    private void loadBookingsFromFile() {

        File file = new File(BOOKING_FILE);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split("\\|");

                if (data.length == 6) {

                    try {

                        String bookingId = data[0];
                        String guestName = data[1];
                        String contactNumber = data[2];
                        String roomType = data[3];
                        int numberOfNights = Integer.parseInt(data[4]);
                        String status = data[5];

                        StandardBooking booking
                                = new StandardBooking(
                                        bookingId,
                                        guestName,
                                        contactNumber,
                                        roomType,
                                        numberOfNights,
                                        status
                                );

                        bookingList.add(booking);

                        updateNextBookingNumber(bookingId);

                    } catch (NumberFormatException e) {

                        System.out.println(
                                "Skipped invalid booking record.");
                    }
                }
            }

        } catch (IOException e) {

            MessageUI.displayInfoMessage(
                    "Unable to load standard booking data: "
                    + e.getMessage());
        }
    }

    /**
     * Saves all walk-in guests to a text file.
     */
    private void saveWalkInsToFile() {

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(WALK_IN_FILE))) {

            for (int i = 1;
                    i <= walkInList.getNumberOfEntries();
                    i++) {

                WalkInGuest guest
                        = walkInList.getEntry(i);

                writer.write(
                        guest.getWalkInId() + "|"
                        + guest.getGuestName() + "|"
                        + guest.getContactNumber() + "|"
                        + guest.getRoomType() + "|"
                        + guest.getNumberOfNights()
                );

                writer.newLine();
            }

        } catch (IOException e) {

            MessageUI.displayInfoMessage(
                    "Unable to save walk-in data: "
                    + e.getMessage());
        }
    }

    /**
     * Loads walk-in guests from the text file.
     */
    private void loadWalkInsFromFile() {

        File file = new File(WALK_IN_FILE);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split("\\|");

                if (data.length == 5) {

                    int numberOfNights = Integer.parseInt(data[4]);

                    WalkInGuest guest = new WalkInGuest(
                            data[0],
                            data[1],
                            data[2],
                            data[3],
                            numberOfNights
                    );

                    walkInList.add(guest);

                    updateNextWalkInNumber(data[0]);
                }
            }

        } catch (IOException e) {

            MessageUI.displayInfoMessage(
                    "Unable to load walk-in data: "
                    + e.getMessage());
        }
    }

    /**
     * Updates the next booking number after loading saved data.
     *
     * @param bookingId existing booking ID
     */
    private void updateNextBookingNumber(String bookingId) {

        try {

            if (bookingId.startsWith("B")) {

                int number = Integer.parseInt(
                        bookingId.substring(1));

                if (number >= nextBookingNumber) {
                    nextBookingNumber = number + 1;
                }
            }

        } catch (NumberFormatException e) {
            // Ignore invalid ID.
        }
    }

    /**
     * Updates the next walk-in number after loading saved data.
     *
     * @param walkInId existing walk-in ID
     */
    private void updateNextWalkInNumber(String walkInId) {

        try {

            if (walkInId.startsWith("W")) {

                int number = Integer.parseInt(
                        walkInId.substring(1));

                if (number >= nextWalkInNumber) {
                    nextWalkInNumber = number + 1;
                }
            }

        } catch (NumberFormatException e) {
            // Ignore invalid ID.
        }
    }
}