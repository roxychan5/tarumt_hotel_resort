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
import utility.PdfReportEngine;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Control class stub for Front-Desk Service module (team member integration point).
 *
 * @author Your Name
 */
public class FrontDeskService {

  private final ListInterface<GuestRecord> guestRecords = new ArrayList<>();
  private final SearchTreeInterface<String, GuestRecord> guestSearchTree =
      new BinarySearchTree<>(); //non-linear data structure for front desk 
  private final FrontDeskDAO frontDeskDAO = new FrontDeskDAO();
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  private final FrontDeskServiceUI frontDeskUI = new FrontDeskServiceUI();

  public FrontDeskService() {
    loadData();
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
        case 5:
          guestsBillingReport();
          break;
        case 6:
          guestsRoomAvailabilityReport();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void searchGuestByConfirmationNumber() {
    GuestRecord guestRecord = promptForGuestRecord(false);
    if (guestRecord == null) return;
    frontDeskUI.displayGuestDetails(guestRecord);
    MessageUI.pressEnterToContinue();
  }

  private void checkRoomAvailability() {
    while (true) {
      String roomNumber = frontDeskUI.inputRoomNumber();
      if (roomNumber.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Room availability check cancelled.");
        return;
      }

      if (!isValidRoomNumber(roomNumber)) {
        frontDeskUI.displayRoomAvailability(
            "  Room number must be R followed by 3-4 digits, for example R101.\n"
            + "  Enter a valid room number, or enter 0 to cancel.");
        continue;
      }

      Room room = findRoom(roomNumber);
      GuestRecord assignedGuest = findGuestByRoomNumber(roomNumber);

      if (room == null && assignedGuest == null) {
        frontDeskUI.displayRoomAvailability(
            "  Room " + roomNumber + " was not found.\n"
            + "  Try again, or enter 0 to cancel.");
        continue;
      }

      frontDeskUI.displayRoomAvailability(formatRoomAvailability(room, assignedGuest));
      MessageUI.pressEnterToContinue();
      return;
    }
  }

  private void viewBillingDetails() {
    GuestRecord guestRecord = promptForGuestRecord(true);
    if (guestRecord == null) return;
    frontDeskUI.displayBillingDetails(guestRecord);
    MessageUI.pressEnterToContinue();
  }

  private void guestsBillingReport() {
    ListInterface<GuestRecord> sortedRecords = guestSearchTree.inOrderTraversal();
    StringBuilder report = new StringBuilder();
    report.append(String.format("%-12s %-20s %-8s %-10s %12s %12s %12s%n",
        "Confirm No.", "Guest Name", "Room", "Type", "Total", "Paid", "Outstanding"));
    report.append("------------------------------------------------------------------------------------------\n");

    int guestCount = 0;
    double totalAmount = 0;
    double totalPaid = 0;
    double totalOutstanding = 0;

    for (int index = 1; index <= sortedRecords.getNumberOfEntries(); index++) {
      GuestRecord guestRecord = sortedRecords.getEntry(index);
      report.append(String.format("%-12s %-20s %-8s %-10s RM %9.2f RM %9.2f RM %9.2f%n",
          guestRecord.getConfirmationNumber(), guestRecord.getGuestName(),
          guestRecord.getRoomNumber(), guestRecord.getRoomType(),
          guestRecord.getTotalAmount(), guestRecord.getPaidAmount(),
          guestRecord.getOutstandingAmount()));
      guestCount++;
      totalAmount += guestRecord.getTotalAmount();
      totalPaid += guestRecord.getPaidAmount();
      totalOutstanding += guestRecord.getOutstandingAmount();
    }

    if (guestCount == 0) {
      report.append("No guest billing records found.\n");
    }
    report.append("\nTotal guests                 : ").append(guestCount).append("\n");
    report.append(String.format("Total bill amount            : RM %.2f%n", totalAmount));
    report.append(String.format("Total paid amount            : RM %.2f%n", totalPaid));
    report.append(String.format("Total outstanding amount     : RM %.2f%n", totalOutstanding));

    frontDeskUI.displayReport("REPORT 1: GUESTS BILLING SUMMARY", report.toString());
    if (frontDeskUI.confirmPdfExport()) {
      exportGuestsBillingReportToPdf(sortedRecords, guestCount, totalAmount,
          totalPaid, totalOutstanding);
    }
    MessageUI.pressEnterToContinue();
  }

  private void guestsRoomAvailabilityReport() {
    ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
    StringBuilder report = new StringBuilder();
    report.append(String.format("%-8s %-12s %-7s %-22s %-20s %-12s %-24s%n",
        "Room", "Type", "Floor", "Housekeeping Status", "Guest", "Confirm No.", "Availability"));
    report.append("-------------------------------------------------------------------------------------------------------------\n");

    int totalRooms = 0;
    int occupiedRooms = 0;
    int availableRooms = 0;
    int unavailableRooms = 0;

    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      GuestRecord assignedGuest = findGuestByRoomNumber(room.getRoomNumber());
      String availability = getRoomAvailabilityLabel(room, assignedGuest);

      if (assignedGuest != null) {
        occupiedRooms++;
      } else if (room.getStatus() == RoomStatus.READY_FOR_CHECK_IN) {
        availableRooms++;
      } else {
        unavailableRooms++;
      }

      report.append(String.format("%-8s %-12s %-7d %-22s %-20s %-12s %-24s%n",
          room.getRoomNumber(), room.getRoomType(), room.getFloor(),
          room.getStatus().getLabel(), guestNameOrDash(assignedGuest),
          confirmationNumberOrDash(assignedGuest), availability));
      totalRooms++;
    }

    if (totalRooms == 0) {
      report.append("No room records found.\n");
    }
    report.append("\nTotal rooms                  : ").append(totalRooms).append("\n");
    report.append("Occupied / reserved rooms    : ").append(occupiedRooms).append("\n");
    report.append("Available for check-in rooms : ").append(availableRooms).append("\n");
    report.append("Not available rooms          : ").append(unavailableRooms).append("\n");

    frontDeskUI.displayReport("REPORT 2: GUESTS ROOM AVAILABILITY", report.toString());
    if (frontDeskUI.confirmPdfExport()) {
      exportGuestsRoomAvailabilityReportToPdf(roomList, totalRooms, occupiedRooms,
          availableRooms, unavailableRooms);
    }
    MessageUI.pressEnterToContinue();
  }

