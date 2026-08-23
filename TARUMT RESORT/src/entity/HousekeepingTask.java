package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents ONE cleaning task in the housekeeping system.
 *
 * Think of it as a "to-do card" on the housekeeping board:
 *   - WHICH room needs cleaning   (roomNumber)
 *   - WHO is assigned to do it    (assignedStaff)
 *   - WHAT kind of cleaning       (taskType)
 *   - HOW FAR along it is         (currentStatus)
 *   - WHEN it was created         (loggedAt)
 *
 * The controller stores these tasks in a Linear List ADT (ArrayList),
 * so they stay in the order they were created - just like a queue of cards.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTask implements Serializable {

  /** Formats the task creation time nicely, e.g. "2026-08-18 01:08". */
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  // ---------- The data stored for each task ----------
  private String taskId;          // Unique ID, e.g. T1001 (auto-assigned)
  private String roomNumber;      // Which room, e.g. R101
  private String assignedStaff;   // Which housekeeper, e.g. HK001
  private String taskType;        // What to do, e.g. CHECKOUT_CLEAN
  private RoomStatus currentStatus; // What stage the room is at now
  private LocalDateTime loggedAt;   // When this task was created

  /** Empty constructor - needed so the class can be rebuilt from a saved file. */
  public HousekeepingTask() {
  }

  /**
   * Full constructor - creates a ready-to-use task.
   * The controller calls this whenever a new cleaning task is added.
   */
  public HousekeepingTask(String taskId, String roomNumber, String assignedStaff,
      String taskType, RoomStatus currentStatus, LocalDateTime loggedAt) {
    this.taskId = taskId;
    this.roomNumber = roomNumber;
    this.assignedStaff = assignedStaff;
    this.taskType = taskType;
    this.currentStatus = currentStatus;
    this.loggedAt = loggedAt;
  }

  // ---------- Getters & Setters (read / update each field) ----------

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public String getAssignedStaff() {
    return assignedStaff;
  }

  public void setAssignedStaff(String assignedStaff) {
    this.assignedStaff = assignedStaff;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public RoomStatus getCurrentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(RoomStatus currentStatus) {
    this.currentStatus = currentStatus;
  }

  public LocalDateTime getLoggedAt() {
    return loggedAt;
  }

  public void setLoggedAt(LocalDateTime loggedAt) {
    this.loggedAt = loggedAt;
  }

  /**
   * Turns this task into one neat line of text for the console report.
   * Each column is padded so the table lines up nicely, e.g.:
   *   T1001    R101      HK001      CHECKOUT_CLEAN  Dirty  2026-08-18 01:08
   */
  @Override
  public String toString() {
    return String.format("%-8s %-8s %-10s %-16s %-22s %s",
        taskId, roomNumber, assignedStaff, taskType,
        currentStatus.getLabel(), loggedAt.format(FORMATTER));
  }
}