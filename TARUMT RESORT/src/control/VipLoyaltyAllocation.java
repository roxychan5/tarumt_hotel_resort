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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import utility.ConsoleUI;
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

  private final VipLoyaltyAllocationUI vipUI = new VipLoyaltyAllocationUI();
  private final LoyaltyRewardsService loyaltyRewardsService;
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  private final HeapPriorityQueue<LoyaltyMember> waitingGuests = new HeapPriorityQueue<>();
  private final ArrayList<RoomAllocation> completedAllocations = new ArrayList<>();
  private static final Path WAITING_GUEST_FILE = DataFiles.resolve("vip_waiting_guests.txt");
  private int arrivalSequence;
  private int allocationSequence;

  public VipLoyaltyAllocation(LoyaltyRewardsService loyaltyRewardsService) {
    this.loyaltyRewardsService = loyaltyRewardsService;
    loadWaitingGuests();
  }

  public void runVipLoyaltyModule() {
    int choice;
    do {
      choice = vipUI.getMenuChoice();
      switch (choice) {
        case 0: MessageUI.displayInfoMessage("Returning to main menu..."); break;
        case 1: addPriorityGuest(); break;
        case 2: viewNextGuest(); break;
        case 3: vipUI.displayPriorityQueue(buildQueueDisplay()); pause(); break;
        case 4: allocateRoom(); break;
        case 5: viewAllocatedRoomBoard(); break;
        case 6: cancelBooking(); break;
        case 7: generateWaitingListReport(); break;
        case 8: generateAllocationPerformanceReport(); break;
        default: MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void addPriorityGuest() {
    vipUI.displayRegisteredMembers(buildRegisteredMemberList());
    String memberId;
    RewardsMember registeredMember;
    while (true) {
      memberId = vipUI.inputMemberId();
      if (memberId.isEmpty()) {
        MessageUI.displayInfoMessage("Priority guest registration cancelled.");
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
      LoyaltyMember member = new LoyaltyMember(registeredMember, roomType, numberOfNights,
          ++arrivalSequence, requestedCheckInDate, MalaysiaTime.now().toLocalDate());
        double nightlyPrice = getRoomPrice(roomType);
        double totalAmount = nightlyPrice * numberOfNights;
      vipUI.displayBookingSummary(buildBookingSummary(member));
        String paymentMethod = vipUI.inputPaymentMethod();
        vipUI.displayPaymentInformation(buildPaymentInformation(member, nightlyPrice,
          totalAmount, paymentMethod));
      waitingGuests.add(member);
      saveWaitingGuests();
        MessageUI.displaySuccessMessage("Payment successful. VIP booking created for "
          + registeredMember.getName() + " (" + registeredMember.getMemberId()
          + "). The booking has been added to the priority waiting list.");
        ConsoleUI.displayDetailPanel("PRIORITY GUEST ADDED",
          "Member name: " + registeredMember.getName(),
          "Member ID: " + registeredMember.getMemberId(),
          "Loyalty tier: " + registeredMember.getTier(),
          "Requested room type: " + roomType,
          "Number of nights: " + numberOfNights,
          "Queue reordered automatically.");
    }
    pause();
  }

  private void viewNextGuest() {
    LoyaltyMember member = waitingGuests.getFront();
    vipUI.displayNextGuest(member == null ? "  No priority guests are waiting."
        : formatMember(member, 1));
    pause();
  }

  private void cancelBooking() {
    if (waitingGuests.isEmpty()) {
      MessageUI.displayErrorMessage("No VIP booking is waiting for allocation.");
      pause();
      return;
    }

    vipUI.displayPriorityQueue(buildQueueDisplay());
    String memberId = vipUI.inputMemberId();
    if (memberId.isEmpty()) {
      MessageUI.displayInfoMessage("Booking cancellation cancelled.");
      pause();
      return;
    }

    LoyaltyMember booking = findWaitingGuestByMemberId(memberId);
    if (booking == null) {
      MessageUI.displayErrorMessage("No waiting VIP booking was found for member ID " + memberId + ".");
      pause();
      return;
    }

    vipUI.displayBookingSummary(buildBookingSummary(booking));
    if (!vipUI.confirmBookingCancellation(memberId)) {
      MessageUI.displayInfoMessage("Booking was not cancelled.");
      pause();
      return;
    }

    if (waitingGuests.removeEntry(booking)) {
      saveWaitingGuests();
      MessageUI.displaySuccessMessage("VIP booking cancelled for " + booking.getGuestName()
          + " (" + booking.getMemberId() + ").");
    }
    pause();
  }

  private LoyaltyMember findWaitingGuestByMemberId(String memberId) {
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingGuests.getEntry(position);
      if (member.getMemberId().equalsIgnoreCase(memberId)) return member;
    }
    return null;
  }

  private void allocateRoom() {
    if (waitingGuests.isEmpty()) {
      MessageUI.displayErrorMessage("No priority guest is waiting for allocation.");
    } else {
      vipUI.displayPriorityQueue(buildQueueDisplay());
      String roomType = vipUI.inputRoomTypeToAllocate();
      if (roomType.isEmpty()) {
        MessageUI.displayInfoMessage("Room allocation cancelled.");
        return;
      }
      int queuePosition = findHighestPriorityGuestRequesting(roomType);
      if (queuePosition == 0) {
        MessageUI.displayErrorMessage("No waiting VIP member has requested a " + roomType + " room.");
      } else {
        LoyaltyMember member = waitingGuests.getEntry(queuePosition);
        Room currentRoom = findOccupiedRoomByMemberId(member.getMemberId());
        if (currentRoom != null) {
          MessageUI.displayErrorMessage(member.getGuestName() + " (" + member.getMemberId()
              + ") is already checked in to room " + currentRoom.getRoomNumber()
              + ". Check out the current room before allocating another room.");
          pause();
          return;
        }
        LocalDateTime checkInAt = vipUI.inputCheckInAt();
        LocalDate checkInDate = checkInAt.toLocalDate();
        LocalDate checkOutDate = checkInDate.plusDays(member.getNumberOfNights());
        LocalDateTime expectedCheckoutAt = LocalDateTime.of(checkOutDate, LocalTime.NOON);
        Room room = reserveAvailableRoom(roomType, member.getMemberId(), checkInAt,
            expectedCheckoutAt);
        if (room == null) {
          MessageUI.displayErrorMessage("No " + roomType
              + " room is ready for check-in. Complete housekeeping first.");
        } else {
          if (waitingGuests.removeEntry(member)) {
            completedAllocations.add(new RoomAllocation(member, room.getRoomNumber(),
                ++allocationSequence, checkInDate, checkOutDate));
            recordCheckInStatusChange(room.getRoomNumber(), member.getMemberId(),
                checkInAt, expectedCheckoutAt);
            saveWaitingGuests();
            MessageUI.displaySuccessMessage("Room " + room.getRoomNumber() + " (" + roomType
                + ") allocated automatically to " + member.getGuestName() + " ("
              + member.getTier() + ") for " + member.getNumberOfNights() + " night(s).\n"
              + "Check-in time: " + MalaysiaTime.format(checkInAt)
              + " | Expected check-out: " + MalaysiaTime.format(expectedCheckoutAt));
          }
        }
      }
    }
    pause();
  }

  /** Displays all completed VIP room allocations for the current system session. */
  private void viewAllocatedRoomBoard() {
    vipUI.displayAllocatedRoomBoard(buildAllocatedRoomBoard());
    pause();
  }

  // Finds the highest-priority waiting guest requesting the selected room type. //
  private int findHighestPriorityGuestRequesting(String roomType) {
    int highestPriorityPosition = 0;
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingGuests.getEntry(position);
      if (member.getRequestedRoomType().equalsIgnoreCase(roomType)
          && (highestPriorityPosition == 0
          || member.compareTo(waitingGuests.getEntry(highestPriorityPosition)) > 0)) {
        highestPriorityPosition = position;
      }
    }
    return highestPriorityPosition;
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
        RoomStatus.OCCUPIED, "Guest checked in by VIP Allocation | Member: "
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
        + String.format("%-5s %-12s %-20s %-12s %-15s %-8s%n", "Rank", "Member ID", "Guest", "Tier", "Requested Room", "Nights")
        + "--------------------------------------------------------------------------------\n";
    int matches = 0;
    List<LoyaltyMember> matchingGuests = new java.util.ArrayList<>();
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingGuests.getEntry(position);
      if (member.getTier().getPriority() >= tier.getPriority()
          && (roomTypeFilter.isEmpty() || member.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter))) {
        report += String.format("%-5d %-12s %-20s %-12s %-15s %-8d%n", position,
          member.getMemberId(), member.getGuestName(), member.getTier(), member.getRequestedRoomType(),
          member.getNumberOfNights());
        matchingGuests.add(member);
        matches++;
      }
    }
    report += matches == 0 ? "No waiting guests meet both filters.\n" : "\nMatching guests: " + matches + "\n";
    vipUI.displayReport("REPORT 1: VIP PRIORITY WAITING LIST", report);
    if (vipUI.confirmPdfExport()) {
      exportWaitingListReportToPdf(matchingGuests, tier, roomTypeFilter);
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
        + String.format("%-8s %-12s %-20s %-12s %-12s %-15s %-8s%n", "Order", "Room", "Guest", "Tier", "Member ID", "Requested Room", "Nights")
        + "-----------------------------------------------------------------------------------------\n";
    for (int index = 0; index < count; index++) {
      LoyaltyMember member = filtered[index].getMember();
        report += String.format("%-8d %-12s %-20s %-12s %-12s %-15s %-8d%n",
          filtered[index].getAllocationSequence(), filtered[index].getRoomNumber(), member.getGuestName(),
          member.getTier(), member.getMemberId(), member.getRequestedRoomType(), member.getNumberOfNights());
    }
    report += "\nCompleted allocations matching filters: " + count
        + "\nPriority guests still waiting: " + waitingGuests.getNumberOfEntries()
        + "\nTotal completed allocations this cycle: " + completedAllocations.getNumberOfEntries() + "\n";
    if (count == 0) report += "No completed allocations meet both filters.\n";
    vipUI.displayReport("REPORT 2: VIP ALLOCATION PERFORMANCE", report);
    if (vipUI.confirmPdfExport()) {
      exportAllocationPerformanceReportToPdf(matchingAllocations, tier, roomTypeFilter);
    }
    pause();
  }

  private void exportWaitingListReportToPdf(List<LoyaltyMember> matchingGuests,
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
      pdf.addKpiRow("Matching Guests", String.valueOf(matchingGuests.size()),
          matchingGuests.isEmpty() ? PdfReportEngine.DANGER : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      LinkedHashMap<String, Integer> tierCounts = new LinkedHashMap<>();
      LinkedHashMap<String, Integer> roomCounts = new LinkedHashMap<>();
      for (LoyaltyMember member : matchingGuests) {
        tierCounts.merge(member.getTier().toString(), 1, Integer::sum);
        roomCounts.merge(member.getRequestedRoomType(), 1, Integer::sum);
      }
      pdf.addSectionHeading("Waiting Guest Distribution");
      pdf.addKpiCards(new String[]{"Matching Guests", "Tier Groups", "Room Types"},
          new String[]{String.valueOf(matchingGuests.size()), String.valueOf(tierCounts.size()),
              String.valueOf(roomCounts.size())},
          new java.awt.Color[]{PdfReportEngine.ACCENT_BLUE, PdfReportEngine.BRAND_GOLD,
              PdfReportEngine.BRAND_TEAL});
      pdf.addSpace(10);
      pdf.addBarChart("Waiting Guests by Tier", tierCounts.keySet().toArray(new String[0]),
          countsToValues(tierCounts), "Guests");
      if (!roomCounts.isEmpty()) {
        pdf.addSectionHeading("Requested Room Types");
        pdf.addDonutChart("Waiting Guests by Room Type", roomCounts.keySet().toArray(new String[0]),
            countsToValues(roomCounts));
      }

      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed VIP Waiting List");
      String[] headers = {"Rank", "Member ID", "Guest", "Tier", "Requested Room", "Nights"};
      float[] widths = {40, 80, 120, 75, 125, 55};
      List<String[]> rows = new java.util.ArrayList<>();
      for (int index = 0; index < matchingGuests.size(); index++) {
        LoyaltyMember member = matchingGuests.get(index);
        rows.add(new String[]{String.valueOf(index + 1), member.getMemberId(), member.getGuestName(),
          member.getTier().toString(), member.getRequestedRoomType(), String.valueOf(member.getNumberOfNights())});
      }
      if (rows.isEmpty()) pdf.addBodyText("No waiting guests meet both filters.", 10);
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
      pdf.addKpiRow("Guests Still Waiting", String.valueOf(waitingGuests.getNumberOfEntries()),
          waitingGuests.isEmpty() ? PdfReportEngine.SUCCESS : PdfReportEngine.WARNING);
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
      String[] headers = {"Order", "Room", "Guest", "Tier", "Member ID", "Requested Room", "Nights"};
      float[] widths = {42, 55, 105, 65, 75, 105, 48};
      List<String[]> rows = new java.util.ArrayList<>();
      for (RoomAllocation allocation : matchingAllocations) {
        LoyaltyMember member = allocation.getMember();
        rows.add(new String[]{String.valueOf(allocation.getAllocationSequence()), allocation.getRoomNumber(),
            member.getGuestName(), member.getTier().toString(), member.getMemberId(),
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

  private void loadWaitingGuests() {
    if (!Files.exists(WAITING_GUEST_FILE)) return;
    try {
      for (String line : Files.readAllLines(WAITING_GUEST_FILE, StandardCharsets.UTF_8)) {
        if (line.trim().isEmpty() || line.startsWith("memberId\t")) continue;
        String[] fields = line.split("\\t", -1);
        if (fields.length != 4 && fields.length != 5 && fields.length != 6 && fields.length != 7) continue;
        RewardsMember registeredMember = loyaltyRewardsService.getMemberById(fields[0]);
        if (registeredMember == null) continue;
        int dataOffset = fields.length >= 5 ? 1 : 0;
        String roomType = fields[1 + dataOffset];
        int numberOfNights = Integer.parseInt(fields[2 + dataOffset]);
        int savedArrivalSequence = Integer.parseInt(fields[3 + dataOffset]);
        LocalDate requestedCheckInDate = fields.length == 7
          ? LocalDate.parse(fields[5]) : MalaysiaTime.now().toLocalDate();
        LocalDate waitingSince = fields.length == 7
          ? LocalDate.parse(fields[6])
          : fields.length == 6 ? LocalDate.parse(fields[5]) : MalaysiaTime.now().toLocalDate();
        waitingGuests.add(new LoyaltyMember(registeredMember, roomType, numberOfNights,
          savedArrivalSequence, requestedCheckInDate, waitingSince));
        arrivalSequence = Math.max(arrivalSequence, savedArrivalSequence);
      }
    } catch (IOException | IllegalArgumentException ex) {
      MessageUI.displayErrorMessage("Could not load VIP waiting guests: " + ex.getMessage());
    }
  }

  private void saveWaitingGuests() {
    try {
      Files.createDirectories(WAITING_GUEST_FILE.getParent());
        StringBuilder contents = new StringBuilder(
          "memberId\tmemberName\troomType\tnights\tarrivalSequence\trequestedCheckInDate\twaitingSince\n");
      for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
        LoyaltyMember member = waitingGuests.getEntry(position);
        contents.append(clean(member.getMemberId())).append('\t')
            .append(clean(member.getGuestName())).append('\t')
            .append(clean(member.getRequestedRoomType())).append('\t')
            .append(member.getNumberOfNights()).append('\t')
            .append(member.getArrivalSequence()).append('\t')
            .append(member.getRequestedCheckInDate()).append('\t')
            .append(member.getWaitingSince()).append('\n');
      }
      Files.write(WAITING_GUEST_FILE, contents.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("Could not save VIP waiting guests: " + ex.getMessage());
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
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      if (waitingGuests.getEntry(position).getMemberId().equalsIgnoreCase(memberId)) return true;
    }
    return false;
  }

  private String buildQueueDisplay() {
    if (waitingGuests.isEmpty()) return "  No priority guests are waiting.";
    LoyaltyMember[] sortedGuests = new LoyaltyMember[waitingGuests.getNumberOfEntries()];
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      sortedGuests[position - 1] = waitingGuests.getEntry(position);
    }
    sortPriorityGuests(sortedGuests);

    String output = String.format("%-5s %-12s %-20s %-12s %-15s %-8s %-13s %-12s%n",
        "Rank", "Member ID", "Guest", "Tier", "Requested Room", "Nights", "Waiting Since",
        "Waiting Time")
      + "------------------------------------------------------------------------------------------------------\n";
    for (int position = 0; position < sortedGuests.length; position++) {
      output += formatMember(sortedGuests[position], position + 1) + "\n";
    }
    return output;
  }

  /** Builds the stay board from allocations in their completed order. */
  private String buildAllocatedRoomBoard() {
    if (completedAllocations.isEmpty()) {
      return "  No VIP rooms have been allocated in this session.";
    }

    StringBuilder board = new StringBuilder();
    board.append(String.format("%-7s %-8s %-12s %-20s %-11s %-12s %-12s %-6s%n",
        "Order", "Room", "Member ID", "Guest", "Tier", "Check-In", "Check-Out", "Nights"));
    board.append("----------------------------------------------------------------------------------------------\n");
    for (int position = 1; position <= completedAllocations.getNumberOfEntries(); position++) {
      RoomAllocation allocation = completedAllocations.getEntry(position);
      LoyaltyMember member = allocation.getMember();
      board.append(String.format("%-7d %-8s %-12s %-20s %-11s %-12s %-12s %-6d%n",
          allocation.getAllocationSequence(), allocation.getRoomNumber(), member.getMemberId(),
          member.getGuestName(), member.getTier(), allocation.getCheckInDate(),
          allocation.getCheckOutDate(), member.getNumberOfNights()));
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

  private void sortPriorityGuests(LoyaltyMember[] entries) {
    for (int index = 1; index < entries.length; index++) {
      LoyaltyMember current = entries[index];
      int position = index - 1;
      while (position >= 0 && current.compareTo(entries[position]) > 0) {
        entries[position + 1] = entries[position--];
      }
      entries[position + 1] = current;
    }
  }

  private String formatMember(LoyaltyMember member, int rank) {
    long waitingDays = Math.max(0, ChronoUnit.DAYS.between(member.getWaitingSince(),
        MalaysiaTime.now().toLocalDate()));
    return String.format("%-5d %-12s %-20s %-12s %-15s %-8d %-13s %-12s", rank, member.getMemberId(),
      member.getGuestName(), member.getTier(), member.getRequestedRoomType(),
      member.getNumberOfNights(), member.getWaitingSince(), waitingDays + " day(s)");
  }

  private String buildBookingSummary(LoyaltyMember member) {
    LocalDate checkOutDate = member.getRequestedCheckInDate()
        .plusDays(member.getNumberOfNights());
    return String.format("%-24s : %s%n%-24s : %s%n%-24s : %s%n%-24s : %s%n"
            + "%-24s : %s%n%-24s : %d night(s)%n%-24s : %s%n",
        "Member", member.getGuestName() + " (" + member.getMemberId() + ")",
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

  private String buildPaymentInformation(LoyaltyMember member, double nightlyPrice,
      double totalAmount, String paymentMethod) {
    String paymentId = "PAY-" + MalaysiaTime.now().format(PAYMENT_TIME_FORMAT);
    return String.format("%-24s : %s%n%-24s : %s%n%-24s : %s%n%-24s : %s%n"
            + "%-24s : %d night(s)%n%-24s : RM %.2f%n%-24s : RM %.2f%n"
            + "%-24s : %s%n%-24s : %s%n",
        "Member", member.getGuestName() + " (" + member.getMemberId() + ")",
        "Room Type", member.getRequestedRoomType(),
        "Check-In Date", member.getRequestedCheckInDate(),
        "Check-Out Date", member.getRequestedCheckInDate().plusDays(member.getNumberOfNights()),
        "Length of Stay", member.getNumberOfNights(),
        "Price Per Night", nightlyPrice,
        "Total Amount", totalAmount,
        "Payment Method", paymentMethod,
        "Payment Status", "PAID - " + paymentId);
  }

  private void pause() { MessageUI.pressEnterToContinue(); }
}
