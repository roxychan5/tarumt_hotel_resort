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
import entity.VipCheckInRecord;
import entity.VipPaymentRecord;
import utility.MalaysiaTime;
import utility.MessageUI;
import utility.PdfReportEngine;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;

/**
 * Control class stub for Front-Desk Service module (team member integration point).
 *
 * @author Your Name
 */
public class FrontDeskService {

  private static final DateTimeFormatter CHECKOUT_DEADLINE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  private static final String EXPECTED_CHECKOUT_MARKER = "Expected check-out: ";

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
          handleLateCheckout();
          break;
        case 5:
          viewMemberAccountDetails();
          break;
        case 6:
          viewBillingDetails();
          break;
        case 7:
          frontDeskUI.displayMemberList(getAllMemberRecords());
          MessageUI.pressEnterToContinue();
          break;
        case 8:
          viewCheckoutHistory();
          break;
        case 9:
          viewLateCheckoutHistory();
          break;
        case 10:
          memberAccountReport();
          break;
        case 11:
          bookingBillingReport();
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
    ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
    frontDeskUI.displayRoomAvailability(formatRoomAvailabilityBoard(roomList));
    MessageUI.pressEnterToContinue();
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

      if (!isActiveGuestRoom(room.getStatus())) {
        frontDeskUI.displayCheckoutResult(
            "  Room " + room.getRoomNumber() + " cannot be checked out.\n"
            + "  Current status: " + room.getStatus().getLabel()
            + "\n  Only occupied or late check-out rooms can be checked out.");
        MessageUI.pressEnterToContinue();
        return;
      }

      RoomStayTimeline timeline = findLatestRoomStayTimeline(room.getRoomNumber());
      frontDeskUI.displayCheckoutResult(formatCheckoutPreview(room, timeline, null, false));
      boolean lateCheckout = room.getStatus() == RoomStatus.LCO
          || frontDeskUI.confirmLateCheckout(room.getRoomNumber());
      LocalDateTime actualCheckoutAt = MalaysiaTime.now();
      frontDeskUI.displayCheckoutConfirmationSummary(formatCheckoutConfirmationSummary(
          room, timeline, actualCheckoutAt, lateCheckout));
      if (!frontDeskUI.confirmCheckout(room.getRoomNumber())) {
        MessageUI.displayInfoMessage("Room check-out cancelled.");
        MessageUI.pressEnterToContinue();
        return;
      }

      RoomStatus previousStatus = room.getStatus();
      String occupantMemberId = room.getOccupantMemberId();
      room.setStatus(RoomStatus.DIRTY);
      // Keep the most recent stay details until a new guest is allocated.
      // Housekeeping may need to roll a READY room back to LCO, after which
      // Front Desk uses these details to identify the guest and extend the date.
      housekeepingDAO.saveRooms(roomList);
      recordCheckoutStatusChange(room.getRoomNumber(), previousStatus, timeline,
          occupantMemberId, lateCheckout, actualCheckoutAt);

