package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Payment and billing details captured when a VIP booking is created. */
public class VipPaymentRecord {

  private final String paymentId;
  private final String bookingId;
  private final String confirmationNumber;
  private final String memberId;
  private final String roomType;
  private final LocalDate checkInDate;
  private final LocalDate checkOutDate;
  private final int nights;
  private final double pricePerNight;
  private final double totalAmount;
  private final String paymentMethod;
  private final LocalDateTime paidAt;

  public VipPaymentRecord(String paymentId, String bookingId, String confirmationNumber,
      String memberId, String roomType, LocalDate checkInDate, LocalDate checkOutDate,
      int nights, double pricePerNight, double totalAmount, String paymentMethod,
      LocalDateTime paidAt) {
    this.paymentId = paymentId;
    this.bookingId = bookingId;
    this.confirmationNumber = confirmationNumber;
    this.memberId = memberId;
    this.roomType = roomType;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
    this.nights = nights;
    this.pricePerNight = pricePerNight;
    this.totalAmount = totalAmount;
    this.paymentMethod = paymentMethod;
    this.paidAt = paidAt;
  }

  public String getPaymentId() { return paymentId; }

  public String getBookingId() { return bookingId; }

  public String getConfirmationNumber() { return confirmationNumber; }

  public String getMemberId() { return memberId; }

  public String getRoomType() { return roomType; }

  public LocalDate getCheckInDate() { return checkInDate; }

  public LocalDate getCheckOutDate() { return checkOutDate; }

  public int getNights() { return nights; }

  public double getPricePerNight() { return pricePerNight; }

  public double getTotalAmount() { return totalAmount; }

  public String getPaymentMethod() { return paymentMethod; }

  public LocalDateTime getPaidAt() { return paidAt; }
}
