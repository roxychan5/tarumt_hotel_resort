package control;

import adt.ArrayList;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.SearchTreeInterface;
import boundary.FrontDeskServiceUI;
import dao.FrontDeskDAO;
import dao.HousekeepingDAO;
import entity.GuestRecord;
import entity.Room;
import entity.RoomStatus;
import utility.MessageUI;

/**
 * Control class stub for Front-Desk Service module (team member integration point).
 *
 * @author Your Name
 */
public class FrontDeskService {

  private final ListInterface<GuestRecord> guestRecords = new ArrayList<>();
  private final ListInterface<Room> roomCatalogue = new ArrayList<>();
  private final SearchTreeInterface<String, GuestRecord> guestSearchTree =
      new BinarySearchTree<>();
  private final FrontDeskDAO frontDeskDAO = new FrontDeskDAO();
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  private final FrontDeskServiceUI frontDeskUI = new FrontDeskServiceUI();

  public FrontDeskService() {
    seedRoomCatalogue();
    loadData();
    if (guestRecords.isEmpty()) {
      seedSampleGuestRecords();
    }
    rebuildGuestSearchTree();
  }

  public void runFrontDeskModule() {
    int choice;
    do {
      choice = frontDeskUI.getMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayInfoMessage("Returning to main menu...");
          break;
        case 1:
          searchGuestByConfirmationNumber();
          break;
        case 2:
          checkRoomAvailability();
          break;
        case 3:
          viewBillingDetails();
          break;
        case 4:
          frontDeskUI.displayGuestList(getAllGuestRecords());
          MessageUI.pressEnterToContinue();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void searchGuestByConfirmationNumber() {
    String confirmationNumber = frontDeskUI.inputConfirmationNumber();
    if (!isValidConfirmationNumber(confirmationNumber)) {
      MessageUI.displayErrorMessage("Confirmation number must contain exactly 8 digits.");
      MessageUI.pressEnterToContinue();
      return;
    }

    GuestRecord guestRecord = guestSearchTree.search(confirmationNumber);
    if (guestRecord == null) {
      frontDeskUI.displaySearchResult("  No guest found for confirmation: "
          + confirmationNumber);
    } else {
      frontDeskUI.displayGuestDetails(guestRecord);
    }
    MessageUI.pressEnterToContinue();
  }

  private void checkRoomAvailability() {
    String roomNumber = frontDeskUI.inputRoomNumber();
    Room room = findRoom(roomNumber);
    GuestRecord assignedGuest = findGuestByRoomNumber(roomNumber);

    if (room == null && assignedGuest == null) {
      frontDeskUI.displayRoomAvailability("  Room " + roomNumber + " was not found.");
    } else if (assignedGuest != null) {
      frontDeskUI.displayRoomAvailability(
          "  Room " + assignedGuest.getRoomNumber() + " is OCCUPIED / RESERVED"
          + "\n  Guest            : " + assignedGuest.getGuestName()
          + "\n  Confirmation No. : " + assignedGuest.getConfirmationNumber()
          + "\n  Stay Period      : " + assignedGuest.getCheckInDate()
          + " to " + assignedGuest.getCheckOutDate());
    } else {
      String availability = room.getStatus() == RoomStatus.READY_FOR_CHECK_IN
          ? "AVAILABLE FOR CHECK-IN"
          : "NOT AVAILABLE";
      frontDeskUI.displayRoomAvailability(
          "  Room No. : " + room.getRoomNumber()
          + "\n  Type     : " + room.getRoomType()
          + "\n  Floor    : " + room.getFloor()
          + "\n  Status   : " + room.getStatus().getLabel()
          + "\n  Result   : " + availability);
    }
    MessageUI.pressEnterToContinue();
  }

  private void viewBillingDetails() {
    String confirmationNumber = frontDeskUI.inputConfirmationNumber();
    if (!isValidConfirmationNumber(confirmationNumber)) {
      MessageUI.displayErrorMessage("Confirmation number must contain exactly 8 digits.");
      MessageUI.pressEnterToContinue();
      return;
    }

    GuestRecord guestRecord = guestSearchTree.search(confirmationNumber);
    if (guestRecord == null) {
      frontDeskUI.displaySearchResult("  No billing record found for confirmation: "
          + confirmationNumber);
    } else {
      frontDeskUI.displayBillingDetails(guestRecord);
    }
    MessageUI.pressEnterToContinue();
  }

  private boolean isValidConfirmationNumber(String confirmationNumber) {
    return confirmationNumber.matches("\\d{8}");
  }

  private Room findRoom(String roomNumber) {
    ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        return room;
      }
    }

    for (int index = 1; index <= roomCatalogue.getNumberOfEntries(); index++) {
      Room room = roomCatalogue.getEntry(index);
      if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        return room;
      }
    }
    return null;
  }

  private GuestRecord findGuestByRoomNumber(String roomNumber) {
    for (int index = 1; index <= guestRecords.getNumberOfEntries(); index++) {
      GuestRecord guestRecord = guestRecords.getEntry(index);
      if (guestRecord.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        return guestRecord;
      }
    }
    return null;
  }

  private String getAllGuestRecords() {
    ListInterface<GuestRecord> sortedRecords = guestSearchTree.inOrderTraversal();
    StringBuilder output = new StringBuilder();
    for (int index = 1; index <= sortedRecords.getNumberOfEntries(); index++) {
      output.append(sortedRecords.getEntry(index)).append("\n");
    }
    return output.toString();
  }

  private void loadData() {
    ListInterface<GuestRecord> loadedRecords = frontDeskDAO.retrieveGuestRecords();
    guestRecords.clear();
    for (int index = 1; index <= loadedRecords.getNumberOfEntries(); index++) {
      guestRecords.add(loadedRecords.getEntry(index));
    }
  }

  private void rebuildGuestSearchTree() {
    guestSearchTree.clear();
    for (int index = 1; index <= guestRecords.getNumberOfEntries(); index++) {
      GuestRecord guestRecord = guestRecords.getEntry(index);
      guestSearchTree.insert(guestRecord.getConfirmationNumber(), guestRecord);
    }
  }

  private void seedSampleGuestRecords() {
    guestRecords.add(new GuestRecord("10234567", "Aiman Hakim", "010101-14-5678",
        "012-3456789", "R101", "Standard", "2026-08-08", "2026-08-10",
        2, 180.00, 180.00));
    guestRecords.add(new GuestRecord("23456789", "Lim Mei Xin", "A12345678",
        "016-2228899", "R201", "Deluxe", "2026-08-08", "2026-08-11",
        3, 260.00, 780.00));
    guestRecords.add(new GuestRecord("34567890", "Siti Nurhaliza", "990909-10-4321",
        "019-8887766", "R301", "Suite", "2026-08-09", "2026-08-12",
        3, 420.00, 500.00));
    guestRecords.add(new GuestRecord("45678901", "Daniel Tan", "D7654321",
        "011-12345678", "R302", "Suite", "2026-08-10", "2026-08-11",
        1, 420.00, 0.00));
    frontDeskDAO.saveGuestRecords(guestRecords);
  }

  private void seedRoomCatalogue() {
    roomCatalogue.clear();
    roomCatalogue.add(new Room("R101", "Standard", 1, RoomStatus.DIRTY));
    roomCatalogue.add(new Room("R102", "Standard", 1, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R103", "Standard", 1, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R104", "Standard", 1, RoomStatus.CLEANING_IN_PROGRESS));
    roomCatalogue.add(new Room("R201", "Deluxe", 2, RoomStatus.INSPECTED));
    roomCatalogue.add(new Room("R202", "Deluxe", 2, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R203", "Deluxe", 2, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R204", "Deluxe", 2, RoomStatus.DIRTY));
    roomCatalogue.add(new Room("R301", "Suite", 3, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R302", "Suite", 3, RoomStatus.DIRTY));
    roomCatalogue.add(new Room("R303", "Suite", 3, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R304", "Suite", 3, RoomStatus.CLEANING_IN_PROGRESS));
    roomCatalogue.add(new Room("R401", "Family", 4, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R402", "Family", 4, RoomStatus.INSPECTED));
    roomCatalogue.add(new Room("R403", "Family", 4, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R501", "Executive", 5, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R502", "Executive", 5, RoomStatus.CLEANING_IN_PROGRESS));
    roomCatalogue.add(new Room("R503", "Executive", 5, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R601", "Presidential", 6, RoomStatus.READY_FOR_CHECK_IN));
    roomCatalogue.add(new Room("R602", "Presidential", 6, RoomStatus.INSPECTED));
  }

  public static void main(String[] args) {
    FrontDeskService frontDeskService = new FrontDeskService();
    frontDeskService.runFrontDeskModule();
  }
}
