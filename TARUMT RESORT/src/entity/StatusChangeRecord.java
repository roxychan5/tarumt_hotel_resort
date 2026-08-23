package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A little "receipt" that remembers ONE room-status change.
 *
 * Every time a room's status changes (e.g. Dirty -> Cleaning), the system
 * saves a StatusChangeRecord showing:
 *   - which room changed        (roomNumber)
 *   - what it was before        (previousStatus)
 *   - what it became            (newStatus)
 *   - why it changed            (reason)
 *   - when it happened          (changedAt)
 *
 * These records are pushed onto a Stack ADT (LinkedStack) in the controller.
 * Stacks work like a pile: the MOST RECENT change sits on TOP, so undoing
 * means "pop the top" - reversing the latest change first (LIFO).
 *
 * @author Chan Rou Xuan
 */
public class StatusChangeRecord implements Serializable {

  private String roomNumber;            // which room was changed, e.g. R101
  private RoomStatus previousStatus;    // status BEFORE the change
  private RoomStatus newStatus;         // status AFTER the change
  private String reason;                // why it happened, e.g. "Status advanced by supervisor"
  private LocalDateTime changedAt;      // when it happened

  /** Empty constructor - needed so the class can be rebuilt from a saved file. */
  public StatusChangeRecord() {
  }

  /** Full constructor - creates a record of one room-status change. */
  public StatusChangeRecord(String roomNumber, RoomStatus previousStatus,
      RoomStatus newStatus, String reason, LocalDateTime changedAt) {
    this.roomNumber = roomNumber;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.reason = reason;
    this.changedAt = changedAt;
  }

  // ---------- Getters (read each field) ----------

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

  /**
   * Turns this change into a short readable sentence, e.g.:
   *   Room R101: Dirty -> Cleaning In Progress (Cleaning task T1001 assigned)
   */
  @Override
  public String toString() {
    return String.format("Room %s: %s -> %s (%s)",
        roomNumber, previousStatus.getLabel(), newStatus.getLabel(), reason);
  }
}