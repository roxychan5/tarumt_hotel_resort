package control;

import adt.ArrayList;
import adt.HeapPriorityQueue;
import adt.ListInterface;
import boundary.VipLoyaltyAllocationUI;
import dao.HousekeepingDAO;
import entity.LoyaltyMember;
import entity.LoyaltyTier;
import entity.RewardsMember;
import entity.Room;
import entity.RoomAllocation;
import entity.RoomStatus;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import utility.MalaysiaTime;
import utility.MessageUI;
import utility.PdfReportEngine;

/**
 * Controls VIP room allocation using a self-ordering priority queue.
 * Reports combine sequential searching, multiple filters and insertion sort.
 *
 * @author Replace with your name
 */
public class VipLoyaltyAllocation {

  private final VipLoyaltyAllocationUI vipUI = new VipLoyaltyAllocationUI();
  private final LoyaltyRewardsService loyaltyRewardsService;
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  private final HeapPriorityQueue<LoyaltyMember> waitingGuests = new HeapPriorityQueue<>();
  private final ArrayList<RoomAllocation> completedAllocations = new ArrayList<>();
  private int arrivalSequence;
  private int allocationSequence;

  public VipLoyaltyAllocation(LoyaltyRewardsService loyaltyRewardsService) {
    this.loyaltyRewardsService = loyaltyRewardsService;
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
        case 5: generateWaitingListReport(); break;
        case 6: generateAllocationPerformanceReport(); break;
        default: MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void addPriorityGuest() {
    String memberId;
    RewardsMember registeredMember;
    while (true) {
      memberId = vipUI.inputMemberId();
      registeredMember = loyaltyRewardsService.getMemberById(memberId);
      if (!memberId.isEmpty() && registeredMember != null) break;

      MessageUI.displayErrorMessage(memberId.isEmpty()
          ? "Member ID is required."
          : "This ID is not a registered loyalty member. Register the member in Loyalty & Rewards first.");
      int action = vipUI.inputMissingMemberAction();
      pause();
      if (action == 2) {
        loyaltyRewardsService.runLoyaltyRewardsModule();
        return;
      }
      vipUI.displayMenu();
    }

    if (registeredMember == null) return;

    if (isMemberWaiting(memberId)) {
      MessageUI.displayErrorMessage("This member is already in the priority queue.");
    } else {
      String roomType = vipUI.inputRequestedRoomType();
      LoyaltyMember member = new LoyaltyMember(registeredMember, roomType, ++arrivalSequence);
      waitingGuests.add(member);
      MessageUI.displaySuccessMessage(registeredMember.getTier() + " member added. Queue reordered automatically.");
    }
    pause();
  }

  private void viewNextGuest() {
    LoyaltyMember member = waitingGuests.getFront();
    vipUI.displayNextGuest(member == null ? "  No priority guests are waiting."
        : formatMember(member, 1));
    pause();
  }

  private void allocateRoom() {
    if (waitingGuests.isEmpty()) {
      MessageUI.displayErrorMessage("No priority guest is waiting for allocation.");
    } else {
      String roomType = vipUI.inputRoomTypeToAllocate();
      int queuePosition = findHighestPriorityGuestRequesting(roomType);
      if (queuePosition == 0) {
        MessageUI.displayErrorMessage("No waiting VIP member has requested a " + roomType + " room.");
      } else {
        Room room = reserveAvailableRoom(roomType);
        if (room == null) {
          MessageUI.displayErrorMessage("No " + roomType
              + " room is ready for check-in. Complete housekeeping first.");
        } else {
          LoyaltyMember member = waitingGuests.getEntry(queuePosition);
          if (waitingGuests.removeEntry(member)) {
            completedAllocations.add(new RoomAllocation(member, room.getRoomNumber(), ++allocationSequence));
            MessageUI.displaySuccessMessage("Room " + room.getRoomNumber() + " (" + roomType
                + ") allocated automatically to " + member.getGuestName() + " ("
                + member.getTier() + ").");
          }
        }
      }
    }
    pause();
  }

  /** Finds the highest-priority waiting guest requesting the selected room type. */
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

  /** Finds the first cleaned room of the selected type and marks it occupied. */
  private Room reserveAvailableRoom(String roomType) {
    ListInterface<Room> rooms = housekeepingDAO.retrieveRooms();
    for (int position = 1; position <= rooms.getNumberOfEntries(); position++) {
      Room room = rooms.getEntry(position);
      if (room.getRoomType().equalsIgnoreCase(roomType)
          && room.getStatus() == RoomStatus.READY_FOR_CHECK_IN) {
        room.setStatus(RoomStatus.OCCUPIED);
        housekeepingDAO.saveRooms(rooms);
        return room;
      }
    }
    return null;
  }

  /** Report 1: searches the queue and filters by tier and room type. */
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
        + String.format("%-5s %-12s %-20s %-12s %-15s%n", "Rank", "Member ID", "Guest", "Tier", "Requested Room")
        + "----------------------------------------------------------------------------\n";
    int matches = 0;
    List<LoyaltyMember> matchingGuests = new java.util.ArrayList<>();
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingGuests.getEntry(position);
      if (member.getTier().getPriority() >= tier.getPriority()
          && (roomTypeFilter.isEmpty() || member.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter))) {
        report += String.format("%-5d %-12s %-20s %-12s %-15s%n", position,
            member.getMemberId(), member.getGuestName(), member.getTier(), member.getRequestedRoomType());
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

  /** Report 2: filters allocations, then insertion-sorts them by tier and allocation sequence. */
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
        + String.format("%-8s %-12s %-20s %-12s %-12s %-15s%n", "Order", "Room", "Guest", "Tier", "Member ID", "Requested Room")
        + "-------------------------------------------------------------------------------------\n";
    for (int index = 0; index < count; index++) {
      LoyaltyMember member = filtered[index].getMember();
      report += String.format("%-8d %-12s %-20s %-12s %-12s %-15s%n",
          filtered[index].getAllocationSequence(), filtered[index].getRoomNumber(), member.getGuestName(),
          member.getTier(), member.getMemberId(), member.getRequestedRoomType());
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
      String[] headers = {"Rank", "Member ID", "Guest", "Tier", "Requested Room"};
      float[] widths = {45, 85, 155, 90, 125};
      List<String[]> rows = new java.util.ArrayList<>();
      for (int index = 0; index < matchingGuests.size(); index++) {
        LoyaltyMember member = matchingGuests.get(index);
        rows.add(new String[]{String.valueOf(index + 1), member.getMemberId(), member.getGuestName(),
            member.getTier().toString(), member.getRequestedRoomType()});
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
      String[] headers = {"Order", "Room", "Guest", "Tier", "Member ID", "Requested Room"};
      float[] widths = {50, 60, 145, 80, 90, 115};
      List<String[]> rows = new java.util.ArrayList<>();
      for (RoomAllocation allocation : matchingAllocations) {
        LoyaltyMember member = allocation.getMember();
        rows.add(new String[]{String.valueOf(allocation.getAllocationSequence()), allocation.getRoomNumber(),
            member.getGuestName(), member.getTier().toString(), member.getMemberId(),
            member.getRequestedRoomType()});
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

    String output = String.format("%-5s %-12s %-20s %-12s %-15s%n", "Rank", "Member ID", "Guest", "Tier", "Requested Room")
        + "----------------------------------------------------------------------------\n";
    for (int position = 0; position < sortedGuests.length; position++) {
      output += formatMember(sortedGuests[position], position + 1) + "\n";
    }
    return output;
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
    return String.format("%-5d %-12s %-20s %-12s %-15s", rank, member.getMemberId(),
        member.getGuestName(), member.getTier(), member.getRequestedRoomType());
  }

  private void pause() { MessageUI.pressEnterToContinue(); }
}