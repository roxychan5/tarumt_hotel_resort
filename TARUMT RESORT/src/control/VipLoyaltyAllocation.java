package control;

import adt.ArrayList;
import adt.HeapPriorityQueue;
import adt.ListInterface;
import adt.StackInterface;
import boundary.VipLoyaltyAllocationUI;
import dao.HousekeepingDAO;
import entity.LoyaltyMember;
import entity.LoyaltyTier;
import entity.RewardsMember;
import entity.Room;
import entity.RoomAllocation;
import entity.RoomStatus;
import entity.StatusChangeRecord;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import utility.ConsoleUI;
import utility.CsvUtils;
import utility.DataFiles;
import utility.MalaysiaTime;
import utility.MessageUI;
import utility.PdfReportEngine;


 //@author Heng Yi Ching//

public class VipLoyaltyAllocation {

  private static final DateTimeFormatter CHECKOUT_DEADLINE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter PAYMENT_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final SecureRandom CONFIRMATION_RANDOM = new SecureRandom();

  private final VipLoyaltyAllocationUI vipUI = new VipLoyaltyAllocationUI();
  private final LoyaltyRewardsService loyaltyRewardsService;
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  private final HeapPriorityQueue<LoyaltyMember> waitingMembers = new HeapPriorityQueue<>();
  private final ArrayList<RoomAllocation> completedAllocations = new ArrayList<>();
  private final ArrayList<LoyaltyMember> cancelledWaitingBookings = new ArrayList<>();
  private static final Path WAITING_MEMBER_FILE = DataFiles.resolve("vip_waiting_members.txt");
  private static final Path CHECKIN_HISTORY_FILE = DataFiles.resolve("vip_checkin_history.txt");
  private static final Path PAYMENT_HISTORY_FILE = DataFiles.resolve("vip_payment_history.txt");
  private int arrivalSequence;
  private int allocationSequence;

  public VipLoyaltyAllocation(LoyaltyRewardsService loyaltyRewardsService) {
    this.loyaltyRewardsService = loyaltyRewardsService;
    loadWaitingMembers();
    loadCheckInHistory();
  }

  public void runVipLoyaltyModule() {
    int choice;
    do {
      choice = vipUI.getMenuChoice();
      switch (choice) {
        case 0: MessageUI.displayInfoMessage("Returning to main menu..."); break;
        case 1: addPriorityMember(); break;
        case 2: viewNextMember(); break;
        case 3: vipUI.displayPriorityQueue(buildQueueDisplay()); pause(); break;
        case 4: allocateRoom(); break;
        case 5: viewAllocatedRoomBoard(); break;
        case 6: cancelBooking(); break;
        case 7: generateWaitingListReport(); break;
        case 8: generateAllocationPerformanceReport(); break;
        case 9: generateCancelledWaitingBookingsReport(); break;
        default: MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void addPriorityMember() {
    vipUI.displayRegisteredMembers(buildRegisteredMemberList());
    String memberId;
    RewardsMember registeredMember;
    while (true) {
      memberId = vipUI.inputMemberId();
      if (memberId.isEmpty()) {
        MessageUI.displayInfoMessage("Priority member registration cancelled.");
        return;
      }
      registeredMember = loyaltyRewardsService.getMemberById(memberId);
      if (registeredMember != null) break;

      MessageUI.displayErrorMessage(
          "This ID is not a registered loyalty member. Register the member in Loyalty & Rewards first.");
      int action = vipUI.inputMissingMemberAction();
      pause();
      if (action == 2) {
        loyaltyRewardsService.runLoyaltyRewardsModule();
        return;
      }
      vipUI.displayMenu();
    }

    if (registeredMember == null) return;

    vipUI.displayVerifiedMember(registeredMember.getMemberId(), registeredMember.getName(),
      registeredMember.getTier().toString());

    if (isMemberWaiting(memberId)) {
      MessageUI.displayErrorMessage("This member is already in the priority queue.");
    } else {
      String roomType = vipUI.inputRequestedRoomType();
      LocalDate requestedCheckInDate = vipUI.inputRequestedCheckInDate();
        LocalDate requestedCheckOutDate = vipUI.inputRequestedCheckOutDate(requestedCheckInDate);
        int numberOfNights = (int) ChronoUnit.DAYS.between(requestedCheckInDate,
          requestedCheckOutDate);
      LocalDateTime waitingStartedAt = MalaysiaTime.now();
      LoyaltyMember member = new LoyaltyMember(registeredMember, roomType, numberOfNights,
            ++arrivalSequence, "VIP-" + String.format("%04d", arrivalSequence),
            generateConfirmationNumber(), requestedCheckInDate, waitingStartedAt.toLocalDate(),
            waitingStartedAt);
        double nightlyPrice = getRoomPrice(roomType);
        double totalAmount = nightlyPrice * numberOfNights;
      vipUI.displayBookingSummary(buildBookingSummary(member));
        String paymentMethod = vipUI.inputPaymentMethod();
        String paymentId = createPaymentId();
        vipUI.displayPaymentInformation(buildPaymentInformation(member, nightlyPrice,
          totalAmount, paymentMethod, paymentId));
          savePaymentInformation(member, nightlyPrice, totalAmount, paymentMethod, paymentId);
      waitingMembers.add(member);
      saveWaitingMembers();
        MessageUI.displaySuccessMessage("Payment successful. VIP booking created for "
          + registeredMember.getName() + " (" + registeredMember.getMemberId()
          + "). The booking has been added to the priority waiting list.");
        ConsoleUI.displayDetailPanel("PRIORITY MEMBER ADDED",
          "Booking ID: " + member.getBookingId(),
          "Confirmation number: " + member.getConfirmationNumber(),
          "Member name: " + registeredMember.getName(),
          "Member ID: " + registeredMember.getMemberId(),
          "Loyalty tier: " + registeredMember.getTier(),
          "Requested room type: " + roomType,
          "Number of nights: " + numberOfNights,
          "Queue reordered automatically.");
    }
    pause();
  }

  private void viewNextMember() {
    LoyaltyMember member = waitingMembers.getFront();
    vipUI.displayNextMember(member == null ? "  No priority members are waiting."
        : buildNextMemberTable(member));
    pause();
  }

  private void cancelBooking() {
    if (waitingMembers.isEmpty()) {
      MessageUI.displayErrorMessage("No VIP booking is waiting for allocation.");
      pause();
      return;
    }

    vipUI.displayPriorityQueue(buildQueueDisplay());
    String confirmationNumber = vipUI.inputConfirmationNumber();
    if (confirmationNumber.isEmpty()) {
      MessageUI.displayInfoMessage("Booking cancellation cancelled.");
      pause();
      return;
    }

    LoyaltyMember booking = findWaitingMemberByConfirmationNumber(confirmationNumber);
    if (booking == null) {
      MessageUI.displayErrorMessage("No waiting VIP booking was found for " + confirmationNumber + ".");
      pause();
      return;
    }

    vipUI.displayBookingSummary(buildBookingSummary(booking));
    if (!vipUI.confirmBookingCancellation(confirmationNumber)) {
      MessageUI.displayInfoMessage("Booking was not cancelled.");
      pause();
      return;
    }

    if (waitingMembers.removeEntry(booking)) {
      cancelledWaitingBookings.add(booking);
      saveWaitingMembers();
      MessageUI.displaySuccessMessage("VIP booking cancelled for " + booking.getMemberName()
          + " (" + booking.getMemberId() + "). It has been recorded for the allocation report.");
    }
    pause();
  }

  private LoyaltyMember findWaitingMemberByMemberId(String memberId) {
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingMembers.getEntry(position);
      if (member.getMemberId().equalsIgnoreCase(memberId)) return member;
    }
    return null;
  }

  private LoyaltyMember findWaitingMemberByBookingId(String bookingId) {
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingMembers.getEntry(position);
      if (member.getBookingId().equalsIgnoreCase(bookingId)) return member;
    }
    return null;
  }

  private LoyaltyMember findWaitingMemberByConfirmationNumber(String confirmationNumber) {
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingMembers.getEntry(position);
      if (member.getConfirmationNumber().equalsIgnoreCase(confirmationNumber)) return member;
    }
    return null;
  }

