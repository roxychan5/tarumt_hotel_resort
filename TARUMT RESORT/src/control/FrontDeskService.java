package control;

import adt.ArrayList;
import adt.BinarySearchTree;
import adt.StackInterface;
import adt.ListInterface;
import adt.SearchTreeInterface;
import boundary.FrontDeskServiceUI;
import dao.FrontDeskDAO;
import dao.HousekeepingDAO;
import entity.LoyaltyTier;
import entity.RewardsMember;
import entity.Room;
import entity.RoomStatus;
import entity.StatusChangeRecord;
import utility.MalaysiaTime;
import utility.MessageUI;
import utility.PdfReportEngine;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Control class stub for Front-Desk Service module (team member integration point).
 *
 * @author Your Name
 */
public class FrontDeskService {

  private final ListInterface<RewardsMember> memberRecords = new ArrayList<>();
  private final SearchTreeInterface<String, RewardsMember> memberSearchTree =
      new BinarySearchTree<>(); //non-linear data structure for front desk 
  private final FrontDeskDAO frontDeskDAO = new FrontDeskDAO();
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  private final FrontDeskServiceUI frontDeskUI = new FrontDeskServiceUI();

  public FrontDeskService() {
    loadData();
    rebuildMemberSearchTree();
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
          searchMember();
          break;
        case 2:
          checkRoomAvailability();
          break;
        case 3:
          checkOutRoom();
          break;
        case 4:
          viewMemberAccountDetails();
          break;
        case 5:
          frontDeskUI.displayMemberList(getAllMemberRecords());
          MessageUI.pressEnterToContinue();
          break;
        case 6:
          viewCheckoutHistory();
          break;
        case 7:
          memberAccountReport();
          break;
        case 8:
          guestsRoomAvailabilityReport();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void searchMember() {
    while (true) {
      String searchKey = frontDeskUI.inputMemberSearchKey();
      if (searchKey.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Member search cancelled.");
        return;
      }

      if (searchKey.trim().isEmpty()) {
        frontDeskUI.displaySearchResult(
            "  Enter a member name or loyalty member ID, or enter 0 to cancel.");
        continue;
      }

      if (isValidMemberId(searchKey.toUpperCase())) {
        RewardsMember memberRecord = memberSearchTree.search(searchKey.toUpperCase());
        if (memberRecord == null) {
          frontDeskUI.displaySearchResult(
              "  No member record found for ID: " + searchKey.toUpperCase()
              + "\n  Try again, or enter 0 to cancel.");
          continue;
        }
        frontDeskUI.displayMemberDetails(memberRecord);
      } else {
        ListInterface<RewardsMember> matches = findMembersByName(searchKey);
        frontDeskUI.displayMemberSearchResults(formatMemberSearchResults(matches));
      }

      MessageUI.pressEnterToContinue();
      return;
    }
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

      if (room == null) {
        frontDeskUI.displayRoomAvailability(
            "  Room " + roomNumber + " was not found.\n"
            + "  Try again, or enter 0 to cancel.");
        continue;
      }

      frontDeskUI.displayRoomAvailability(formatRoomAvailability(room));
      MessageUI.pressEnterToContinue();
      return;
    }
  }

  private void checkOutRoom() {
    ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
    frontDeskUI.displayCheckoutAvailability(formatRoomAvailabilityBoard(roomList));
    while (true) {
      String roomNumber = frontDeskUI.inputRoomNumber();
      if (roomNumber.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Room check-out cancelled.");
        return;
      }

      if (!isValidRoomNumber(roomNumber)) {
        frontDeskUI.displayCheckoutResult(
            "  Room number must be R followed by 3-4 digits, for example R101.\n"
            + "  Enter a valid room number, or enter 0 to cancel.");
        continue;
      }

      Room room = findRoom(roomList, roomNumber);
      if (room == null) {
        frontDeskUI.displayCheckoutResult(
            "  Room " + roomNumber + " was not found.\n"
            + "  Try again, or enter 0 to cancel.");
        continue;
      }

      if (room.getStatus() != RoomStatus.OCCUPIED) {
        frontDeskUI.displayCheckoutResult(
            "  Room " + room.getRoomNumber() + " cannot be checked out.\n"
            + "  Current status: " + room.getStatus().getLabel()
            + "\n  Only occupied rooms can be checked out.");
        MessageUI.pressEnterToContinue();
        return;
      }

      frontDeskUI.displayCheckoutResult(formatRoomAvailability(room));
      if (!frontDeskUI.confirmCheckout(room.getRoomNumber())) {
        MessageUI.displayInfoMessage("Room check-out cancelled.");
        MessageUI.pressEnterToContinue();
        return;
      }

      RoomStatus previousStatus = room.getStatus();
      room.setStatus(RoomStatus.DIRTY);
      housekeepingDAO.saveRooms(roomList);
      recordCheckoutStatusChange(room.getRoomNumber(), previousStatus);

      frontDeskUI.displayCheckoutResult(formatRoomAvailability(room));
      frontDeskUI.displayCheckoutAvailability(formatRoomAvailabilityBoard(roomList));
      MessageUI.displaySuccessMessage(
          "Room " + room.getRoomNumber() + " checked out and marked Dirty for housekeeping.");
      MessageUI.pressEnterToContinue();
      return;
    }
  }

