package entity;

/**
 * Represents a standard booking made by a member.
 *
 * @author Your Name
 */
public class StandardBooking {

    // Attributes
    private String bookingId;
    private String memberName;
    private String contactNumber;
    private String roomType;
    private int numberOfNights;
    private String status;

    // Constructor
    public StandardBooking(String bookingId, String memberName,
            String contactNumber, String roomType,
            int numberOfNights, String status) {

        this.bookingId = bookingId;
        this.memberName = memberName;
        this.contactNumber = contactNumber;
        this.roomType = roomType;
        this.numberOfNights = numberOfNights;
        this.status = status;
    }

    // Getters and Setters

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(int numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // toString()

    @Override
    public String toString() {
        return "Booking ID: " + bookingId
                + " | Member: " + memberName
                + " | Contact: " + contactNumber
                + " | Room Type: " + roomType
                + " | Nights: " + numberOfNights
                + " | Status: " + status;
    }

    // equals()

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        StandardBooking other = (StandardBooking) obj;

        return bookingId != null && bookingId.equals(other.bookingId);
    }

    // hashCode()

    @Override
    public int hashCode() {
        return bookingId != null ? bookingId.hashCode() : 0;
    }
}
