package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Room allocation details captured when a VIP booking is checked in. */
public class VipCheckInRecord {

  private final String bookingId;
  private final String confirmationNumber;
  private final String memberId;
  private final String memberName;
  private final String roomType;
  private final int nights;
  private final int allocationSequence;
  private final LocalDate requestedCheckInDate;
  private final LocalDate waitingSince;
  private final String roomNumber;
  private final LocalDateTime checkInAt;
  private final LocalDateTime expectedCheckoutAt;

  public VipCheckInRecord(String bookingId, String confirmationNumber, String memberId,
      String memberName, String roomType, int nights, int allocationSequence,
      LocalDate requestedCheckInDate, LocalDate waitingSince, String roomNumber,
      LocalDateTime checkInAt, LocalDateTime expectedCheckoutAt) {
    this.bookingId = bookingId;
    this.confirmationNumber = confirmationNumber;
    this.memberId = memberId;
    this.memberName = memberName;
    this.roomType = roomType;
    this.nights = nights;
    this.allocationSequence = allocationSequence;
    this.requestedCheckInDate = requestedCheckInDate;
    this.waitingSince = waitingSince;
    this.roomNumber = roomNumber;
    this.checkInAt = checkInAt;
    this.expectedCheckoutAt = expectedCheckoutAt;
  }

  public String getBookingId() { return bookingId; }

  public String getConfirmationNumber() { return confirmationNumber; }

  public String getMemberId() { return memberId; }

  public String getMemberName() { return memberName; }

  public String getRoomType() { return roomType; }

  public int getNights() { return nights; }

  public int getAllocationSequence() { return allocationSequence; }

  public LocalDate getRequestedCheckInDate() { return requestedCheckInDate; }

  public LocalDate getWaitingSince() { return waitingSince; }

  public String getRoomNumber() { return roomNumber; }

  public LocalDateTime getCheckInAt() { return checkInAt; }

  public LocalDateTime getExpectedCheckoutAt() { return expectedCheckoutAt; }
}
