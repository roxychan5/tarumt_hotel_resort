package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one entry in the Housekeeping sequential task log (Linear List
 * ADT). The controller appends tasks in logging order.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTask implements Serializable {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private String taskId;
  private String roomNumber;
  private String assignedStaff;
  private String taskType;
  private RoomStatus currentStatus;
  private LocalDateTime loggedAt;

  public HousekeepingTask() {
  }

  public HousekeepingTask(String taskId, String roomNumber, String assignedStaff,
      String taskType, RoomStatus currentStatus, LocalDateTime loggedAt) {
    this.taskId = taskId;
    this.roomNumber = roomNumber;
    this.assignedStaff = assignedStaff;
    this.taskType = taskType;
    this.currentStatus = currentStatus;
    this.loggedAt = loggedAt;
  }

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

  @Override
  public String toString() {
    return String.format("%-8s %-8s %-16s %-14s %-22s %s",
        taskId, roomNumber, assignedStaff, taskType,
        currentStatus.getLabel(), loggedAt.format(FORMATTER));
  }
}