  private void exportGuestsBillingReportToPdf(ListInterface<GuestRecord> sortedRecords,
      int guestCount, double totalAmount, double totalPaid, double totalOutstanding) {
    PdfReportEngine pdf = null;
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String outPath = outDir + File.separator + "frontdesk_billing_" + timestamp + ".pdf";

      pdf = new PdfReportEngine();
      pdf.addCoverPage(
          "Front-Desk Guest Billing Summary",
          "Guest Charges | Paid Amounts | Outstanding Balances",
          "Current guest records", "Front Desk Officer");

      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type", "Guest Billing Summary", null);
      pdf.addKpiRow("Records Analysed", String.valueOf(guestCount),
          guestCount == 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addKpiRow("Total Bill Amount", money(totalAmount), null);
      pdf.addKpiRow("Total Paid Amount", money(totalPaid), PdfReportEngine.SUCCESS);
      pdf.addKpiRow("Total Outstanding Amount", money(totalOutstanding),
          totalOutstanding > 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      pdf.addSectionHeading("Key Billing Indicators");
      pdf.addKpiCards(
          new String[]{"Guests", "Total Bill", "Paid", "Outstanding"},
          new String[]{String.valueOf(guestCount), money(totalAmount),
              money(totalPaid), money(totalOutstanding)},
          new java.awt.Color[]{PdfReportEngine.BRAND_TEAL, PdfReportEngine.ACCENT_BLUE,
              PdfReportEngine.SUCCESS, totalOutstanding > 0
                  ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS});
      pdf.addSpace(10);
      pdf.addBarChart("Billing Summary",
          new String[]{"Total", "Paid", "Outstanding"},
          new double[]{totalAmount, totalPaid, totalOutstanding}, "Amount (RM)");

      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed Guest Billing Records");
      pdf.addBodyText(
          "Records are listed by confirmation number using the front-desk Binary Search Tree traversal.",
          9);
      pdf.addSpace(6);

      String[] headers = {"Confirm", "Guest Name", "Room", "Type", "Total", "Paid", "Outstanding"};
      float[] colW = {70, 105, 45, 55, 70, 70, 80};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      for (int index = 1; index <= sortedRecords.getNumberOfEntries(); index++) {
        GuestRecord guestRecord = sortedRecords.getEntry(index);
        rows.add(new String[]{
            guestRecord.getConfirmationNumber(), guestRecord.getGuestName(),
            guestRecord.getRoomNumber(), guestRecord.getRoomType(),
            money(guestRecord.getTotalAmount()), money(guestRecord.getPaidAmount()),
            money(guestRecord.getOutstandingAmount())
        });
      }
      if (rows.isEmpty()) {
        pdf.addBodyText("No guest billing records found.", 10);
      } else {
        pdf.addTable(headers, rows, colW);
      }

      pdf.save(outPath);
      frontDeskUI.displayPdfExportSuccess(outPath);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    } finally {
      try {
        if (pdf != null) pdf.close();
      } catch (IOException ignored) {
        // Best-effort cleanup after the main export attempt.
      }
    }
  }

  private void exportGuestsRoomAvailabilityReportToPdf(ListInterface<Room> roomList,
      int totalRooms, int occupiedRooms, int availableRooms, int unavailableRooms) {
    PdfReportEngine pdf = null;
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String outPath = outDir + File.separator + "frontdesk_room_availability_"
          + timestamp + ".pdf";

      pdf = new PdfReportEngine();
      pdf.addCoverPage(
          "Front-Desk Room Availability Report",
          "Housekeeping Status | Occupancy | Check-In Readiness",
          "Current room records", "Front Desk Officer");

      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type", "Room Availability Report", null);
      pdf.addKpiRow("Rooms Analysed", String.valueOf(totalRooms),
          totalRooms == 0 ? PdfReportEngine.WARNING : PdfReportEngine.ACCENT_BLUE);
      pdf.addKpiRow("Occupied / Reserved", String.valueOf(occupiedRooms),
          occupiedRooms > 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addKpiRow("Available for Check-In", String.valueOf(availableRooms),
          availableRooms > 0 ? PdfReportEngine.SUCCESS : PdfReportEngine.WARNING);
      pdf.addKpiRow("Not Available", String.valueOf(unavailableRooms),
          unavailableRooms > 0 ? PdfReportEngine.DANGER : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      pdf.addSectionHeading("Availability Indicators");
      pdf.addKpiCards(
          new String[]{"Total Rooms", "Occupied", "Available", "Not Available"},
          new String[]{String.valueOf(totalRooms), String.valueOf(occupiedRooms),
              String.valueOf(availableRooms), String.valueOf(unavailableRooms)},
          new java.awt.Color[]{PdfReportEngine.ACCENT_BLUE, PdfReportEngine.WARNING,
              PdfReportEngine.SUCCESS, PdfReportEngine.DANGER});
      pdf.addSpace(10);
      pdf.addDonutChart("Room Availability Distribution",
          new String[]{"Occupied", "Available", "Not Available"},
          new double[]{occupiedRooms, availableRooms, unavailableRooms});

      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed Room Availability Records");
      pdf.addBodyText(
          "Room availability combines housekeeping readiness with assigned front-desk guest records.",
          9);
      pdf.addSpace(6);

      String[] headers = {"Room", "Type", "Floor", "Status", "Guest", "Confirm", "Availability"};
      float[] colW = {45, 65, 35, 95, 80, 65, 110};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
        Room room = roomList.getEntry(index);
        GuestRecord assignedGuest = findGuestByRoomNumber(room.getRoomNumber());

        rows.add(new String[]{
            room.getRoomNumber(), room.getRoomType(), String.valueOf(room.getFloor()),
            room.getStatus().getLabel(), guestNameOrDash(assignedGuest),
            confirmationNumberOrDash(assignedGuest),
            getRoomAvailabilityLabel(room, assignedGuest)
        });
      }
      if (rows.isEmpty()) {
        pdf.addBodyText("No room records found.", 10);
      } else {
        pdf.addTable(headers, rows, colW);
      }

      pdf.save(outPath);
      frontDeskUI.displayPdfExportSuccess(outPath);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    } finally {
      try {
        if (pdf != null) pdf.close();
      } catch (IOException ignored) {
        // Best-effort cleanup after the main export attempt.
      }
    }
  }

  private String money(double amount) {
    return String.format("RM %.2f", amount);
  }

  private GuestRecord promptForGuestRecord(boolean billingLookup) {
    while (true) {
      String confirmationNumber = frontDeskUI.inputConfirmationNumber();
      if (confirmationNumber.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage(billingLookup
            ? "Billing lookup cancelled."
            : "Guest search cancelled.");
        return null;
      }

      if (!isValidConfirmationNumber(confirmationNumber)) {
        displayConfirmationNumberInputMessage(billingLookup,
            "  Confirmation number must contain exactly 8 digits.\n"
            + "  Enter a valid confirmation number, or enter 0 to cancel.");
        continue;
      }

      GuestRecord guestRecord = guestSearchTree.search(confirmationNumber);
      if (guestRecord == null) {
        displayConfirmationNumberInputMessage(billingLookup,
            "  No " + (billingLookup ? "billing record" : "guest")
            + " found for confirmation: " + confirmationNumber
            + "\n  Try again, or enter 0 to cancel.");
        continue;
      }

      return guestRecord;
    }
  }

  private void displayConfirmationNumberInputMessage(boolean billingLookup,
      String message) {
    if (billingLookup) {
      frontDeskUI.displayBillingResult(message);
    } else {
      frontDeskUI.displaySearchResult(message);
    }
  }

  private String formatRoomAvailability(Room room, GuestRecord assignedGuest) {
    if (assignedGuest != null) {
      return "  Room " + assignedGuest.getRoomNumber() + " is "
          + getRoomAvailabilityLabel(room, assignedGuest)
          + "\n  Guest            : " + assignedGuest.getGuestName()
          + "\n  Confirmation No. : " + assignedGuest.getConfirmationNumber()
          + "\n  Stay Period      : " + assignedGuest.getCheckInDate()
          + " to " + assignedGuest.getCheckOutDate();
    }

    return "  Room No. : " + room.getRoomNumber()
        + "\n  Type     : " + room.getRoomType()
        + "\n  Floor    : " + room.getFloor()
        + "\n  Status   : " + room.getStatus().getLabel()
        + "\n  Result   : " + getRoomAvailabilityLabel(room, assignedGuest);
  }

  private String getRoomAvailabilityLabel(Room room, GuestRecord assignedGuest) {
    if (assignedGuest != null) return "OCCUPIED / RESERVED";
    return room.getStatus() == RoomStatus.READY_FOR_CHECK_IN
        ? "AVAILABLE FOR CHECK-IN"
        : "NOT AVAILABLE";
  }

  private String guestNameOrDash(GuestRecord guestRecord) {
    return guestRecord == null ? "-" : guestRecord.getGuestName();
  }

  private String confirmationNumberOrDash(GuestRecord guestRecord) {
    return guestRecord == null ? "-" : guestRecord.getConfirmationNumber();
  }

  private boolean isValidConfirmationNumber(String confirmationNumber) {
    return confirmationNumber.matches("\\d{8}");
  }

  private boolean isValidRoomNumber(String roomNumber) {
    return roomNumber.matches("R[0-9]{3,4}");
  }

  private Room findRoom(String roomNumber) {
    ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
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

  public static void main(String[] args) {
    FrontDeskService frontDeskService = new FrontDeskService();
    frontDeskService.runFrontDeskModule();
  }
}
