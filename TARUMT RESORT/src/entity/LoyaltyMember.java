package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoyaltyMember implements Comparable<LoyaltyMember> {

  /** The registered loyalty profile this priority-queue entry belongs to. */
  private final RewardsMember loyaltyMember;
  private final String bookingId;
  private final String confirmationNumber;
  private String requestedRoomType;
  private final int numberOfNights;
  private final int arrivalSequence;
  private final LocalDate requestedCheckInDate;
  private final LocalDate waitingSince;
  private final LocalDateTime waitingStartedAt;

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence,
      "VIP-" + String.format("%04d", arrivalSequence),
      String.format("%08d", arrivalSequence), LocalDate.now(), LocalDate.now(),
      java.time.LocalDateTime.now());
  }

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, LocalDate waitingSince) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence,
      "VIP-" + String.format("%04d", arrivalSequence),
      String.format("%08d", arrivalSequence), LocalDate.now(), waitingSince,
      waitingSince.atStartOfDay());
    }

    public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, LocalDate requestedCheckInDate,
      LocalDate waitingSince) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence,
      "VIP-" + String.format("%04d", arrivalSequence),
      String.format("%08d", arrivalSequence), requestedCheckInDate, waitingSince,
      waitingSince.atStartOfDay());
  }

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, String bookingId,
      LocalDate requestedCheckInDate, LocalDate waitingSince) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence, bookingId,
      String.format("%08d", arrivalSequence), requestedCheckInDate, waitingSince,
      waitingSince.atStartOfDay());
  }

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, String bookingId, String confirmationNumber,
      LocalDate requestedCheckInDate, LocalDate waitingSince) {
    this(loyaltyMember, requestedRoomType, numberOfNights, arrivalSequence, bookingId,
      confirmationNumber, requestedCheckInDate, waitingSince, waitingSince.atStartOfDay());
  }

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence, String bookingId, String confirmationNumber,
      LocalDate requestedCheckInDate, LocalDate waitingSince, LocalDateTime waitingStartedAt) {
    this.loyaltyMember = loyaltyMember;
    this.bookingId = bookingId;
    this.confirmationNumber = confirmationNumber;
    this.requestedRoomType = requestedRoomType;
    this.numberOfNights = numberOfNights;
    this.arrivalSequence = arrivalSequence;
    this.requestedCheckInDate = requestedCheckInDate;
    this.waitingSince = waitingSince;
    this.waitingStartedAt = waitingStartedAt == null ? waitingSince.atStartOfDay() : waitingStartedAt;
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

  public String getConfirmationNumber() {
    return confirmationNumber;
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

  public LocalDateTime getWaitingStartedAt() {
    return waitingStartedAt;
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