  private void allocateRoom() {
    if (waitingMembers.isEmpty()) {
      MessageUI.displayErrorMessage("No priority member is waiting for allocation.");
      pause();
      return;
    }

    String roomType = vipUI.inputRoomTypeToAllocate();
    if (roomType == null || roomType.isEmpty()) {
      MessageUI.displayInfoMessage("Room allocation cancelled.");
      pause();
      return;
    }

    vipUI.displayPriorityQueue(buildQueueDisplay());
    LocalDate today = MalaysiaTime.now().toLocalDate();
    LoyaltyMember member = findHighestEligibleMemberForRoomType(roomType, today);
    if (member == null) {
      MessageUI.displayInfoMessage("No eligible priority guest is ready for a " + roomType
          + " room today. Check the queue or wait for the next eligible check-in date.");
      pause();
      return;
    }

    Room currentRoom = findOccupiedRoomByMemberId(member.getMemberId());
    if (currentRoom != null) {
      MessageUI.displayErrorMessage(member.getMemberName() + " (" + member.getMemberId()
          + ") is already checked in to room " + currentRoom.getRoomNumber()
          + ". Check out the current room before allocating another room.");
      pause();
      return;
    }

    LocalDateTime checkInAt = vipUI.inputCheckInAt(member.getRequestedCheckInDate());
    LocalDate checkInDate = checkInAt.toLocalDate();
    LocalDate checkOutDate = checkInDate.plusDays(member.getNumberOfNights());
    LocalDateTime expectedCheckoutAt = LocalDateTime.of(checkOutDate, LocalTime.NOON);
    Room room = reserveAvailableRoom(roomType, member.getMemberId(), checkInAt,
        expectedCheckoutAt);
    if (room == null) {
      MessageUI.displayErrorMessage("No " + roomType
          + " room is ready for check-in. Complete housekeeping first.");
      pause();
      return;
    }

    if (waitingMembers.removeEntry(member)) {
      int savedAllocationSequence = ++allocationSequence;
      completedAllocations.add(new RoomAllocation(member, room.getRoomNumber(),
        savedAllocationSequence, checkInDate, checkOutDate));
      recordCheckInStatusChange(room.getRoomNumber(), member.getMemberId(),
          checkInAt, expectedCheckoutAt);
      saveCheckInInformation(member, room, checkInAt, expectedCheckoutAt,
        savedAllocationSequence);
      saveWaitingMembers();
      vipUI.displayCheckInInformation(buildCheckInInformation(member, room,
        checkInAt, expectedCheckoutAt));
      MessageUI.displaySuccessMessage("Room " + room.getRoomNumber() + " ("
        + roomType
          + ") allocated automatically to " + member.getMemberName() + " ("
        + member.getTier() + ") for " + member.getNumberOfNights() + " night(s).\n"
        + "Selected by priority order for the requested room type.\n"
        + "Check-in time: " + MalaysiaTime.format(checkInAt)
        + " | Expected check-out: " + MalaysiaTime.format(expectedCheckoutAt));
    }
    pause();
  }

  /** Displays all completed VIP room allocations for the current system session. */
  private void viewAllocatedRoomBoard() {
    vipUI.displayAllocatedRoomBoard(buildAllocatedRoomBoard());
    pause();
  }

