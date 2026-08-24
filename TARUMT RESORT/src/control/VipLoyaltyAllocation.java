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
import utility.MessageUI;

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
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      LoyaltyMember member = waitingGuests.getEntry(position);
      if (member.getTier().getPriority() >= tier.getPriority()
          && (roomTypeFilter.isEmpty() || member.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter))) {
        report += String.format("%-5d %-12s %-20s %-12s %-15s%n", position,
            member.getMemberId(), member.getGuestName(), member.getTier(), member.getRequestedRoomType());
        matches++;
      }
    }
    report += matches == 0 ? "No waiting guests meet both filters.\n" : "\nMatching guests: " + matches + "\n";
    vipUI.displayReport("REPORT 1: VIP PRIORITY WAITING LIST", report);
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
    pause();
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