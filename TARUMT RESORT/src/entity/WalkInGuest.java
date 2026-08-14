package entity;

/**
 * Represents a walk-in guest registration.
 *
 * @author Your Name
 */
public class WalkInGuest {

    private String walkInId;
    private String guestName;
    private String contactNumber;
    private String roomType;
    private int numberOfNights;

    public WalkInGuest(String walkInId,
            String guestName,
            String contactNumber,
            String roomType,
            int numberOfNights) {

        this.walkInId = walkInId;
        this.guestName = guestName;
        this.contactNumber = contactNumber;
        this.roomType = roomType;
        this.numberOfNights = numberOfNights;
    }

    public String getWalkInId() {
        return walkInId;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public int getNumberOfNights() {
        return numberOfNights;
    }

    public void setWalkInId(String walkInId) {
        this.walkInId = walkInId;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setNumberOfNights(int numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    @Override
    public String toString() {
        return String.format(
                "%-12s %-18s %-14s %-12s %-8d",
                walkInId,
                guestName,
                contactNumber,
                roomType,
                numberOfNights
        );
    }
}