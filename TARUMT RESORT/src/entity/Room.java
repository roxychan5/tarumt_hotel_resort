package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a hotel room stored in Housekeeping's sequential Linear List
 * ADT. Its status is updated as the cleaning workflow advances or rolls back.
 *
 * @author Chan Rou Xuan
 */
public class Room implements Serializable {

  private String roomNumber;
  private String roomType;
  private int floor;
  private RoomStatus status;

  public Room() {
  }

  public Room(String roomNumber, String roomType, int floor, RoomStatus status) {
    this.roomNumber = roomNumber;
    this.roomType = roomType;
    this.floor = floor;
    this.status = status;
  }

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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Room other = (Room) obj;
    return Objects.equals(roomNumber, other.roomNumber);
  }

  @Override
  public String toString() {
    return String.format("%-8s %-12s %-8d %-22s",
        roomNumber, roomType, floor, status.getLabel());
  }
}