      frontDeskUI.displayCheckoutResult(formatCheckoutPreview(room, timeline,
          actualCheckoutAt, lateCheckout));
      frontDeskUI.displayCheckoutAvailability(formatRoomAvailabilityBoard(roomList));
      MessageUI.displaySuccessMessage(
          "Room " + room.getRoomNumber() + " checked out and marked Dirty for housekeeping.");
      MessageUI.pressEnterToContinue();
      return;
    }
  }

  private void handleLateCheckout() {
    while (true) {
      String memberId = frontDeskUI.inputMemberId();
      if (memberId.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Late check-out handling cancelled.");
        return;
      }

      if (!isValidMemberId(memberId)) {
        frontDeskUI.displayLateCheckoutResult(
            "  Member ID must be LM followed by 3 to 6 digits, for example LM001.");
        continue;
      }

      RewardsMember memberRecord = memberSearchTree.search(memberId);
      if (memberRecord == null) {
        frontDeskUI.displayLateCheckoutResult(
            "  No member record found for ID: " + memberId + ".");
        continue;
      }

      ListInterface<Room> roomList = housekeepingDAO.retrieveRooms();
      Room room = findOccupiedRoomByMemberId(roomList, memberId);
      if (room == null) {
        frontDeskUI.displayLateCheckoutResult(
            "  " + memberRecord.getName() + " (" + memberId
            + ") is not linked to any occupied room.");
        MessageUI.pressEnterToContinue();
        return;
      }

      LocalDateTime previousExpectedCheckoutAt = room.getExpectedCheckoutAt();
      frontDeskUI.displayLateCheckoutResult(formatLateCheckoutDetails(memberRecord, room,
          previousExpectedCheckoutAt, null));

      LocalDateTime newExpectedCheckoutAt = promptForNewExpectedCheckoutAt(
          previousExpectedCheckoutAt, room.getCheckInAt());
      if (newExpectedCheckoutAt == null) return;

      frontDeskUI.displayLateCheckoutResult(formatLateCheckoutDetails(memberRecord, room,
          previousExpectedCheckoutAt, newExpectedCheckoutAt));
      if (!frontDeskUI.confirmLateCheckoutExtension(room.getRoomNumber())) {
        MessageUI.displayInfoMessage("Late check-out update cancelled.");
        MessageUI.pressEnterToContinue();
        return;
      }

      RoomStatus previousStatus = room.getStatus();
      room.setExpectedCheckoutAt(newExpectedCheckoutAt);
      room.setStatus(RoomStatus.LCO);
      housekeepingDAO.saveRooms(roomList);
      recordLateCheckoutExtension(room, previousStatus, memberRecord, previousExpectedCheckoutAt,
          newExpectedCheckoutAt);

      frontDeskUI.displayLateCheckoutSummary("Member: " + memberRecord.getName()
          + " (" + memberRecord.getMemberId() + ")\n"
          + "Room: " + room.getRoomNumber() + "\n"
          + "Previous expected check-out: " + formatRecordedDateTime(previousExpectedCheckoutAt)
          + "\nNew expected check-out: " + formatRecordedDateTime(newExpectedCheckoutAt));
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

  private void viewBillingDetails() {
    while (true) {
      String searchKey = frontDeskUI.inputBillingSearchKey();
      if (searchKey.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Billing lookup cancelled.");
        return;
      }

      if (searchKey.trim().isEmpty()) {
        frontDeskUI.displayBillingDetails(
            "  Enter a confirmation number, booking ID or loyalty member ID.");
        continue;
      }

      if (!isValidBillingSearchKey(searchKey)) {
        frontDeskUI.displayBillingDetails(
            "  Enter an 8-digit confirmation number, booking ID such as VIP-0001,\n"
            + "  or member ID such as LM001.");
        continue;
      }

      ListInterface<VipPaymentRecord> matches = findBillingRecords(searchKey);
      if (matches.getNumberOfEntries() == 0) {
        frontDeskUI.displayBillingDetails(
            "  No VIP billing record found for: " + searchKey
            + "\n  Try another confirmation number, booking ID or member ID.");
        continue;
      }

      frontDeskUI.displayBillingDetails(formatBillingDetails(searchKey, matches,
          frontDeskDAO.retrieveVipCheckInRecords()));
      MessageUI.pressEnterToContinue();
      return;
    }
  }

  private void viewCheckoutHistory() {
    LocalDate[] dateRange = promptForCheckoutHistoryDateRange();
    if (dateRange == null) return;
    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    frontDeskUI.displayCheckoutHistory(formatCheckoutHistory(history, dateRange[0], dateRange[1]));
    MessageUI.pressEnterToContinue();
  }

  private void viewLateCheckoutHistory() {
    LocalDate[] dateRange = promptForCheckoutHistoryDateRange();
    if (dateRange == null) return;
    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    frontDeskUI.displayLateCheckoutHistory(formatLateCheckoutHistory(history,
        dateRange[0], dateRange[1]));
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

  private void bookingBillingReport() {
    ListInterface<VipPaymentRecord> paymentRecords = frontDeskDAO.retrieveVipPaymentRecords();
    ListInterface<VipCheckInRecord> checkInRecords = frontDeskDAO.retrieveVipCheckInRecords();
    StringBuilder report = new StringBuilder();

    report.append(String.format("%-12s %-10s %-12s %-18s %-8s %-12s %-7s %-12s %-18s%n",
        "Confirm No.", "Booking", "Member ID", "Guest", "Room", "Type", "Nights",
        "Total", "Payment Method"));
    report.append("-------------------------------------------------------------------------------------------------------------\n");

    int bookingCount = 0;
    int checkedInCount = 0;
    double totalRevenue = 0.0;
    LinkedHashMap<String, Integer> methodCounts = new LinkedHashMap<>();
    LinkedHashMap<String, Double> roomTypeRevenue = new LinkedHashMap<>();

    for (int index = paymentRecords.getNumberOfEntries(); index >= 1; index--) {
      VipPaymentRecord payment = paymentRecords.getEntry(index);
      VipCheckInRecord checkInRecord = findCheckInRecord(payment, checkInRecords);
      RewardsMember memberRecord = memberSearchTree.search(payment.getMemberId().toUpperCase());
      String guestName = memberRecord == null ? "Unknown member" : memberRecord.getName();
      String roomNumber = checkInRecord == null ? "-" : checkInRecord.getRoomNumber();

      report.append(String.format("%-12s %-10s %-12s %-18s %-8s %-12s %-7d %-12s %-18s%n",
          payment.getConfirmationNumber(), payment.getBookingId(), payment.getMemberId(),
          guestName, roomNumber, payment.getRoomType(), payment.getNights(),
          formatMoney(payment.getTotalAmount()), payment.getPaymentMethod()));

      bookingCount++;
      if (checkInRecord != null) checkedInCount++;
      totalRevenue += payment.getTotalAmount();
      methodCounts.merge(payment.getPaymentMethod(), 1, Integer::sum);
      roomTypeRevenue.merge(payment.getRoomType(), payment.getTotalAmount(), Double::sum);
    }

    if (bookingCount == 0) {
      report.append("No VIP billing records found.\n");
    }

    report.append("\nTotal paid bookings          : ").append(bookingCount).append("\n");
    report.append("Checked-in paid bookings     : ").append(checkedInCount).append("\n");
    report.append("Paid but not checked in      : ").append(bookingCount - checkedInCount).append("\n");
    report.append("Total paid revenue           : ").append(formatMoney(totalRevenue)).append("\n");
    report.append("\nPayment method summary:\n")
        .append(formatCountSummary(methodCounts));
    report.append("\nRevenue by room type:\n")
        .append(formatRevenueSummary(roomTypeRevenue));

    frontDeskUI.displayReport("REPORT 2: BOOKING BILLING SUMMARY", report.toString());
    if (frontDeskUI.confirmPdfExport()) {
      exportBookingBillingReportToPdf(paymentRecords, checkInRecords, bookingCount,
          checkedInCount, totalRevenue, methodCounts, roomTypeRevenue);
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

  private void exportBookingBillingReportToPdf(ListInterface<VipPaymentRecord> paymentRecords,
      ListInterface<VipCheckInRecord> checkInRecords, int bookingCount, int checkedInCount,
      double totalRevenue, LinkedHashMap<String, Integer> methodCounts,
      LinkedHashMap<String, Double> roomTypeRevenue) {
    PdfReportEngine pdf = null;
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
      String outPath = outDir + File.separator + "frontdesk_booking_billing_"
          + timestamp + ".pdf";

      pdf = new PdfReportEngine();
      pdf.addCoverPage(
          "Front-Desk Booking Billing Report",
          "VIP Payments | Confirmation Numbers | Revenue",
          "Current VIP payment history", "Front Desk Officer");

      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type", "Booking Billing Summary", null);
      pdf.addKpiRow("Paid Bookings", String.valueOf(bookingCount),
          bookingCount == 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addKpiRow("Checked-In Paid Bookings", String.valueOf(checkedInCount), null);
      pdf.addKpiRow("Paid Not Checked In", String.valueOf(bookingCount - checkedInCount),
          bookingCount - checkedInCount > 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addKpiRow("Total Paid Revenue", formatMoney(totalRevenue), PdfReportEngine.SUCCESS);
      pdf.addDivider();

      pdf.addSectionHeading("Billing Indicators");
      pdf.addKpiCards(
          new String[]{"Paid Bookings", "Checked In", "Pending Stay", "Revenue"},
          new String[]{String.valueOf(bookingCount), String.valueOf(checkedInCount),
              String.valueOf(bookingCount - checkedInCount), formatMoney(totalRevenue)},
          new java.awt.Color[]{PdfReportEngine.ACCENT_BLUE, PdfReportEngine.SUCCESS,
              PdfReportEngine.WARNING, PdfReportEngine.BRAND_TEAL});
      pdf.addSpace(10);
      if (!methodCounts.isEmpty()) {
        pdf.addDonutChart("Payment Method Distribution",
            methodCounts.keySet().toArray(new String[0]), countsToValues(methodCounts));
      }
      pdf.addSectionHeading("Revenue by Room Type");
      pdf.addBodyText(formatRevenueSummary(roomTypeRevenue).replace("\n", " ").trim(), 10);

      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed Booking Billing Records");
      pdf.addBodyText(
          "Records are read from VIP payment history and matched with VIP check-in history.",
          9);
      pdf.addSpace(6);

      String[] headers = {"Confirm", "Booking", "Member", "Guest", "Room", "Type",
          "Nights", "Total", "Method"};
      float[] colW = {55, 55, 55, 75, 50, 55, 35, 55, 65};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      for (int index = paymentRecords.getNumberOfEntries(); index >= 1; index--) {
        VipPaymentRecord payment = paymentRecords.getEntry(index);
        VipCheckInRecord checkInRecord = findCheckInRecord(payment, checkInRecords);
        RewardsMember memberRecord = memberSearchTree.search(payment.getMemberId().toUpperCase());
        rows.add(new String[]{
            payment.getConfirmationNumber(), payment.getBookingId(), payment.getMemberId(),
            memberRecord == null ? "Unknown" : memberRecord.getName(),
            checkInRecord == null ? "-" : checkInRecord.getRoomNumber(),
            payment.getRoomType(), String.valueOf(payment.getNights()),
            formatMoney(payment.getTotalAmount()), payment.getPaymentMethod()
        });
      }
      if (rows.isEmpty()) {
        pdf.addBodyText("No VIP billing records found.", 10);
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

  private double[] countsToValues(LinkedHashMap<String, Integer> counts) {
    double[] values = new double[counts.size()];
    int index = 0;
    for (Integer count : counts.values()) {
      values[index++] = count;
    }
    return values;
  }

  private String formatCountSummary(LinkedHashMap<String, Integer> counts) {
    if (counts.isEmpty()) return "  - None\n";
    StringBuilder output = new StringBuilder();
    for (String label : counts.keySet()) {
      output.append("  - ").append(label).append(": ")
          .append(counts.get(label)).append(" booking(s)\n");
    }
    return output.toString();
  }

  private String formatRevenueSummary(LinkedHashMap<String, Double> revenue) {
    if (revenue.isEmpty()) return "  - None\n";
    StringBuilder output = new StringBuilder();
    for (String roomType : revenue.keySet()) {
      output.append("  - ").append(roomType).append(": ")
          .append(formatMoney(revenue.get(roomType))).append("\n");
    }
    return output.toString();
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
    frontDeskUI.displaySearchResult(message);
  }

  private String formatRoomAvailability(Room room) {
    return "  Room No. : " + room.getRoomNumber()
        + "\n  Type     : " + room.getRoomType()
        + "\n  Floor    : " + room.getFloor()
        + "\n  Status   : " + room.getStatus().getLabel()
        + "\n  Check-In : " + formatRecordedDateTime(room.getCheckInAt())
        + "\n  Check-Out: " + formatRecordedDateTime(room.getExpectedCheckoutAt())
        + "\n  Result   : " + getRoomAvailabilityLabel(room);
  }

  private String formatLateCheckoutDetails(RewardsMember memberRecord, Room room,
      LocalDateTime previousExpectedCheckoutAt, LocalDateTime newExpectedCheckoutAt) {
    return "  Member ID                   : " + memberRecord.getMemberId()
        + "\n  Name                        : " + memberRecord.getName()
        + "\n  Email                       : " + memberRecord.getEmail()
        + "\n  Tier                        : " + memberRecord.getTier()
        + "\n  Room No.                    : " + room.getRoomNumber()
        + "\n  Room Type                   : " + room.getRoomType()
        + "\n  Check-In Date / Time        : " + formatRecordedDateTime(room.getCheckInAt())
        + "\n  Previous Expected Check-Out : "
        + formatRecordedDateTime(previousExpectedCheckoutAt)
        + "\n  New Expected Check-Out      : "
        + formatRecordedDateTime(newExpectedCheckoutAt);
  }

  private String formatCheckoutPreview(Room room, RoomStayTimeline timeline,
      LocalDateTime actualCheckoutAt, boolean lateCheckout) {
    String checkoutType = actualCheckoutAt == null
        ? "Pending staff confirmation"
        : (lateCheckout ? "Late check-out" : "On-time check-out");

    return "  Room No.              : " + room.getRoomNumber()
        + "\n  Member ID             : " + formatOptionalText(room.getOccupantMemberId())
        + "\n  Guest                 : " + getGuestNameForRoom(room)
        + "\n  Type                  : " + room.getRoomType()
        + "\n  Floor                 : " + room.getFloor()
        + "\n  Current Status        : " + room.getStatus().getLabel()
        + "\n  Check-In Date / Time  : " + formatRecordedDateTime(timeline.checkInAt)
        + "\n  Expected Check-Out    : " + formatRecordedDateTime(timeline.expectedCheckoutAt)
        + "\n  Actual Check-Out      : " + formatRecordedDateTime(actualCheckoutAt)
        + "\n  Check-Out Type        : " + checkoutType
        + "\n  Next Room Status      : Dirty";
  }

  private String formatCheckoutConfirmationSummary(Room room, RoomStayTimeline timeline,
      LocalDateTime actualCheckoutAt, boolean lateCheckout) {
    return "Member: " + getGuestNameForRoom(room) + " ("
        + formatOptionalText(room.getOccupantMemberId()) + ")"
        + "\nRoom: " + room.getRoomNumber() + " - " + room.getRoomType()
        + "\nCurrent status: " + room.getStatus().getLabel()
        + "\nCheck-in: " + formatRecordedDateTime(timeline.checkInAt)
        + "\nExpected check-out: " + formatRecordedDateTime(timeline.expectedCheckoutAt)
        + "\nActual check-out: " + formatRecordedDateTime(actualCheckoutAt)
        + "\nCheck-out type: " + (lateCheckout ? "Late check-out" : "On-time check-out")
        + "\nNext room status: Dirty";
  }

  private String formatBillingDetails(String searchKey, ListInterface<VipPaymentRecord> records,
      ListInterface<VipCheckInRecord> checkInRecords) {
    StringBuilder output = new StringBuilder();
    output.append("  Search key          : ").append(searchKey).append("\n");
    output.append("  Billing records     : ").append(records.getNumberOfEntries()).append("\n");

    for (int index = 1; index <= records.getNumberOfEntries(); index++) {
      VipPaymentRecord payment = records.getEntry(index);
      VipCheckInRecord checkInRecord = findCheckInRecord(payment, checkInRecords);
      RewardsMember memberRecord = memberSearchTree.search(payment.getMemberId().toUpperCase());

      output.append("\n  --- BILLING RECORD ").append(index).append(" ---\n");
      appendBillingLine(output, "Payment ID", payment.getPaymentId());
      appendBillingLine(output, "Payment Status", "PAID");
      appendBillingLine(output, "Booking ID", payment.getBookingId());
      appendBillingLine(output, "Confirmation No.", payment.getConfirmationNumber());
      appendBillingLine(output, "Member", formatBillingMember(payment.getMemberId(), memberRecord));
      appendBillingLine(output, "Email", memberRecord == null ? "Not recorded"
          : memberRecord.getEmail());
      appendBillingLine(output, "Tier", memberRecord == null ? "Not recorded"
          : memberRecord.getTier().toString());
      appendBillingLine(output, "Room Type", payment.getRoomType());
      appendBillingLine(output, "Room Number", checkInRecord == null ? "Not checked in yet"
          : checkInRecord.getRoomNumber());
      appendBillingLine(output, "Booked Check-In", String.valueOf(payment.getCheckInDate()));
      appendBillingLine(output, "Booked Check-Out", String.valueOf(payment.getCheckOutDate()));
      appendBillingLine(output, "Actual Check-In", checkInRecord == null ? "Not checked in yet"
          : formatRecordedDateTime(checkInRecord.getCheckInAt()));
      appendBillingLine(output, "Expected Check-Out", checkInRecord == null ? "Not checked in yet"
          : formatRecordedDateTime(checkInRecord.getExpectedCheckoutAt()));
      appendBillingLine(output, "Length of Stay", payment.getNights() + " night(s)");
      appendBillingLine(output, "Price Per Night", formatMoney(payment.getPricePerNight()));
      appendBillingLine(output, "Total Amount", formatMoney(payment.getTotalAmount()));
      appendBillingLine(output, "Payment Method", payment.getPaymentMethod());
      appendBillingLine(output, "Paid At", formatRecordedDateTime(payment.getPaidAt()));
    }

    return output.toString();
  }

  private String formatRoomAvailabilityBoard(ListInterface<Room> roomList) {
    StringBuilder output = new StringBuilder();
    output.append(String.format("  %-8s %-12s %-7s %-20s %-18s %-18s %-24s%n",
        "Room", "Type", "Floor", "Status", "Check-In", "Expected Out", "Availability"));
    output.append("  -------------------------------------------------------------------------------------------------------------\n");

    int totalRooms = 0;
    int occupiedRooms = 0;
    int lateCheckoutRooms = 0;
    int availableRooms = 0;
    int unavailableRooms = 0;

    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      if (isActiveGuestRoom(room.getStatus())) {
        occupiedRooms++;
        if (room.getStatus() == RoomStatus.LCO) lateCheckoutRooms++;
      } else if (room.getStatus() == RoomStatus.READY_FOR_CHECK_IN) {
        availableRooms++;
      } else {
        unavailableRooms++;
      }

      output.append(String.format("  %-8s %-12s %-7d %-20s %-18s %-18s %-24s%n",
          room.getRoomNumber(), room.getRoomType(), room.getFloor(),
          room.getStatus().getLabel(), formatBoardDateTime(room.getCheckInAt()),
          formatBoardDateTime(room.getExpectedCheckoutAt()),
          getRoomAvailabilityLabel(room)));
      totalRooms++;
    }

    if (totalRooms == 0) {
      output.append("  No room records found.\n");
    }

    output.append("\n");
    output.append("  Total rooms                  : ").append(totalRooms).append("\n");
    output.append("  Occupied / reserved rooms    : ").append(occupiedRooms).append("\n");
    output.append("  Late check-out rooms         : ").append(lateCheckoutRooms).append("\n");
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

  private String formatCheckoutHistory(StackInterface<StatusChangeRecord> history,
      LocalDate fromDate, LocalDate toDate) {
    StringBuilder output = new StringBuilder();
    output.append("  Date range: ")
        .append(fromDate == null ? "All dates" : fromDate)
        .append(" to ")
        .append(toDate == null ? "All dates" : toDate)
        .append("\n\n");
    output.append(String.format("  %-8s %-22s %-22s %-24s %s%n",
        "Room", "Previous Status", "New Status", "Changed At", "Reason"));
    output.append("  ------------------------------------------------------------------------------------------------\n");

    int checkoutCount = 0;
    while (!history.isEmpty()) {
      StatusChangeRecord record = history.pop();
      if (!isCheckoutRecord(record)) {
        continue;
      }
      LocalDate changedDate = record.getChangedAt().toLocalDate();
      if (fromDate != null && changedDate.isBefore(fromDate)) {
        continue;
      }
      if (toDate != null && changedDate.isAfter(toDate)) {
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

  private String formatLateCheckoutHistory(StackInterface<StatusChangeRecord> history,
      LocalDate fromDate, LocalDate toDate) {
    StringBuilder output = new StringBuilder();
    output.append("  Date range: ")
        .append(fromDate == null ? "All dates" : fromDate)
        .append(" to ")
        .append(toDate == null ? "All dates" : toDate)
        .append("\n\n");
    output.append(String.format("  %-8s %-18s %-18s %-24s %s%n",
        "Room", "Previous", "New", "Changed At", "Reason"));
    output.append("  ------------------------------------------------------------------------------------------------\n");

    int lateCheckoutCount = 0;
    while (!history.isEmpty()) {
      StatusChangeRecord record = history.pop();
      if (!isLateCheckoutExtensionRecord(record)) {
        continue;
      }
      LocalDate changedDate = record.getChangedAt().toLocalDate();
      if (fromDate != null && changedDate.isBefore(fromDate)) {
        continue;
      }
      if (toDate != null && changedDate.isAfter(toDate)) {
        continue;
      }

      output.append(String.format("  %-8s %-18s %-18s %-24s %s%n",
          record.getRoomNumber(), record.getPreviousStatus().getLabel(),
          record.getNewStatus().getLabel(), MalaysiaTime.format(record.getChangedAt()),
          record.getReason()));
      lateCheckoutCount++;
    }

    if (lateCheckoutCount == 0) {
      output.append("  No late check-out extension records found.\n");
    }

    output.append("\n  Late check-out records shown: ").append(lateCheckoutCount);
    return output.toString();
  }

  private LocalDate[] promptForCheckoutHistoryDateRange() {
    while (true) {
      String fromInput = frontDeskUI.inputCheckoutHistoryDate("From date");
      if (fromInput.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Check-out history cancelled.");
        return null;
      }

      String toInput = frontDeskUI.inputCheckoutHistoryDate("To date  ");
      if (toInput.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Check-out history cancelled.");
        return null;
      }

      try {
        LocalDate fromDate = fromInput.isEmpty() ? null : LocalDate.parse(fromInput);
        LocalDate toDate = toInput.isEmpty() ? null : LocalDate.parse(toInput);
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
          MessageUI.displayErrorMessage("To date must be on or after the from date.");
          continue;
        }
        return new LocalDate[]{fromDate, toDate};
      } catch (DateTimeParseException ex) {
        MessageUI.displayErrorMessage("Enter dates in yyyy-MM-dd format, for example 2026-08-28.");
      }
    }
  }

  private LocalDateTime promptForNewExpectedCheckoutAt(LocalDateTime previousExpectedCheckoutAt,
      LocalDateTime checkInAt) {
    LocalDate today = MalaysiaTime.now().toLocalDate();
    LocalTime defaultTime = previousExpectedCheckoutAt == null
        ? LocalTime.NOON
        : previousExpectedCheckoutAt.toLocalTime().withSecond(0).withNano(0);

    datePrompt:
    while (true) {
      String dateInput = frontDeskUI.inputNewExpectedCheckoutDate();
      if (dateInput.equalsIgnoreCase("0")) {
        MessageUI.displayInfoMessage("Late check-out update cancelled.");
        MessageUI.pressEnterToContinue();
        return null;
      }

      LocalDate newDate;
      try {
        newDate = LocalDate.parse(dateInput);
      } catch (DateTimeParseException ex) {
        MessageUI.displayErrorMessage("Enter the date in yyyy-MM-dd format, for example 2026-08-29.");
        continue;
      }

      if (newDate.isBefore(today)) {
        MessageUI.displayErrorMessage("New expected check-out date cannot be before today ("
            + today + ").");
        continue;
      }

      while (true) {
        String timeInput = frontDeskUI.inputNewExpectedCheckoutTime(
            defaultTime.format(TIME_FORMAT));
        if (timeInput.equalsIgnoreCase("0")) {
          MessageUI.displayInfoMessage("Late check-out update cancelled.");
          MessageUI.pressEnterToContinue();
          return null;
        }

        try {
          LocalTime newTime = timeInput.isEmpty() ? defaultTime : LocalTime.parse(timeInput);
          LocalDateTime newExpectedCheckoutAt = LocalDateTime.of(newDate, newTime);
          if (checkInAt != null && !newExpectedCheckoutAt.isAfter(checkInAt)) {
            MessageUI.displayErrorMessage(
                "New expected check-out must be later than the check-in time ("
                + formatRecordedDateTime(checkInAt) + ").");
            continue datePrompt;
          }
          if (previousExpectedCheckoutAt != null
              && !newExpectedCheckoutAt.isAfter(previousExpectedCheckoutAt)) {
            MessageUI.displayErrorMessage(
                "New expected check-out must be later than the previous expected check-out.");
            continue datePrompt;
          }
          return newExpectedCheckoutAt;
        } catch (DateTimeParseException ex) {
          MessageUI.displayErrorMessage("Enter the time in HH:mm format, for example 14:30.");
        }
      }
    }
  }

  private String getRoomAvailabilityLabel(Room room) {
    if (room.getStatus() == RoomStatus.LCO) return "LATE CHECK-OUT / RESERVED";
    if (room.getStatus() == RoomStatus.OCCUPIED) return "OCCUPIED / RESERVED";
    return room.getStatus() == RoomStatus.READY_FOR_CHECK_IN
        ? "AVAILABLE FOR CHECK-IN"
        : "NOT AVAILABLE";
  }

  private ListInterface<VipPaymentRecord> findBillingRecords(String searchKey) {
    ListInterface<VipPaymentRecord> allPayments = frontDeskDAO.retrieveVipPaymentRecords();
    ListInterface<VipPaymentRecord> matches = new ArrayList<>();
    for (int index = allPayments.getNumberOfEntries(); index >= 1; index--) {
      VipPaymentRecord payment = allPayments.getEntry(index);
      if (matchesBillingSearchKey(payment, searchKey)) {
        matches.add(payment);
      }
    }
    return matches;
  }

  private boolean matchesBillingSearchKey(VipPaymentRecord payment, String searchKey) {
    return payment.getConfirmationNumber().equalsIgnoreCase(searchKey)
        || payment.getBookingId().equalsIgnoreCase(searchKey)
        || payment.getMemberId().equalsIgnoreCase(searchKey);
  }

  private boolean isValidBillingSearchKey(String searchKey) {
    return isValidConfirmationNumber(searchKey)
        || isValidBookingId(searchKey)
        || isValidMemberId(searchKey);
  }

  private boolean isValidConfirmationNumber(String confirmationNumber) {
    return confirmationNumber.matches("[0-9]{8}");
  }

  private boolean isValidBookingId(String bookingId) {
    return bookingId.matches("VIP-[0-9]{4,}");
  }

  private VipCheckInRecord findCheckInRecord(VipPaymentRecord payment,
      ListInterface<VipCheckInRecord> checkInRecords) {
    for (int index = checkInRecords.getNumberOfEntries(); index >= 1; index--) {
      VipCheckInRecord record = checkInRecords.getEntry(index);
      if (record.getConfirmationNumber().equalsIgnoreCase(payment.getConfirmationNumber())
          || record.getBookingId().equalsIgnoreCase(payment.getBookingId())) {
        return record;
      }
    }
    return null;
  }

  private String formatBillingMember(String memberId, RewardsMember memberRecord) {
    String memberName = memberRecord == null ? "Unknown member" : memberRecord.getName();
    return memberName + " (" + memberId + ")";
  }

  private void appendBillingLine(StringBuilder output, String label, String value) {
    output.append(String.format("  %-18s : %s%n", label, formatOptionalText(value)));
  }

  private String formatMoney(double amount) {
    return String.format("RM %.2f", amount);
  }

  private boolean isActiveGuestRoom(RoomStatus status) {
    return status == RoomStatus.OCCUPIED || status == RoomStatus.LCO;
  }

  private String getGuestNameForRoom(Room room) {
    String memberId = room.getOccupantMemberId();
    if (memberId == null || memberId.trim().isEmpty()) return "-";
    RewardsMember memberRecord = memberSearchTree.search(memberId.trim().toUpperCase());
    return memberRecord == null ? "Unknown member" : memberRecord.getName();
  }

  private String formatDuplicateActiveStayWarnings(ListInterface<Room> roomList) {
    StringBuilder warnings = new StringBuilder();
    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      String memberId = room.getOccupantMemberId();
      if (!isActiveGuestRoom(room.getStatus()) || memberId == null
          || memberId.trim().isEmpty()) {
        continue;
      }

      int activeStayCount = 0;
      StringBuilder roomNumbers = new StringBuilder();
      for (int checkIndex = 1; checkIndex <= roomList.getNumberOfEntries(); checkIndex++) {
        Room checkRoom = roomList.getEntry(checkIndex);
        String checkMemberId = checkRoom.getOccupantMemberId();
        if (isActiveGuestRoom(checkRoom.getStatus()) && checkMemberId != null
            && checkMemberId.equalsIgnoreCase(memberId)) {
          activeStayCount++;
          if (roomNumbers.length() > 0) roomNumbers.append(", ");
          roomNumbers.append(checkRoom.getRoomNumber());
        }
      }

      if (activeStayCount > 1 && !warnings.toString().contains(memberId.toUpperCase())) {
        warnings.append("  - ").append(memberId.toUpperCase())
            .append(" is linked to ").append(activeStayCount)
            .append(" active rooms: ").append(roomNumbers).append("\n");
      }
    }
    return warnings.toString();
  }

  private String formatActiveStayList(ListInterface<Room> roomList, String filter,
      LocalDate today, LocalDateTime now) {
    StringBuilder output = new StringBuilder();
    int count = 0;
    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      if (!isActiveGuestRoom(room.getStatus()) || !matchesActiveStayFilter(room, filter,
          today, now)) {
        continue;
      }

      output.append(String.format("  - %-6s %-18s expected %s%n",
          room.getRoomNumber(), room.getStatus().getLabel(),
          formatBoardDateTime(room.getExpectedCheckoutAt())));
      count++;
    }
    if (count == 0) {
      output.append("  - None\n");
    }
    return output.toString();
  }

  private boolean matchesActiveStayFilter(Room room, String filter, LocalDate today,
      LocalDateTime now) {
    if ("LCO".equals(filter)) {
      return room.getStatus() == RoomStatus.LCO;
    }
    if ("EXPECTED_TODAY".equals(filter)) {
      return room.getExpectedCheckoutAt() != null
          && room.getExpectedCheckoutAt().toLocalDate().equals(today);
    }
    if ("OVERDUE".equals(filter)) {
      return room.getExpectedCheckoutAt() != null && room.getExpectedCheckoutAt().isBefore(now);
    }
    return false;
  }

  private int countCheckoutRecordsOnDate(StackInterface<StatusChangeRecord> history,
      LocalDate date) {
    int count = 0;
    while (!history.isEmpty()) {
      StatusChangeRecord record = history.pop();
      if (isCheckoutRecord(record) && record.getChangedAt().toLocalDate().equals(date)) {
        count++;
      }
    }
    return count;
  }

  private boolean isValidMemberId(String memberId) {
    return memberId.matches("LM[0-9]{3,6}");
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
    return record.getNewStatus() == RoomStatus.DIRTY
        && (record.getPreviousStatus() == RoomStatus.OCCUPIED
            || record.getPreviousStatus() == RoomStatus.LCO
            || reason.startsWith("late check-out")
            || reason.startsWith("on-time check-out")
            || reason.contains("checked out"));
  }

  private boolean isLateCheckoutExtensionRecord(StatusChangeRecord record) {
    String reason = record.getReason() == null ? "" : record.getReason().toLowerCase();
    return record.getNewStatus() == RoomStatus.LCO
        || reason.startsWith("late check-out extension");
  }

  private RoomStayTimeline findLatestRoomStayTimeline(String roomNumber) {
    Room room = findRoom(roomNumber);
    if (room != null && room.getCheckInAt() != null) {
      return new RoomStayTimeline(room.getCheckInAt(), room.getExpectedCheckoutAt());
    }

    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    while (!history.isEmpty()) {
      StatusChangeRecord record = history.pop();
      if (record.getRoomNumber().equalsIgnoreCase(roomNumber)
          && (record.getNewStatus() == RoomStatus.OCCUPIED
              || record.getNewStatus() == RoomStatus.LCO)) {
        return new RoomStayTimeline(record.getChangedAt(),
            extractExpectedCheckoutAt(record.getReason()));
      }
    }
    return new RoomStayTimeline(null, null);
  }

  private LocalDateTime extractExpectedCheckoutAt(String reason) {
    if (reason == null) return null;
    int start = reason.indexOf(EXPECTED_CHECKOUT_MARKER);
    if (start < 0) return null;
    start += EXPECTED_CHECKOUT_MARKER.length();
    int end = reason.indexOf('|', start);
    String value = (end < 0 ? reason.substring(start) : reason.substring(start, end)).trim();
    try {
      return LocalDateTime.parse(value, CHECKOUT_DEADLINE_FORMAT);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private String formatRecordedDateTime(LocalDateTime dateTime) {
    return dateTime == null ? "Not recorded" : MalaysiaTime.format(dateTime);
  }

  private String formatBoardDateTime(LocalDateTime dateTime) {
    return dateTime == null ? "-" : dateTime.format(CHECKOUT_DEADLINE_FORMAT);
  }

  private String formatBoardText(String value) {
    return value == null || value.trim().isEmpty() ? "-" : value.trim();
  }

  private String formatOptionalText(String value) {
    return value == null || value.trim().isEmpty() ? "Not recorded" : value.trim();
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

  private Room findOccupiedRoomByMemberId(ListInterface<Room> roomList, String memberId) {
    for (int index = 1; index <= roomList.getNumberOfEntries(); index++) {
      Room room = roomList.getEntry(index);
      String occupantMemberId = room.getOccupantMemberId();
      if (isActiveGuestRoom(room.getStatus())
          && occupantMemberId != null
          && occupantMemberId.equalsIgnoreCase(memberId)) {
        return room;
      }
    }
    return null;
  }

  private void recordCheckoutStatusChange(String roomNumber, RoomStatus previousStatus,
      RoomStayTimeline timeline, String memberId, boolean lateCheckout,
      LocalDateTime actualCheckoutAt) {
    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    StackInterface<StatusChangeRecord> redoHistory = housekeepingDAO.retrieveRedoHistory();
    String checkoutType = lateCheckout ? "Late check-out" : "On-time check-out";
    String reason = checkoutType + " at Front Desk"
        + " | Member: " + formatOptionalText(memberId)
        + " | Check-in: " + formatRecordedDateTime(timeline.checkInAt)
        + " | Expected check-out: " + formatRecordedDateTime(timeline.expectedCheckoutAt)
        + " | Actual check-out: " + formatRecordedDateTime(actualCheckoutAt);
    history.push(new StatusChangeRecord(roomNumber, previousStatus, RoomStatus.DIRTY,
        reason, actualCheckoutAt));
    redoHistory.clear();
    housekeepingDAO.saveHistory(history);
    housekeepingDAO.saveRedoHistory(redoHistory);
  }

  private void recordLateCheckoutExtension(Room room, RoomStatus previousStatus,
      RewardsMember memberRecord, LocalDateTime previousExpectedCheckoutAt,
      LocalDateTime newExpectedCheckoutAt) {
    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    StackInterface<StatusChangeRecord> redoHistory = housekeepingDAO.retrieveRedoHistory();
    String reason = "Late check-out extension at Front Desk"
        + " | Member: " + memberRecord.getMemberId() + " " + memberRecord.getName()
        + " | Previous expected check-out: "
        + formatRecordedDateTime(previousExpectedCheckoutAt)
        + " | New expected check-out: " + formatRecordedDateTime(newExpectedCheckoutAt);
    history.push(new StatusChangeRecord(room.getRoomNumber(), previousStatus,
        RoomStatus.LCO, reason, MalaysiaTime.now()));
    redoHistory.clear();
    housekeepingDAO.saveHistory(history);
    housekeepingDAO.saveRedoHistory(redoHistory);
  }

  private static class RoomStayTimeline {
    private final LocalDateTime checkInAt;
    private final LocalDateTime expectedCheckoutAt;

    private RoomStayTimeline(LocalDateTime checkInAt, LocalDateTime expectedCheckoutAt) {
      this.checkInAt = checkInAt;
      this.expectedCheckoutAt = expectedCheckoutAt;
    }
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
