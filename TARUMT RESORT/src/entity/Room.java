package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents one hotel room in the housekeeping module.
 *
 * A Room is like a row in the hotel's "room board":
 *   - roomNumber : which room (e.g. R101)
 *   - roomType   : Standard / Deluxe / Suite
 *   - floor      : which floor it is on
 *   - status     : how clean it is right now (Dirty / Cleaning / etc.)
 *
 * The controller stores rooms in a Linear List ADT (ArrayList),
 * keeping them in the original registration order.
 *
 * @author Chan Rou Xuan
 */
public class Room implements Serializable {

  private String roomNumber;   // e.g. R101
  private String roomType;     // e.g. Standard, Deluxe, Suite
  private int floor;           // e.g. 1, 2, 3
  private RoomStatus status;   // current cleaning stage (see RoomStatus)
  private LocalDateTime checkInAt;
  private LocalDateTime expectedCheckoutAt;
  private String occupantMemberId;

  /** Empty constructor - needed so the class can be rebuilt from a saved file. */
  public Room() {
  }

  /** Full constructor - creates a room with all its details filled in. */
  public Room(String roomNumber, String roomType, int floor, RoomStatus status) {
    this(roomNumber, roomType, floor, status, null, null, null);
  }

  public Room(String roomNumber, String roomType, int floor, RoomStatus status,
      LocalDateTime checkInAt, LocalDateTime expectedCheckoutAt) {
    this(roomNumber, roomType, floor, status, checkInAt, expectedCheckoutAt, null);
  }

  public Room(String roomNumber, String roomType, int floor, RoomStatus status,
      LocalDateTime checkInAt, LocalDateTime expectedCheckoutAt, String occupantMemberId) {
    this.roomNumber = roomNumber;
    this.roomType = roomType;
    this.floor = floor;
    this.status = status;
    this.checkInAt = checkInAt;
    this.expectedCheckoutAt = expectedCheckoutAt;
    this.occupantMemberId = occupantMemberId;
  }

  // ---------- Getters & Setters (read / update each field) ----------

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public String getRoomType() {
    return roomType;
  }

  public void setRoomType(String roomType) {
    this.roomType = roomType;
  }

  public int getFloor() {
    return floor;
  }

  public void setFloor(int floor) {
    this.floor = floor;
  }

  public RoomStatus getStatus() {
    return status;
  }

  public void setStatus(RoomStatus status) {
    this.status = status;
  }

  public LocalDateTime getCheckInAt() {
    return checkInAt;
  }

  public void setCheckInAt(LocalDateTime checkInAt) {
    this.checkInAt = checkInAt;
  }

  public LocalDateTime getExpectedCheckoutAt() {
    return expectedCheckoutAt;
  }

  public void setExpectedCheckoutAt(LocalDateTime expectedCheckoutAt) {
    this.expectedCheckoutAt = expectedCheckoutAt;
  }

  public String getOccupantMemberId() {
    return occupantMemberId;
  }

  public void setOccupantMemberId(String occupantMemberId) {
    this.occupantMemberId = occupantMemberId;
  }

  /**
   * Two rooms are treated as the SAME room if they have the same room number.
   * This is used to search for a room in the list without comparing every field.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true; // same object, definitely equal
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false; // not a Room at all, so not equal
    }
    Room other = (Room) obj;
    return Objects.equals(roomNumber, other.roomNumber); // compare by room no.
  }

  /**
   * Turns this room into one neat line of text for the console report, e.g.:
   *   R101      Standard    1        Dirty
   */
  @Override
  public String toString() {
    return String.format("%-8s %-12s %-8d %-22s",
        roomNumber, roomType, floor, status.getLabel());
  }
}
