package entity;

import java.time.LocalDate;

public class LoyaltyMember implements Comparable<LoyaltyMember> {

  /** The registered loyalty profile this priority-queue entry belongs to. */
  private final RewardsMember loyaltyMember;
  private final String bookingId;
  private String requestedRoomType;
  private final int numberOfNights;
  private final int arrivalSequence;
  private final LocalDate requestedCheckInDate;
  private final LocalDate waitingSince;

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence,
      "VIP-" + String.format("%04d", arrivalSequence), LocalDate.now(), LocalDate.now());
  }

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, LocalDate waitingSince) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence,
      "VIP-" + String.format("%04d", arrivalSequence), LocalDate.now(), waitingSince);
    }

    public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, LocalDate requestedCheckInDate,
      LocalDate waitingSince) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence,
      "VIP-" + String.format("%04d", arrivalSequence), requestedCheckInDate, waitingSince);
  }

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, String bookingId,
      LocalDate requestedCheckInDate, LocalDate waitingSince) {
    this.loyaltyMember = loyaltyMember;
    this.bookingId = bookingId;
    this.requestedRoomType = requestedRoomType;
    this.numberOfNights = numberOfNights;
    this.arrivalSequence = arrivalSequence;
    this.requestedCheckInDate = requestedCheckInDate;
    this.waitingSince = waitingSince;
  }

  public RewardsMember getLoyaltyMember() { 
    return loyaltyMember; 
  }

  public String getMemberId() { 
    return loyaltyMember.getMemberId(); 
  }

  public String getBookingId() {
    return bookingId;
  }

  public String getGuestName() { 
    return loyaltyMember.getName(); 
  }

  public LoyaltyTier getTier() { 
    return loyaltyMember.getTier(); 
  }

  public String getRequestedRoomType() { 
    return requestedRoomType; 
  }

  public void setRequestedRoomType(String requestedRoomType) {
    this.requestedRoomType = requestedRoomType;
  }

  public int getNumberOfNights() {
    return numberOfNights;
  }

  public int getArrivalSequence() { 
    return arrivalSequence; 
  }

  public LocalDate getWaitingSince() {
    return waitingSince;
  }

  public LocalDate getRequestedCheckInDate() {
    return requestedCheckInDate;
  }

  @Override
  public int compareTo(LoyaltyMember other) {
    int tierComparison = getTier().getPriority() - other.getTier().getPriority();
    // Earlier arrival wins only when membership tiers are equal.
    return tierComparison != 0 ? tierComparison
        : other.arrivalSequence - arrivalSequence;
  }
}