  // Finds the highest-priority waiting member requesting the selected room type. //
  private int findHighestPriorityMemberRequesting(String roomType) {
    int highestPriorityPosition = 0;
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingMembers.getEntry(position);
      if (member.getRequestedRoomType().equalsIgnoreCase(roomType)
          && (highestPriorityPosition == 0
          || member.compareTo(waitingMembers.getEntry(highestPriorityPosition)) > 0)) {
        highestPriorityPosition = position;
      }
    }
    return highestPriorityPosition;
  }

  private LoyaltyMember findHighestEligibleMemberForRoomType(String roomType, LocalDate today) {
    LoyaltyMember selected = null;
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingMembers.getEntry(position);
      if (!member.getRequestedRoomType().equalsIgnoreCase(roomType)) continue;
      if (member.getRequestedCheckInDate().isAfter(today)) continue;
      if (selected == null || member.compareTo(selected) > 0) {
        selected = member;
      }
    }
    return selected;
  }

  // Finds the first cleaned room of the selected type and marks it occupied. //
  private Room reserveAvailableRoom(String roomType, String memberId, LocalDateTime checkInAt,
      LocalDateTime expectedCheckoutAt) {
    ListInterface<Room> rooms = housekeepingDAO.retrieveRooms();
    for (int position = 1; position <= rooms.getNumberOfEntries(); position++) {
      Room room = rooms.getEntry(position);
      if (room.getRoomType().equalsIgnoreCase(roomType)
          && room.getStatus() == RoomStatus.READY_FOR_CHECK_IN) {
        room.setStatus(RoomStatus.OCCUPIED);
        room.setCheckInAt(checkInAt);
        room.setExpectedCheckoutAt(expectedCheckoutAt);
        room.setOccupantMemberId(memberId);
        housekeepingDAO.saveRooms(rooms);
        return room;
      }
    }
    return null;
  }

  private Room findOccupiedRoomByMemberId(String memberId) {
    ListInterface<Room> rooms = housekeepingDAO.retrieveRooms();
    for (int position = 1; position <= rooms.getNumberOfEntries(); position++) {
      Room room = rooms.getEntry(position);
      String occupantMemberId = room.getOccupantMemberId();
      if ((room.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.LCO)
          && occupantMemberId != null
          && occupantMemberId.equalsIgnoreCase(memberId)) {
        return room;
      }
    }
    return null;
  }

  private void recordCheckInStatusChange(String roomNumber, String memberId,
      LocalDateTime checkInAt, LocalDateTime expectedCheckoutAt) {
    StackInterface<StatusChangeRecord> history = housekeepingDAO.retrieveHistory();
    StackInterface<StatusChangeRecord> redoHistory = housekeepingDAO.retrieveRedoHistory();
    history.push(new StatusChangeRecord(roomNumber, RoomStatus.READY_FOR_CHECK_IN,
        RoomStatus.OCCUPIED, "Member checked in by VIP Allocation | Member: "
        + memberId + " | Expected check-out: "
        + expectedCheckoutAt.format(CHECKOUT_DEADLINE_FORMAT), checkInAt));
    redoHistory.clear();
    housekeepingDAO.saveHistory(history);
    housekeepingDAO.saveRedoHistory(redoHistory);
  }

  // Report 1: searches the queue and filters by tier and room type. //
  private void generateWaitingListReport() {
    int minimumTier = vipUI.inputMinimumTier();
    String roomTypeFilter = vipUI.inputRoomTypeFilter();
    LoyaltyTier tier = LoyaltyTier.fromPriority(minimumTier);
    if (tier == null) {
      MessageUI.displayErrorMessage("Minimum tier must be from 1 to 5.");
      pause();
      return;
    }
    String report = "Filters: Tier " + tier + " or above; Room Type: "
        + (roomTypeFilter.isEmpty() ? "All" : roomTypeFilter) + "\n\n"
        + String.format("%-5s %-12s %-20s %-12s %-15s %-8s %-12s %-16s%n",
          "Rank", "Member ID", "Member", "Tier", "Requested Room", "Nights",
          "Wait Started", "Waiting Time")
        + "----------------------------------------------------------------------------------------------------\n";
    int matches = 0;
    List<LoyaltyMember> matchingMembers = new java.util.ArrayList<>();
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingMembers.getEntry(position);
      if (member.getTier().getPriority() >= tier.getPriority()
          && (roomTypeFilter.isEmpty() || member.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter))) {
        report += String.format("%-5d %-12s %-20s %-12s %-15s %-8d %-12s %-16s%n", position,
          member.getMemberId(), member.getMemberName(), member.getTier(), member.getRequestedRoomType(),
          member.getNumberOfNights(), member.getWaitingSince(),
          formatWaitingDuration(member.getWaitingStartedAt()));
        matchingMembers.add(member);
        matches++;
      }
    }
    report += matches == 0 ? "No waiting members meet both filters.\n" : "\nMatching members: " + matches + "\n";
    vipUI.displayReport("REPORT 1: VIP PRIORITY WAITING LIST", report);
    if (vipUI.confirmPdfExport()) {
      exportWaitingListReportToPdf(matchingMembers, tier, roomTypeFilter);
    }
    pause();
  }

  // Report 2: filters allocations, then insertion-sorts them by tier and allocation sequence. //
  private void generateAllocationPerformanceReport() {
    int minimumTier = vipUI.inputMinimumTier();
    String roomTypeFilter = vipUI.inputRoomTypeFilter();
    LoyaltyTier tier = LoyaltyTier.fromPriority(minimumTier);
    if (tier == null) {
      MessageUI.displayErrorMessage("Minimum tier must be from 1 to 5.");
      pause();
      return;
    }
    RoomAllocation[] filtered = new RoomAllocation[completedAllocations.getNumberOfEntries()];
    int count = 0;
    for (int position = 1; position <= completedAllocations.getNumberOfEntries(); position++) {
      RoomAllocation allocation = completedAllocations.getEntry(position);
      LoyaltyMember member = allocation.getMember();
      if (member.getTier().getPriority() >= tier.getPriority()
          && (roomTypeFilter.isEmpty() || member.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter))) {
        filtered[count++] = allocation;
      }
    }
    insertionSortAllocations(filtered, count);
    List<RoomAllocation> matchingAllocations = new java.util.ArrayList<>();
    for (int index = 0; index < count; index++) matchingAllocations.add(filtered[index]);

    String report = "Filters: Tier " + tier + " or above; Room Type: "
        + (roomTypeFilter.isEmpty() ? "All" : roomTypeFilter) + "\n\n"
        + String.format("%-8s %-12s %-20s %-12s %-12s %-15s %-8s%n", "Order", "Room", "Member", "Tier", "Member ID", "Requested Room", "Nights")
        + "-----------------------------------------------------------------------------------------\n";
    for (int index = 0; index < count; index++) {
      LoyaltyMember member = filtered[index].getMember();
      report += String.format("%-8d %-12s %-20s %-12s %-12s %-15s %-8d%n",
          filtered[index].getAllocationSequence(), filtered[index].getRoomNumber(), member.getMemberName(),
          member.getTier(), member.getMemberId(), member.getRequestedRoomType(), member.getNumberOfNights());
    }
    if (count == 0) report += "No completed allocations meet both filters.\n";

    report += "\nCompleted allocations matching filters: " + count
        + "\nPriority members still waiting: " + waitingMembers.getNumberOfEntries()
        + "\nTotal completed allocations this cycle: " + completedAllocations.getNumberOfEntries() + "\n";
    vipUI.displayReport("REPORT 2: VIP ALLOCATION PERFORMANCE", report);
    if (vipUI.confirmPdfExport()) {
      exportAllocationPerformanceReportToPdf(matchingAllocations, tier, roomTypeFilter);
    }
    pause();
  }

  private void generateCancelledWaitingBookingsReport() {
    int minimumTier = vipUI.inputMinimumTier();
    String roomTypeFilter = vipUI.inputRoomTypeFilter();
    LoyaltyTier tier = LoyaltyTier.fromPriority(minimumTier);
    if (tier == null) {
      MessageUI.displayErrorMessage("Minimum tier must be from 1 to 5.");
      pause();
      return;
    }

    List<LoyaltyMember> matchingCancelledBookings = new java.util.ArrayList<>();
    for (int position = 1; position <= cancelledWaitingBookings.getNumberOfEntries(); position++) {
      LoyaltyMember member = cancelledWaitingBookings.getEntry(position);
      if (member.getTier().getPriority() >= tier.getPriority()
          && (roomTypeFilter.isEmpty() || member.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter))) {
        matchingCancelledBookings.add(member);
      }
    }

    String report = "Filters: Tier " + tier + " or above; Room Type: "
        + (roomTypeFilter.isEmpty() ? "All" : roomTypeFilter) + "\n\n"
        + String.format("%-20s %-10s %-12s %-15s %-8s %-12s %-16s%n",
          "Member", "Tier", "Member ID", "Requested Room", "Nights", "Wait Started", "Waiting Time")
        + "-------------------------------------------------------------------------------------------\n";
    for (LoyaltyMember member : matchingCancelledBookings) {
      report += String.format("%-20s %-10s %-12s %-15s %-8d %-12s %-16s%n",
          member.getMemberName(), member.getTier(), member.getMemberId(),
          member.getRequestedRoomType(), member.getNumberOfNights(),
          member.getWaitingSince(), formatWaitingDuration(member.getWaitingStartedAt()));
    }

    if (matchingCancelledBookings.isEmpty()) {
      report += "No cancelled bookings meet both filters.\n";
    }

    report += "\nCancelled waiting bookings matching filters: " + matchingCancelledBookings.size() + "\n";
    vipUI.displayReport("REPORT 3: CANCELLED WAITING BOOKINGS", report);
    if (vipUI.confirmPdfExport()) {
      exportCancelledWaitingBookingsReportToPdf(matchingCancelledBookings, tier, roomTypeFilter);
    }
    pause();
  }

  private void exportWaitingListReportToPdf(List<LoyaltyMember> matchingMembers,
      LoyaltyTier minimumTier, String roomTypeFilter) {
    PdfReportEngine pdf = null;
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
      String outPath = outDir + File.separator + "vip_waiting_list_" + timestamp + ".pdf";
      pdf = new PdfReportEngine();
      pdf.addCoverPage("VIP Priority Waiting List", "Tier and Room Type Filtered Analysis",
          "Current business cycle", "VIP Loyalty Allocation");

      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type", "VIP Priority Waiting List", null);
      pdf.addKpiRow("Minimum Tier", minimumTier.toString(), null);
      pdf.addKpiRow("Room Type Filter", roomTypeFilter.isEmpty() ? "All" : roomTypeFilter, null);
      pdf.addKpiRow("Matching Members", String.valueOf(matchingMembers.size()),
          matchingMembers.isEmpty() ? PdfReportEngine.DANGER : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      LinkedHashMap<String, Integer> tierCounts = new LinkedHashMap<>();
      LinkedHashMap<String, Integer> roomCounts = new LinkedHashMap<>();
      for (LoyaltyMember member : matchingMembers) {
        tierCounts.merge(member.getTier().toString(), 1, Integer::sum);
        roomCounts.merge(member.getRequestedRoomType(), 1, Integer::sum);
      }
      pdf.addSectionHeading("Waiting Member Distribution");
      pdf.addKpiCards(new String[]{"Matching Members", "Tier Groups", "Room Types"},
          new String[]{String.valueOf(matchingMembers.size()), String.valueOf(tierCounts.size()),
              String.valueOf(roomCounts.size())},
          new java.awt.Color[]{PdfReportEngine.ACCENT_BLUE, PdfReportEngine.BRAND_GOLD,
              PdfReportEngine.BRAND_TEAL});
      pdf.addSpace(10);
      pdf.addBarChart("Waiting Members by Tier", tierCounts.keySet().toArray(new String[0]),
          countsToValues(tierCounts), "Members");
      if (!roomCounts.isEmpty()) {
        pdf.addSectionHeading("Requested Room Types");
        pdf.addDonutChart("Waiting Members by Room Type", roomCounts.keySet().toArray(new String[0]),
            countsToValues(roomCounts));
      }

      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed VIP Waiting List");
      String[] headers = {"Rank", "Member ID", "Member", "Tier", "Requested Room", "Nights", "Wait Started", "Waiting Time"};
      float[] widths = {28, 60, 72, 52, 70, 42, 80, 90};
      List<String[]> rows = new java.util.ArrayList<>();
      for (int index = 0; index < matchingMembers.size(); index++) {
        LoyaltyMember member = matchingMembers.get(index);
        rows.add(new String[]{String.valueOf(index + 1), member.getMemberId(), member.getMemberName(),
          member.getTier().toString(), member.getRequestedRoomType(), String.valueOf(member.getNumberOfNights()),
          member.getWaitingSince().toString(), formatWaitingDuration(member.getWaitingStartedAt())});
      }
      if (rows.isEmpty()) pdf.addBodyText("No waiting members meet both filters.", 10);
      else pdf.addTable(headers, rows, widths);
      pdf.save(outPath);
      vipUI.displayPdfExportSuccess(outPath);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    } finally {
      closePdf(pdf);
    }
  }

  private void exportAllocationPerformanceReportToPdf(List<RoomAllocation> matchingAllocations,
      LoyaltyTier minimumTier, String roomTypeFilter) {
    PdfReportEngine pdf = null;
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
      String outPath = outDir + File.separator + "vip_allocation_performance_" + timestamp + ".pdf";
      pdf = new PdfReportEngine();
      pdf.addCoverPage("VIP Allocation Performance", "Completed Room Allocation Analysis",
          "Current business cycle", "VIP Loyalty Allocation");
      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type", "VIP Allocation Performance", null);
      pdf.addKpiRow("Minimum Tier", minimumTier.toString(), null);
      pdf.addKpiRow("Room Type Filter", roomTypeFilter.isEmpty() ? "All" : roomTypeFilter, null);
      pdf.addKpiRow("Matching Allocations", String.valueOf(matchingAllocations.size()),
          matchingAllocations.isEmpty() ? PdfReportEngine.DANGER : PdfReportEngine.SUCCESS);
      pdf.addKpiRow("Members Still Waiting", String.valueOf(waitingMembers.getNumberOfEntries()),
          waitingMembers.isEmpty() ? PdfReportEngine.SUCCESS : PdfReportEngine.WARNING);
      pdf.addKpiRow("Total Completed", String.valueOf(completedAllocations.getNumberOfEntries()), null);
      pdf.addDivider();

      LinkedHashMap<String, Integer> tierCounts = new LinkedHashMap<>();
      for (RoomAllocation allocation : matchingAllocations) {
        tierCounts.merge(allocation.getMember().getTier().toString(), 1, Integer::sum);
      }
      pdf.addSectionHeading("Allocation Distribution");
      pdf.addBarChart("Completed Allocations by Tier", tierCounts.keySet().toArray(new String[0]),
          countsToValues(tierCounts), "Allocations");

      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed Completed Allocations");
      String[] headers = {"Order", "Room", "Member", "Tier", "Member ID", "Requested Room", "Nights"};
      float[] widths = {42, 55, 105, 65, 75, 105, 48};
      List<String[]> rows = new java.util.ArrayList<>();
      for (RoomAllocation allocation : matchingAllocations) {
        LoyaltyMember member = allocation.getMember();
        rows.add(new String[]{String.valueOf(allocation.getAllocationSequence()), allocation.getRoomNumber(),
            member.getMemberName(), member.getTier().toString(), member.getMemberId(),
          member.getRequestedRoomType(), String.valueOf(member.getNumberOfNights())});
      }
      if (rows.isEmpty()) pdf.addBodyText("No completed allocations meet both filters.", 10);
      else pdf.addTable(headers, rows, widths);
      pdf.save(outPath);
      vipUI.displayPdfExportSuccess(outPath);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    } finally {
      closePdf(pdf);
    }
  }

  private void exportCancelledWaitingBookingsReportToPdf(List<LoyaltyMember> matchingCancelledBookings,
      LoyaltyTier minimumTier, String roomTypeFilter) {
    PdfReportEngine pdf = null;
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
      String outPath = outDir + File.separator + "vip_cancelled_waiting_bookings_" + timestamp + ".pdf";
      pdf = new PdfReportEngine();
      pdf.addCoverPage("Cancelled Waiting Bookings", "Booking Cancellation Analysis",
          "Current business cycle", "VIP Loyalty Allocation");
      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type", "Cancelled Waiting Bookings", null);
      pdf.addKpiRow("Minimum Tier", minimumTier.toString(), null);
      pdf.addKpiRow("Room Type Filter", roomTypeFilter.isEmpty() ? "All" : roomTypeFilter, null);
      pdf.addKpiRow("Matching Cancellations", String.valueOf(matchingCancelledBookings.size()),
          matchingCancelledBookings.isEmpty() ? PdfReportEngine.SUCCESS : PdfReportEngine.WARNING);
      pdf.addDivider();

      pdf.beginContentPage();
      pdf.addSectionHeading("Cancelled Waiting Bookings");
      String[] cancelHeaders = {"Member", "Tier", "Member ID", "Requested Room", "Nights", "Wait Started", "Waiting Time"};
      float[] cancelWidths = {80, 52, 70, 72, 38, 78, 90};
      List<String[]> cancelRows = new java.util.ArrayList<>();
      for (LoyaltyMember member : matchingCancelledBookings) {
        cancelRows.add(new String[]{member.getMemberName(), member.getTier().toString(),
            member.getMemberId(), member.getRequestedRoomType(),
            String.valueOf(member.getNumberOfNights()), member.getWaitingSince().toString(),
            formatWaitingDuration(member.getWaitingStartedAt())});
      }
      if (cancelRows.isEmpty()) pdf.addBodyText("No cancelled bookings meet both filters.", 10);
      else pdf.addTable(cancelHeaders, cancelRows, cancelWidths);
      pdf.save(outPath);
      vipUI.displayPdfExportSuccess(outPath);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    } finally {
      closePdf(pdf);
    }
  }

  private double[] countsToValues(LinkedHashMap<String, Integer> counts) {
    double[] values = new double[counts.size()];
    int index = 0;
    for (Integer count : counts.values()) values[index++] = count;
    return values;
  }

  private void closePdf(PdfReportEngine pdf) {
    if (pdf != null) {
      try {
        pdf.close();
      } catch (IOException ignored) {
        // Cleanup failure does not change the export result.
      }
    }
  }

  private void loadWaitingMembers() {
    if (!Files.exists(WAITING_MEMBER_FILE)) return;
    try {
      for (String line : Files.readAllLines(WAITING_MEMBER_FILE, StandardCharsets.UTF_8)) {
        if (line.trim().isEmpty() || line.startsWith("bookingId,")) continue;
        String[] fields = CsvUtils.parse(line);
        if (fields.length != 4 && fields.length != 5 && fields.length != 6
          && fields.length != 7 && fields.length != 8 && fields.length != 9 && fields.length != 10) continue;
        boolean hasBookingId = fields.length >= 8;
        boolean hasConfirmationNumber = fields.length == 9 || fields.length == 10;
        int memberIdIndex = hasConfirmationNumber ? 2 : hasBookingId ? 1 : 0;
        RewardsMember registeredMember = loyaltyRewardsService.getMemberById(fields[memberIdIndex]);
        if (registeredMember == null) continue;
        int roomTypeIndex = hasBookingId ? (hasConfirmationNumber ? 4 : 3)
          : fields.length >= 5 ? 2 : 1;
        int nightsIndex = roomTypeIndex + 1;
        int sequenceIndex = roomTypeIndex + 2;
        String roomType = fields[roomTypeIndex];
        int numberOfNights = Integer.parseInt(fields[nightsIndex]);
        int savedArrivalSequence = Integer.parseInt(fields[sequenceIndex]);
        LocalDate requestedCheckInDate = hasConfirmationNumber
          ? LocalDate.parse(fields[7]) : hasBookingId ? LocalDate.parse(fields[6])
          : fields.length == 7 ? LocalDate.parse(fields[5]) : MalaysiaTime.now().toLocalDate();
        LocalDate waitingSince = hasConfirmationNumber
          ? LocalDate.parse(fields[8]) : hasBookingId ? LocalDate.parse(fields[7])
          : fields.length == 7 ? LocalDate.parse(fields[6])
          : fields.length == 6 ? LocalDate.parse(fields[5]) : MalaysiaTime.now().toLocalDate();
        LocalDateTime waitingStartedAt = hasConfirmationNumber && fields.length >= 10
          ? LocalDateTime.parse(fields[9])
          : waitingSince.atStartOfDay();
        String bookingId = hasBookingId ? fields[0]
            : "VIP-" + String.format("%04d", savedArrivalSequence);
        String confirmationNumber = hasConfirmationNumber && fields[1].matches("[0-9]{8}")
          ? fields[1] : String.format("%08d", savedArrivalSequence);
        waitingMembers.add(new LoyaltyMember(registeredMember, roomType, numberOfNights,
          savedArrivalSequence, bookingId, confirmationNumber, requestedCheckInDate,
          waitingSince, waitingStartedAt));
        arrivalSequence = Math.max(arrivalSequence, savedArrivalSequence);
      }
    } catch (IOException | IllegalArgumentException ex) {
      MessageUI.displayErrorMessage("Could not load VIP waiting members: " + ex.getMessage());
    }
  }

  private void loadCheckInHistory() {
    if (!Files.exists(CHECKIN_HISTORY_FILE)) return;
    try {
      for (String line : Files.readAllLines(CHECKIN_HISTORY_FILE, StandardCharsets.UTF_8)) {
        if (line.trim().isEmpty() || line.startsWith("bookingId,")) continue;
        String[] fields = CsvUtils.parse(line);
        if (fields.length != 11 && fields.length != 12) continue;
        boolean hasConfirmationNumber = fields.length == 12;
        int memberIdIndex = hasConfirmationNumber ? 2 : 1;
        RewardsMember registeredMember = loyaltyRewardsService.getMemberById(fields[memberIdIndex]);
        if (registeredMember == null) continue;
        int roomTypeIndex = hasConfirmationNumber ? 4 : 3;
        int nights = Integer.parseInt(fields[roomTypeIndex + 1]);
        int sequence = Integer.parseInt(fields[roomTypeIndex + 2]);
        String confirmationNumber = hasConfirmationNumber ? fields[1]
          : String.format("%08d", sequence);
        int requestedDateIndex = roomTypeIndex + 3;
        LoyaltyMember member = new LoyaltyMember(registeredMember, fields[roomTypeIndex], nights, sequence,
          fields[0], confirmationNumber, LocalDate.parse(fields[requestedDateIndex]),
          LocalDate.parse(fields[requestedDateIndex + 1]));
        completedAllocations.add(new RoomAllocation(member, fields[requestedDateIndex + 2], sequence,
          LocalDateTime.parse(fields[requestedDateIndex + 3]).toLocalDate(),
          LocalDateTime.parse(fields[requestedDateIndex + 4]).toLocalDate()));
        allocationSequence = Math.max(allocationSequence, sequence);
      }
    } catch (IOException | IllegalArgumentException ex) {
      MessageUI.displayErrorMessage("Could not load VIP check-in history: " + ex.getMessage());
    }
  }

  private void saveCheckInInformation(LoyaltyMember member, Room room,
      LocalDateTime checkInAt, LocalDateTime expectedCheckoutAt, int sequence) {
    try {
      Files.createDirectories(CHECKIN_HISTORY_FILE.getParent());
      if (!Files.exists(CHECKIN_HISTORY_FILE)) {
        Files.write(CHECKIN_HISTORY_FILE,
          (CsvUtils.row("bookingId", "confirmationNumber", "memberId", "memberName", "roomType",
              "nights", "allocationSequence", "requestedCheckInDate", "waitingSince", "roomNumber",
              "checkInAt", "expectedCheckoutAt") + "\n")
            .getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      }
          String record = CsvUtils.row(clean(member.getBookingId()),
          clean(member.getConfirmationNumber()), clean(member.getMemberId()), clean(member.getMemberName()),
          clean(member.getRequestedRoomType()),
          String.valueOf(member.getNumberOfNights()), String.valueOf(sequence),
          member.getRequestedCheckInDate().toString(), member.getWaitingSince().toString(),
          clean(room.getRoomNumber()), checkInAt.toString(), expectedCheckoutAt.toString()) + "\n";
      Files.write(CHECKIN_HISTORY_FILE, record.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("Could not save VIP check-in history: " + ex.getMessage());
    }
  }

  private void savePaymentInformation(LoyaltyMember member, double nightlyPrice,
      double totalAmount, String paymentMethod, String paymentId) {
    try {
      Files.createDirectories(PAYMENT_HISTORY_FILE.getParent());
      if (!Files.exists(PAYMENT_HISTORY_FILE)) {
        Files.write(PAYMENT_HISTORY_FILE,
          (CsvUtils.row("paymentId", "bookingId", "confirmationNumber", "memberId", "roomType",
              "checkInDate", "checkOutDate", "nights", "pricePerNight", "totalAmount",
              "paymentMethod", "paidAt") + "\n")
            .getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
      }
        String record = CsvUtils.row(paymentId, member.getBookingId(),
          member.getConfirmationNumber(), member.getMemberId(),
          clean(member.getRequestedRoomType()), member.getRequestedCheckInDate().toString(),
          member.getRequestedCheckInDate().plusDays(member.getNumberOfNights()).toString(),
          String.valueOf(member.getNumberOfNights()), String.format("%.2f", nightlyPrice),
          String.format("%.2f", totalAmount), clean(paymentMethod), MalaysiaTime.now().toString()) + "\n";
      Files.write(PAYMENT_HISTORY_FILE, record.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("Could not save VIP payment history: " + ex.getMessage());
    }
  }

  private void saveWaitingMembers() {
    try {
      Files.createDirectories(WAITING_MEMBER_FILE.getParent());
        StringBuilder contents = new StringBuilder(CsvUtils.row("bookingId", "confirmationNumber",
          "memberId", "memberName", "roomType", "nights", "arrivalSequence",
          "requestedCheckInDate", "waitingSince", "waitingStartedAt")).append('\n');
      for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
        LoyaltyMember member = waitingMembers.getEntry(position);
        contents.append(CsvUtils.row(clean(member.getBookingId()), clean(member.getConfirmationNumber()),
            clean(member.getMemberId()), clean(member.getMemberName()), clean(member.getRequestedRoomType()),
            String.valueOf(member.getNumberOfNights()), String.valueOf(member.getArrivalSequence()),
            String.valueOf(member.getRequestedCheckInDate()), String.valueOf(member.getWaitingSince()),
            String.valueOf(member.getWaitingStartedAt())))
            .append('\n');
      }
      Files.write(WAITING_MEMBER_FILE, contents.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("Could not save VIP waiting members: " + ex.getMessage());
    }
  }

  private String clean(String value) {
    return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
  }

  private void insertionSortAllocations(RoomAllocation[] entries, int length) {
    for (int index = 1; index < length; index++) {
      RoomAllocation current = entries[index];
      int position = index - 1;
      while (position >= 0 && comesBefore(current, entries[position])) {
        entries[position + 1] = entries[position--];
      }
      entries[position + 1] = current;
    }
  }

  private boolean comesBefore(RoomAllocation first, RoomAllocation second) {
    int firstTier = first.getMember().getTier().getPriority();
    int secondTier = second.getMember().getTier().getPriority();
    return firstTier > secondTier || (firstTier == secondTier
        && first.getAllocationSequence() < second.getAllocationSequence());
  }

  private boolean isMemberWaiting(String memberId) {
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      if (waitingMembers.getEntry(position).getMemberId().equalsIgnoreCase(memberId)) return true;
    }
    return false;
  }

  private String buildQueueDisplay() {
    if (waitingMembers.isEmpty()) return "  No priority members are waiting.";
    LoyaltyMember[] sortedMembers = new LoyaltyMember[waitingMembers.getNumberOfEntries()];
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      sortedMembers[position - 1] = waitingMembers.getEntry(position);
    }
    sortPriorityMembers(sortedMembers);

    String output = String.format("%-12s %-12s %-6s %-12s %-20s %-11s %-15s %-8s %-12s %-12s %-18s%n",
        "Booking ID", "Confirm No.", "Rank", "Member ID", "Member", "Tier", "Room Type",
        "Nights", "Check-In", "Waiting Since", "Waiting Time")
      + "------------------------------------------------------------------------------------------------------------------------------------------\n";
    for (int position = 0; position < sortedMembers.length; position++) {
      output += formatMember(sortedMembers[position], position + 1) + "\n";
    }
    return output;
  }

  /** Builds the stay board from allocations in their completed order. */
  private String buildAllocatedRoomBoard() {
    if (completedAllocations.isEmpty()) {
      return "  No VIP rooms have been allocated in this session.";
    }

    StringBuilder board = new StringBuilder();
    board.append(String.format("%-7s %-10s %-10s %-8s %-12s %-20s %-11s %-12s %-12s %-6s%n",
      "Order", "Booking ID", "Confirm No.", "Room", "Member ID", "Member", "Tier",
      "Check-In", "Check-Out", "Nights"));
    board.append("----------------------------------------------------------------------------------------------------------------\n");
    for (int position = 1; position <= completedAllocations.getNumberOfEntries(); position++) {
      RoomAllocation allocation = completedAllocations.getEntry(position);
      LoyaltyMember member = allocation.getMember();
        board.append(String.format("%-7d %-10s %-10s %-8s %-12s %-20s %-11s %-12s %-12s %-6d%n",
          allocation.getAllocationSequence(), member.getBookingId(), member.getConfirmationNumber(),
          allocation.getRoomNumber(), member.getMemberId(), member.getMemberName(), member.getTier(),
          allocation.getCheckInDate(), allocation.getCheckOutDate(), member.getNumberOfNights()));
    }
    board.append("\nTotal allocated rooms: ").append(completedAllocations.getNumberOfEntries());
    return board.toString();
  }

  /** Builds a member reference list so staff can choose an ID for the priority queue. */
  private String buildRegisteredMemberList() {
    RewardsMember[] members = loyaltyRewardsService.getRegisteredMembers();
    if (members.length == 0) {
      return "  No loyalty members are registered. Enter 0 to cancel, then register a member in Loyalty & Rewards.";
    }

    for (int index = 1; index < members.length; index++) {
      RewardsMember current = members[index];
      int previous = index - 1;
      while (previous >= 0
          && members[previous].getMemberId().compareToIgnoreCase(current.getMemberId()) > 0) {
        members[previous + 1] = members[previous];
        previous--;
      }
      members[previous + 1] = current;
    }

    StringBuilder list = new StringBuilder();
    list.append(String.format("%-12s %-24s %-12s %-8s%n",
        "Member ID", "Name", "Tier", "Points"));
    list.append("--------------------------------------------------------------\n");
    for (RewardsMember member : members) {
      list.append(String.format("%-12s %-24s %-12s %-8d%n",
          member.getMemberId(), member.getName(), member.getTier(), member.getPoints()));
    }
    list.append("\nTotal registered members: ").append(members.length)
        .append("\nEnter a Member ID below, or 0 to cancel.");
    return list.toString();
  }

  private void sortPriorityMembers(LoyaltyMember[] entries) {
    for (int index = 1; index < entries.length; index++) {
      LoyaltyMember current = entries[index];
      int position = index - 1;
      while (position >= 0 && current.compareTo(entries[position]) > 0) {
        entries[position + 1] = entries[position--];
      }
      entries[position + 1] = current;
    }
  }

  private String buildNextMemberTable(LoyaltyMember member) {
    return String.format("%-12s %-12s %-12s %-20s %-11s %-15s %-8s %-12s %-18s%n",
        "Booking ID", "Confirm No.", "Member ID", "Member", "Tier", "Room Type",
        "Nights", "Check-In", "Waiting Time")
      + "-------------------------------------------------------------------------------------------------------------------\n"
      + String.format("%-12s %-12s %-12s %-20s %-11s %-15s %-8d %-12s %-18s%n",
          member.getBookingId(), member.getConfirmationNumber(), member.getMemberId(),
          member.getMemberName(), member.getTier(), member.getRequestedRoomType(),
          member.getNumberOfNights(), member.getRequestedCheckInDate(),
          formatWaitingDuration(member.getWaitingStartedAt()));
  }

  private String formatMember(LoyaltyMember member, int rank) {
    return String.format("%-12s %-12s %-6d %-12s %-20s %-11s %-15s %-8d %-12s %-12s %-18s",
      member.getBookingId(), member.getConfirmationNumber(), rank, member.getMemberId(),
      member.getMemberName(), member.getTier(), member.getRequestedRoomType(),
      member.getNumberOfNights(), member.getRequestedCheckInDate(),
      member.getWaitingSince(), formatWaitingDuration(member.getWaitingStartedAt()));
  }

  private String formatWaitingDuration(LocalDateTime waitingStartedAt) {
    Duration waitingDuration = Duration.between(waitingStartedAt, MalaysiaTime.now());
    long waitingHours = Math.max(0, waitingDuration.toHours());
    long waitingDays = waitingHours / 24;
    long remainingHours = waitingHours % 24;
    return waitingDays + " day(s) " + remainingHours + " hour(s)";
  }

  private String buildBookingSummary(LoyaltyMember member) {
    LocalDate checkOutDate = member.getRequestedCheckInDate()
        .plusDays(member.getNumberOfNights());
        return String.format("%-24s : %s%n%-24s : %s%n%-24s : %s%n%-24s : %s%n"
          + "%-24s : %s%n%-24s : %s%n%-24s : %s%n%-24s : %d night(s)%n"
          + "%-24s : %s%n",
        "Booking ID", member.getBookingId(),
        "Confirmation No.", member.getConfirmationNumber(),
        "Member", member.getMemberName() + " (" + member.getMemberId() + ")",
        "Loyalty Tier", member.getTier(),
        "Requested Room Type", member.getRequestedRoomType(),
        "Requested Check-In Date", member.getRequestedCheckInDate(),
        "Requested Check-Out Date", checkOutDate,
        "Length of Stay", member.getNumberOfNights(),
        "Booking Created", member.getWaitingSince());
  }

  private double getRoomPrice(String roomType) {
    switch (roomType.toLowerCase()) {
      case "standard": return 150.00;
      case "deluxe": return 250.00;
      case "suite": return 400.00;
      case "family": return 350.00;
      case "executive": return 550.00;
      case "presidential": return 1000.00;
      default: return 0.00;
    }
  }

  private String createPaymentId() {
    return "PAY-" + MalaysiaTime.now().format(PAYMENT_TIME_FORMAT);
  }

  private String generateConfirmationNumber() {
    while (true) {
      String confirmationNumber = String.format("%08d", CONFIRMATION_RANDOM.nextInt(100000000));
      if (!isConfirmationNumberUsed(confirmationNumber)) return confirmationNumber;
    }
  }

  private boolean isConfirmationNumberUsed(String confirmationNumber) {
    for (int position = 1; position <= waitingMembers.getNumberOfEntries(); position++) {
      if (waitingMembers.getEntry(position).getConfirmationNumber().equals(confirmationNumber)) return true;
    }
    return containsConfirmationNumber(CHECKIN_HISTORY_FILE, confirmationNumber)
        || containsConfirmationNumber(PAYMENT_HISTORY_FILE, confirmationNumber);
  }

  private boolean containsConfirmationNumber(Path file, String confirmationNumber) {
    if (!Files.exists(file)) return false;
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        String[] fields = CsvUtils.parse(line);
        if (fields.length > 1 && confirmationNumber.equals(fields[1])) return true;
      }
    } catch (IOException ignored) {
      return false;
    }
    return false;
  }

  private String buildPaymentInformation(LoyaltyMember member, double nightlyPrice,
      double totalAmount, String paymentMethod, String paymentId) {
        return String.format("%-24s : %s%n%-24s : %s%n%-24s : %s%n%-24s : %s%n"
          + "%-24s : %s%n%-24s : %d night(s)%n%-24s : RM %.2f%n"
          + "%-24s : RM %.2f%n%-24s : %s%n%-24s : %s%n",
        "Confirmation No.", member.getConfirmationNumber(),
        "Member", member.getMemberName() + " (" + member.getMemberId() + ")",
        "Room Type", member.getRequestedRoomType(),
        "Check-In Date", member.getRequestedCheckInDate(),
        "Check-Out Date", member.getRequestedCheckInDate().plusDays(member.getNumberOfNights()),
        "Length of Stay", member.getNumberOfNights(),
        "Price Per Night", nightlyPrice,
        "Total Amount", totalAmount,
        "Payment Method", paymentMethod,
        "Payment Status", "PAID - " + paymentId);
  }

        private String buildCheckInInformation(LoyaltyMember member, Room room,
        LocalDateTime checkInAt, LocalDateTime expectedCheckoutAt) {
          return String.format("%-24s : %s%n%-24s : %s%n%-24s : %s%n%-24s : %s%n"
          + "%-24s : %s%n%-24s : %s%n%-24s : %s%n%-24s : %d night(s)%n"
          + "%-24s : %s%n",
          "Booking ID", member.getBookingId(),
          "Confirmation No.", member.getConfirmationNumber(),
          "Member", member.getMemberName() + " (" + member.getMemberId() + ")",
          "Room Number", room.getRoomNumber(),
          "Room Type", room.getRoomType(),
          "Check-In", MalaysiaTime.format(checkInAt),
          "Expected Check-Out", MalaysiaTime.format(expectedCheckoutAt),
          "Length of Stay", member.getNumberOfNights(),
          "Room Status", room.getStatus().getLabel());
        }

  private void pause() { MessageUI.pressEnterToContinue(); }
}
