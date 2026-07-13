package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Stores a status change for rollback using the stack ADT.
 *
 * @author Your Name
 */
public class StatusChangeRecord implements Serializable {

  private String roomNumber;
  private RoomStatus previousStatus;
  private RoomStatus newStatus;
  private String reason;
  private LocalDateTime changedAt;

  public StatusChangeRecord() {
  }

  public StatusChangeRecord(String roomNumber, RoomStatus previousStatus,
      RoomStatus newStatus, String reason, LocalDateTime changedAt) {
    this.roomNumber = roomNumber;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.reason = reason;
    this.changedAt = changedAt;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public RoomStatus getPreviousStatus() {
    return previousStatus;
  }

  public RoomStatus getNewStatus() {
    return newStatus;
  }

  public String getReason() {
    return reason;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }

  @Override
  public String toString() {
    return String.format("Room %s: %s -> %s (%s)",
        roomNumber, previousStatus.getLabel(), newStatus.getLabel(), reason);
  }
}
