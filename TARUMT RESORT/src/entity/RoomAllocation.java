package entity;

import java.time.LocalDate;

// Completed room allocation kept for end-of-cycle reporting. //
public class RoomAllocation {

  private final LoyaltyMember member;
  private final String roomNumber;
  private final int allocationSequence;
  private final LocalDate checkInDate;
  private final LocalDate checkOutDate;

  public RoomAllocation(LoyaltyMember member, String roomNumber, int allocationSequence) {
    this(member, roomNumber, allocationSequence, LocalDate.now(),
        LocalDate.now().plusDays(member.getNumberOfNights()));
  }

  public RoomAllocation(LoyaltyMember member, String roomNumber, int allocationSequence,
      LocalDate checkInDate, LocalDate checkOutDate) {
    this.member = member;
    this.roomNumber = roomNumber;
    this.allocationSequence = allocationSequence;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
  }

  public LoyaltyMember getMember() { return member; }
  public String getRoomNumber() { return roomNumber; }
  public int getAllocationSequence() { return allocationSequence; }
  public LocalDate getCheckInDate() { return checkInDate; }
  public LocalDate getCheckOutDate() { return checkOutDate; }
}