  private void viewMemberAccountDetails() {
    RewardsMember memberRecord = promptForMemberRecord(true);
    if (memberRecord == null) return;
    frontDeskUI.displayMemberAccountDetails(memberRecord);
    MessageUI.pressEnterToContinue();
  }

  private void viewCheckoutHistory() {
    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    frontDeskUI.displayCheckoutHistory(formatCheckoutHistory(history));
    MessageUI.pressEnterToContinue();
  }

  private void memberAccountReport() {
    ListInterface<RewardsMember> sortedRecords = memberSearchTree.inOrderTraversal();
    StringBuilder report = new StringBuilder();
    report.append(String.format("%-12s %-22s %-12s %-10s %-15s %s%n",
        "Member ID", "Name", "Tier", "Points", "Expiry", "Promotion"));
    report.append("-------------------------------------------------------------------------------------------------\n");

    int memberCount = 0;
    int totalPoints = 0;
    int expiringSoon = 0;
    int[] tierCounts = new int[LoyaltyTier.values().length];
    LocalDate expiryLimit = LocalDate.now().plusDays(30);

    for (int index = 1; index <= sortedRecords.getNumberOfEntries(); index++) {
      RewardsMember memberRecord = sortedRecords.getEntry(index);
      report.append(String.format("%-12s %-22s %-12s %-10d %-15s %s%n",
          memberRecord.getMemberId(), memberRecord.getName(), memberRecord.getTier(),
          memberRecord.getPoints(), memberRecord.getPointsExpiryDate(),
          memberRecord.getPromotion()));
      memberCount++;
      totalPoints += memberRecord.getPoints();
      tierCounts[memberRecord.getTier().ordinal()]++;
      if (memberRecord.getPointsExpiryDate() != null
          && !memberRecord.getPointsExpiryDate().isAfter(expiryLimit)) {
        expiringSoon++;
      }
    }

    if (memberCount == 0) {
      report.append("No linked member records found.\n");
    }
    report.append("\nTotal members                : ").append(memberCount).append("\n");
    report.append("Total loyalty points         : ").append(totalPoints).append("\n");
    report.append("Members expiring within 30d  : ").append(expiringSoon).append("\n");
    for (LoyaltyTier tier : LoyaltyTier.values()) {
      report.append(String.format("%-28s: %d%n", tier + " members", tierCounts[tier.ordinal()]));
    }

    frontDeskUI.displayReport("REPORT 1: MEMBER ACCOUNT SUMMARY", report.toString());
    if (frontDeskUI.confirmPdfExport()) {
      exportMemberAccountReportToPdf(sortedRecords, memberCount, totalPoints, expiringSoon,
          tierCounts);
    }
    MessageUI.pressEnterToContinue();
  }

