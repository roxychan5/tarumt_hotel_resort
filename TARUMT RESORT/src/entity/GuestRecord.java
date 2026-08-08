package entity;

import java.io.Serializable;

public class GuestRecord implements Serializable {

  private String confirmationNumber;
  private String guestName;
  private String identificationNumber;
  private String contactNumber;
  private String roomNumber;
  private String roomType;
  private String checkInDate;
  private String checkOutDate;
  private int numberOfNights;
  private double nightlyRate;
  private double paidAmount;

  public GuestRecord(String confirmationNumber, String guestName,
      String identificationNumber, String contactNumber, String roomNumber,
      String roomType, String checkInDate, String checkOutDate,
      int numberOfNights, double nightlyRate, double paidAmount) {
    this.confirmationNumber = confirmationNumber;
    this.guestName = guestName;
    this.identificationNumber = identificationNumber;
    this.contactNumber = contactNumber;
    this.roomNumber = roomNumber;
    this.roomType = roomType;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
    this.numberOfNights = numberOfNights;
    this.nightlyRate = nightlyRate;
    this.paidAmount = paidAmount;
  }

  public String getConfirmationNumber() {
    return confirmationNumber;
  }

  public String getGuestName() {
    return guestName;
  }

  public String getIdentificationNumber() {
    return identificationNumber;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public String getRoomType() {
    return roomType;
  }

  public String getCheckInDate() {
    return checkInDate;
  }

  public String getCheckOutDate() {
    return checkOutDate;
  }

  public int getNumberOfNights() {
    return numberOfNights;
  }

  public double getNightlyRate() {
    return nightlyRate;
  }

  public double getPaidAmount() {
    return paidAmount;
  }

  public double getTotalAmount() {
    return numberOfNights * nightlyRate;
  }

  public double getOutstandingAmount() {
    return getTotalAmount() - paidAmount;
  }

  public String toFullDetailsString() {
    return "  Confirmation No. : " + confirmationNumber
        + "\n  Guest Name       : " + guestName
        + "\n  Guest ID         : " + identificationNumber
        + "\n  Contact No.      : " + contactNumber
        + "\n  Room             : " + roomNumber + " (" + roomType + ")"
        + "\n  Check-In Date    : " + checkInDate
        + "\n  Check-Out Date   : " + checkOutDate
        + "\n  Nights           : " + numberOfNights
        + "\n  Total Amount     : RM " + String.format("%.2f", getTotalAmount())
        + "\n  Paid Amount      : RM " + String.format("%.2f", paidAmount)
        + "\n  Outstanding      : RM " + String.format("%.2f", getOutstandingAmount());
  }

  public String toBillingString() {
    return "  Confirmation No. : " + confirmationNumber
        + "\n  Guest Name       : " + guestName
        + "\n  Room             : " + roomNumber + " (" + roomType + ")"
        + "\n  Nights           : " + numberOfNights
        + "\n  Rate Per Night   : RM " + String.format("%.2f", nightlyRate)
        + "\n  Total Amount     : RM " + String.format("%.2f", getTotalAmount())
        + "\n  Paid Amount      : RM " + String.format("%.2f", paidAmount)
        + "\n  Outstanding      : RM " + String.format("%.2f", getOutstandingAmount());
  }

  @Override
  public String toString() {
    return String.format("%-12s %-20s %-10s %-10s RM %9.2f",
        confirmationNumber, guestName, roomNumber, roomType, getOutstandingAmount());
  }
}
