package entity;

/** Completed room allocation kept for end-of-cycle reporting. */
public class RoomAllocation {

  private final LoyaltyMember member;
  private final String roomNumber;
  private final int allocationSequence;

  public RoomAllocation(LoyaltyMember member, String roomNumber, int allocationSequence) {
    this.member = member;
    this.roomNumber = roomNumber;
    this.allocationSequence = allocationSequence;
  }

  public LoyaltyMember getMember() { return member; }
  public String getRoomNumber() { return roomNumber; }
  public int getAllocationSequence() { return allocationSequence; }
}