  private void guestsRoomAvailabilityReport() {
    ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
    StringBuilder report = new StringBuilder();
    report.append(String.format("%-8s %-12s %-7s %-22s %-24s%n",
        "Room", "Type", "Floor", "Housekeeping Status", "Availability"));
    report.append("-------------------------------------------------------------------------------\n");

    int totalRooms = 0;
    int occupiedRooms = 0;
    int availableRooms = 0;
    int unavailableRooms = 0;

    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      String availability = getRoomAvailabilityLabel(room);

      if (room.getStatus() == RoomStatus.OCCUPIED) {
        occupiedRooms++;
      } else if (room.getStatus() == RoomStatus.READY_FOR_CHECK_IN) {
        availableRooms++;
      } else {
        unavailableRooms++;
      }

      report.append(String.format("%-8s %-12s %-7d %-22s %-24s%n",
          room.getRoomNumber(), room.getRoomType(), room.getFloor(),
          room.getStatus().getLabel(), availability));
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

  private void exportMemberAccountReportToPdf(ListInterface<RewardsMember> sortedRecords,
      int memberCount, int totalPoints, int expiringSoon, int[] tierCounts) {
    PdfReportEngine pdf = null;
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
      String outPath = outDir + File.separator + "frontdesk_member_accounts_" + timestamp + ".pdf";

      pdf = new PdfReportEngine();
      pdf.addCoverPage(
          "Front-Desk Member Account Summary",
          "Member Records | Loyalty Tier | Reward Points",
          "Linked loyalty member records", "Front Desk Officer");

      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type", "Member Account Summary", null);
      pdf.addKpiRow("Records Analysed", String.valueOf(memberCount),
          memberCount == 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addKpiRow("Total Loyalty Points", String.valueOf(totalPoints), null);
      pdf.addKpiRow("Expiring Within 30 Days", String.valueOf(expiringSoon),
          expiringSoon > 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      pdf.addSectionHeading("Key Member Indicators");
      pdf.addKpiCards(
          new String[]{"Members", "Total Points", "Expiring Soon", "Tier Groups"},
          new String[]{String.valueOf(memberCount), String.valueOf(totalPoints),
              String.valueOf(expiringSoon), String.valueOf(LoyaltyTier.values().length)},
          new java.awt.Color[]{PdfReportEngine.BRAND_TEAL, PdfReportEngine.ACCENT_BLUE,
              expiringSoon > 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS,
              PdfReportEngine.SUCCESS});
      pdf.addSpace(10);
      pdf.addBarChart("Members by Tier", tierLabels(), countsToValues(tierCounts), "Members");

      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed Member Records");
      pdf.addBodyText(
          "Records are listed by member ID using the front-desk Binary Search Tree traversal.",
          9);
      pdf.addSpace(6);

      String[] headers = {"Member ID", "Name", "Tier", "Points", "Expiry", "Promotion"};
      float[] colW = {65, 105, 70, 50, 80, 125};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      for (int index = 1; index <= sortedRecords.getNumberOfEntries(); index++) {
        RewardsMember memberRecord = sortedRecords.getEntry(index);
        rows.add(new String[]{
            memberRecord.getMemberId(), memberRecord.getName(), memberRecord.getTier().toString(),
            String.valueOf(memberRecord.getPoints()),
            String.valueOf(memberRecord.getPointsExpiryDate()),
            memberRecord.getPromotion()
        });
      }
      if (rows.isEmpty()) {
        pdf.addBodyText("No linked member records found.", 10);
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
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
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
          "Room availability is checked directly from the shared housekeeping room records.",
          9);
      pdf.addSpace(6);

      String[] headers = {"Room", "Type", "Floor", "Status", "Availability"};
      float[] colW = {55, 85, 45, 145, 165};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
        Room room = roomList.getEntry(index);
        rows.add(new String[]{
            room.getRoomNumber(), room.getRoomType(), String.valueOf(room.getFloor()),
            room.getStatus().getLabel(), getRoomAvailabilityLabel(room)
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

  private String[] tierLabels() {
    LoyaltyTier[] tiers = LoyaltyTier.values();
    String[] labels = new String[tiers.length];
    for (int index = 0; index < tiers.length; index++) {
      labels[index] = tiers[index].toString();
    }
    return labels;
  }

  private double[] countsToValues(int[] counts) {
    double[] values = new double[counts.length];
    for (int index = 0; index < counts.length; index++) {
      values[index] = counts[index];
    }
    return values;
  }

  private RewardsMember promptForMemberRecord(boolean accountLookup) {
    while (true) {
      String searchKey = frontDeskUI.inputMemberSearchKey();
      if (searchKey.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage(accountLookup
            ? "Member account lookup cancelled."
            : "Member search cancelled.");
        return null;
      }

      if (searchKey.trim().isEmpty()) {
        displayMemberIdInputMessage(accountLookup,
            "  Enter a member name or loyalty member ID, or enter 0 to cancel.");
        continue;
      }

      if (isValidMemberId(searchKey.toUpperCase())) {
        RewardsMember memberRecord = memberSearchTree.search(searchKey.toUpperCase());
        if (memberRecord == null) {
          displayMemberIdInputMessage(accountLookup,
              "  No member record found for ID: " + searchKey.toUpperCase()
              + "\n  Try again, or enter 0 to cancel.");
          continue;
        }
        return memberRecord;
      }

      if (searchKey.toUpperCase().startsWith("LM")) {
        displayMemberIdInputMessage(accountLookup,
            "  Member ID must be LM followed by 3 digits, for example LM001.\n"
            + "  Or enter a guest name to search by name.");
        continue;
      }

      ListInterface<RewardsMember> matches = findMembersByName(searchKey);
      if (matches.getNumberOfEntries() == 0) {
        displayMemberIdInputMessage(accountLookup,
            "  No member name matched: " + searchKey
            + "\n  Try another name or member ID, or enter 0 to cancel.");
        continue;
      }

      if (matches.getNumberOfEntries() == 1) {
        return matches.getEntry(1);
      }

      frontDeskUI.displayMemberSearchResults(formatMemberSearchResults(matches)
          + "\n  More than one member matched. Enter a member ID or a more specific name.");
    }
  }

  private void displayMemberIdInputMessage(boolean accountLookup,
      String message) {
    if (accountLookup) {
      frontDeskUI.displayBillingResult(message);
    } else {
      frontDeskUI.displaySearchResult(message);
    }
  }

  private String formatRoomAvailability(Room room) {
    return "  Room No. : " + room.getRoomNumber()
        + "\n  Type     : " + room.getRoomType()
        + "\n  Floor    : " + room.getFloor()
        + "\n  Status   : " + room.getStatus().getLabel()
        + "\n  Result   : " + getRoomAvailabilityLabel(room);
  }

  private String formatRoomAvailabilityBoard(ListInterface<Room> roomList) {
    StringBuilder output = new StringBuilder();
    output.append(String.format("  %-8s %-12s %-7s %-22s %-24s%n",
        "Room", "Type", "Floor", "Housekeeping Status", "Availability"));
    output.append("  -------------------------------------------------------------------------------\n");

    int totalRooms = 0;
    int occupiedRooms = 0;
    int availableRooms = 0;
    int unavailableRooms = 0;

    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      if (room.getStatus() == RoomStatus.OCCUPIED) {
        occupiedRooms++;
      } else if (room.getStatus() == RoomStatus.READY_FOR_CHECK_IN) {
        availableRooms++;
      } else {
        unavailableRooms++;
      }

      output.append(String.format("  %-8s %-12s %-7d %-22s %-24s%n",
          room.getRoomNumber(), room.getRoomType(), room.getFloor(),
          room.getStatus().getLabel(), getRoomAvailabilityLabel(room)));
      totalRooms++;
    }

    if (totalRooms == 0) {
      output.append("  No room records found.\n");
    }

    output.append("\n");
    output.append("  Total rooms                  : ").append(totalRooms).append("\n");
    output.append("  Occupied / reserved rooms    : ").append(occupiedRooms).append("\n");
    output.append("  Available for check-in rooms : ").append(availableRooms).append("\n");
    output.append("  Not available rooms          : ").append(unavailableRooms);
    return output.toString();
  }

  private String formatMemberSearchResults(ListInterface<RewardsMember> matches) {
    StringBuilder output = new StringBuilder();
    if (matches.getNumberOfEntries() == 0) {
      return "  No matching member records found.";
    }

    output.append(String.format("  %-12s %-22s %-12s %-10s %-15s %s%n",
        "Member ID", "Name", "Tier", "Points", "Expiry", "Email"));
    output.append("  -------------------------------------------------------------------------------------------\n");
    for (int index = 1; index <= matches.getNumberOfEntries(); index++) {
      RewardsMember memberRecord = matches.getEntry(index);
      output.append(String.format("  %-12s %-22s %-12s %-10d %-15s %s%n",
          memberRecord.getMemberId(), memberRecord.getName(), memberRecord.getTier(),
          memberRecord.getPoints(), memberRecord.getPointsExpiryDate(),
          memberRecord.getEmail()));
    }
    output.append("\n  Matching records: ").append(matches.getNumberOfEntries());
    return output.toString();
  }

  private String formatCheckoutHistory(StackInterface<StatusChangeRecord> history) {
    StringBuilder output = new StringBuilder();
    output.append(String.format("  %-8s %-22s %-22s %-24s %s%n",
        "Room", "Previous Status", "New Status", "Changed At", "Reason"));
    output.append("  ------------------------------------------------------------------------------------------------\n");

    int checkoutCount = 0;
    while (!history.isEmpty()) {
      StatusChangeRecord record = history.pop();
      if (!isCheckoutRecord(record)) {
        continue;
      }

      output.append(String.format("  %-8s %-22s %-22s %-24s %s%n",
          record.getRoomNumber(), record.getPreviousStatus().getLabel(),
          record.getNewStatus().getLabel(), MalaysiaTime.format(record.getChangedAt()),
          record.getReason()));
      checkoutCount++;
    }

    if (checkoutCount == 0) {
      output.append("  No check-out records found.\n");
    }

    output.append("\n  Check-out records shown: ").append(checkoutCount);
    return output.toString();
  }

  private String getRoomAvailabilityLabel(Room room) {
    if (room.getStatus() == RoomStatus.OCCUPIED) return "OCCUPIED / RESERVED";
    return room.getStatus() == RoomStatus.READY_FOR_CHECK_IN
        ? "AVAILABLE FOR CHECK-IN"
        : "NOT AVAILABLE";
  }

  private boolean isValidMemberId(String memberId) {
    return memberId.matches("LM[0-9]{3}");
  }

  private ListInterface<RewardsMember> findMembersByName(String searchKey) {
    ListInterface<RewardsMember> matches = new ArrayList<>();
    String normalizedSearchKey = searchKey.trim().toLowerCase();
    for (int index = 1; index <= memberRecords.getNumberOfEntries(); index++) {
      RewardsMember memberRecord = memberRecords.getEntry(index);
      if (memberRecord.getName().toLowerCase().contains(normalizedSearchKey)) {
        matches.add(memberRecord);
      }
    }
    return matches;
  }

  private boolean isCheckoutRecord(StatusChangeRecord record) {
    String reason = record.getReason() == null ? "" : record.getReason().toLowerCase();
    return reason.contains("check-out")
        || reason.contains("checked out")
        || (record.getPreviousStatus() == RoomStatus.OCCUPIED
            && record.getNewStatus() == RoomStatus.DIRTY);
  }

  private boolean isValidRoomNumber(String roomNumber) {
    return roomNumber.matches("R[0-9]{3,4}");
  }

  private Room findRoom(String roomNumber) {
    ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
    return findRoom(roomList, roomNumber);
  }

  private Room findRoom(ListInterface<Room> roomList, String roomNumber) {
    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        return room;
      }
    }
    return null;
  }

  private void recordCheckoutStatusChange(String roomNumber, RoomStatus previousStatus) {
    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    StackInterface<StatusChangeRecord> redoHistory = housekeepingDAO.retrieveRedoHistory();
    history.push(new StatusChangeRecord(roomNumber, previousStatus, RoomStatus.DIRTY,
        "Guest checked out at Front Desk", MalaysiaTime.now()));
    redoHistory.clear();
    housekeepingDAO.saveHistory(history);
    housekeepingDAO.saveRedoHistory(redoHistory);
  }

  private String getAllMemberRecords() {
    ListInterface<RewardsMember> sortedRecords = memberSearchTree.inOrderTraversal();
    StringBuilder output = new StringBuilder();
    for (int index = 1; index <= sortedRecords.getNumberOfEntries(); index++) {
      RewardsMember memberRecord = sortedRecords.getEntry(index);
      output.append(String.format("%-12s %-22s %-12s %-10d %s%n",
          memberRecord.getMemberId(), memberRecord.getName(), memberRecord.getTier(),
          memberRecord.getPoints(), memberRecord.getPointsExpiryDate()));
    }
    return output.toString();
  }

  private void loadData() {
    ListInterface<RewardsMember> loadedRecords = frontDeskDAO.retrieveMemberRecords();
    memberRecords.clear();
    for (int index = 1; index <= loadedRecords.getNumberOfEntries(); index++) {
      memberRecords.add(loadedRecords.getEntry(index));
    }
  }

  private void rebuildMemberSearchTree() {
    memberSearchTree.clear();
    for (int index = 1; index <= memberRecords.getNumberOfEntries(); index++) {
      RewardsMember memberRecord = memberRecords.getEntry(index);
      memberSearchTree.insert(memberRecord.getMemberId(), memberRecord);
    }
  }
}
