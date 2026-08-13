package control;

import adt.ArrayList;
import adt.ArrayPriorityQueue;
import boundary.VipLoyaltyAllocationUI;
import entity.LoyaltyMember;
import entity.LoyaltyTier;
import entity.RoomAllocation;
import utility.MessageUI;

/**
 * Controls VIP room allocation using a self-ordering priority queue.
 * Reports combine sequential searching, multiple filters and insertion sort.
 *
 * @author Replace with your name
 */
public class VipLoyaltyAllocation {

  private final VipLoyaltyAllocationUI vipUI = new VipLoyaltyAllocationUI();
  private final ArrayPriorityQueue<LoyaltyMember> waitingGuests = new ArrayPriorityQueue<>();
  private final ArrayList<RoomAllocation> completedAllocations = new ArrayList<>();
  private int arrivalSequence;
  private int allocationSequence;

  public void runVipLoyaltyModule() {
    int choice;
    do {
      choice = vipUI.getMenuChoice();
      switch (choice) {
        case 0: MessageUI.displayInfoMessage("Returning to main menu..."); break;
        case 1: addPriorityGuest(); break;
        case 2: viewNextGuest(); break;
        case 3: allocateRoom(); break;
        case 4: vipUI.displayPriorityQueue(buildQueueDisplay()); pause(); break;
        case 5: generateWaitingListReport(); break;
        case 6: generateAllocationPerformanceReport(); break;
        default: MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void addPriorityGuest() {
    String memberId = vipUI.inputMemberId();
    String guestName = vipUI.inputGuestName();
    int tierNumber = vipUI.inputLoyaltyTier();
    String roomType = vipUI.inputRequestedRoomType();
    LoyaltyTier tier = LoyaltyTier.fromPriority(tierNumber);
    if (memberId.isEmpty() || guestName.isEmpty() || roomType.isEmpty() || tier == null) {
      MessageUI.displayErrorMessage("Member ID, name, room type and a tier from 1 to 5 are required.");
    } else if (isMemberWaiting(memberId)) {
      MessageUI.displayErrorMessage("This member is already in the priority queue.");
    } else {
      LoyaltyMember member = new LoyaltyMember(memberId, guestName, tier, roomType, ++arrivalSequence);
      waitingGuests.add(member);
      MessageUI.displaySuccessMessage(tier + " member added. Queue reordered automatically.");
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
    LoyaltyMember member = waitingGuests.getFront();
    if (member == null) {
      MessageUI.displayErrorMessage("No priority guest is waiting for allocation.");
    } else {
      String roomNumber = vipUI.inputRoomNumber();
      if (roomNumber.isEmpty()) {
        MessageUI.displayErrorMessage("Room number is required.");
      } else {
        waitingGuests.remove();
        completedAllocations.add(new RoomAllocation(member, roomNumber, ++allocationSequence));
        MessageUI.displaySuccessMessage("Room " + roomNumber + " allocated to "
            + member.getGuestName() + " (" + member.getTier() + ").");
      }
    }
    pause();
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
    String output = String.format("%-5s %-12s %-20s %-12s %-15s%n", "Rank", "Member ID", "Guest", "Tier", "Requested Room")
        + "----------------------------------------------------------------------------\n";
    for (int position = 1; position <= waitingGuests.getNumberOfEntries(); position++) {
      output += formatMember(waitingGuests.getEntry(position), position) + "\n";
    }
    return output;
  }

  private String formatMember(LoyaltyMember member, int rank) {
    return String.format("%-5d %-12s %-20s %-12s %-15s", rank, member.getMemberId(),
        member.getGuestName(), member.getTier(), member.getRequestedRoomType());
  }

  private void pause() { MessageUI.pressEnterToContinue(); }
}