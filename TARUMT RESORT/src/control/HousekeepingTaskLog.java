package control;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import boundary.HousekeepingTaskLogUI;
import dao.HousekeepingDAO;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatus;
import entity.StatusChangeRecord;
import java.time.LocalDateTime;
import utility.MessageUI;

/**
 * Control class for Housekeeping and Task Log module.
 * Uses List ADT for sequential task log and Stack ADT for status rollback.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTaskLog {

  private final ListInterface<Room> roomList = new ArrayList<>();
  private final ListInterface<HousekeepingTask> taskList = new ArrayList<>();
  private final StackInterface<StatusChangeRecord> statusHistory = new LinkedStack<>();
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  private final HousekeepingTaskLogUI housekeepingUI = new HousekeepingTaskLogUI();
  private int taskCounter = 1000;

  public HousekeepingTaskLog() {
    loadData();
    if (roomList.isEmpty()) {
      seedSampleRooms();
    }
  }

  public void runHousekeepingModule() {
    int choice;
    do {
      choice = housekeepingUI.getMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayInfoMessage("Returning to main menu...");
          break;
        case 1:
          housekeepingUI.listTaskQueue(getAllTasks());
          MessageUI.pressEnterToContinue();
          break;
        case 2:
          addCleaningTask();
          break;
        case 3:
          advanceRoomStatus();
          break;
        case 4:
          rollbackLastChange();
          break;
        case 5:
          handleLateCheckout();
          break;
        case 6:
          housekeepingUI.listRoomStatuses(getAllRooms());
          MessageUI.pressEnterToContinue();
          break;
        case 7:
          generateTasksByStatusReport();
          break;
        case 8:
          generateStaffWorkloadReport();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void addCleaningTask() {
    String roomNumber = housekeepingUI.inputRoomNumber();
    Room room = findRoom(roomNumber);
    if (room == null) {
      MessageUI.displayErrorMessage("Room not found.");
      return;
    }

    String staffId = housekeepingUI.inputAssignedStaff();
    String taskType = housekeepingUI.inputTaskType();
    taskCounter++;
    String taskId = "HK" + taskCounter;

    HousekeepingTask task = new HousekeepingTask(
        taskId, roomNumber, staffId, taskType, room.getStatus(), LocalDateTime.now());
    taskList.add(task);
    saveData();
    housekeepingUI.displayTaskDetails(task);
    MessageUI.displaySuccessMessage("Cleaning task added to sequential log.");
    MessageUI.pressEnterToContinue();
  }

  private void advanceRoomStatus() {
    String roomNumber = housekeepingUI.inputRoomNumber();
    Room room = findRoom(roomNumber);
    if (room == null) {
      MessageUI.displayErrorMessage("Room not found.");
      return;
    }

    if (!room.getStatus().canAdvance()) {
      MessageUI.displayErrorMessage("Room is already Ready for Check-In.");
      return;
    }

    housekeepingUI.displayStatusFlowGuide();
    RoomStatus previousStatus = room.getStatus();
    RoomStatus newStatus = previousStatus.nextStatus();
    recordStatusChange(roomNumber, previousStatus, newStatus, "Status advanced by supervisor");
    room.setStatus(newStatus);
    syncTaskStatus(roomNumber, newStatus);
    saveData();

    housekeepingUI.displayRoomDetails(room);
    MessageUI.displaySuccessMessage("Room status updated successfully.");
    MessageUI.pressEnterToContinue();
  }

  private void rollbackLastChange() {
    if (statusHistory.isEmpty()) {
      MessageUI.displayErrorMessage("No status changes to roll back.");
      return;
    }

    String reason = housekeepingUI.inputRollbackReason();
    StatusChangeRecord record = statusHistory.pop();
    Room room = findRoom(record.getRoomNumber());
    if (room == null) {
      MessageUI.displayErrorMessage("Room no longer exists. Rollback cancelled.");
      statusHistory.push(record);
      return;
    }

    room.setStatus(record.getPreviousStatus());
    syncTaskStatus(record.getRoomNumber(), record.getPreviousStatus());
    saveData();

    System.out.println("\nRolled back: " + record);
    MessageUI.displaySuccessMessage("Schedule rolled back. Reason: " + reason);
    housekeepingUI.displayRoomDetails(room);
    MessageUI.pressEnterToContinue();
  }

  private void handleLateCheckout() {
    String roomNumber = housekeepingUI.inputRoomNumber();
    Room room = findRoom(roomNumber);
    if (room == null) {
      MessageUI.displayErrorMessage("Room not found.");
      return;
    }

    RoomStatus previousStatus = room.getStatus();
    if (previousStatus == RoomStatus.DIRTY) {
      MessageUI.displayInfoMessage("Room is already marked Dirty.");
      return;
    }

    recordStatusChange(roomNumber, previousStatus, RoomStatus.DIRTY, "Late check-out requested");
    room.setStatus(RoomStatus.DIRTY);
    syncTaskStatus(roomNumber, RoomStatus.DIRTY);
    saveData();

    housekeepingUI.displayRoomDetails(room);
    MessageUI.displaySuccessMessage("Late check-out handled. Room reset to Dirty.");
    MessageUI.pressEnterToContinue();
  }

  private void generateTasksByStatusReport() {
    RoomStatus[] statuses = RoomStatus.values();
    StringBuilder report = new StringBuilder();
    report.append(String.format("%-24s %8s\n", "Status", "Count"));
    report.append("----------------------------------------\n");

    for (RoomStatus status : statuses) {
      int count = countTasksByStatus(status);
      report.append(String.format("%-24s %8d\n", status.getLabel(), count));
    }

    housekeepingUI.displayReport("REPORT 1: TASKS BY STATUS", report.toString());
    MessageUI.pressEnterToContinue();
  }

  private void generateStaffWorkloadReport() {
    ListInterface<String> staffIds = new ArrayList<>();
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      String staffId = taskList.getEntry(i).getAssignedStaff();
      if (!staffIds.contains(staffId)) {
        staffIds.add(staffId);
      }
    }

    sortStaffByWorkload(staffIds);

    StringBuilder report = new StringBuilder();
    report.append(String.format("%-12s %8s %8s\n", "Staff ID", "Tasks", "Pending"));
    report.append("----------------------------------------\n");

    for (int i = 1; i <= staffIds.getNumberOfEntries(); i++) {
      String staffId = staffIds.getEntry(i);
      int totalTasks = countTasksForStaff(staffId);
      int pendingTasks = countPendingTasksForStaff(staffId);
      report.append(String.format("%-12s %8d %8d\n", staffId, totalTasks, pendingTasks));
    }

    housekeepingUI.displayReport("REPORT 2: STAFF WORKLOAD SUMMARY", report.toString());
    MessageUI.pressEnterToContinue();
  }

  private void sortStaffByWorkload(ListInterface<String> staffIds) {
    int n = staffIds.getNumberOfEntries();
    for (int i = 1; i < n; i++) {
      for (int j = 1; j <= n - i; j++) {
        String current = staffIds.getEntry(j);
        String next = staffIds.getEntry(j + 1);
        if (countTasksForStaff(current) < countTasksForStaff(next)) {
          staffIds.replace(j, next);
          staffIds.replace(j + 1, current);
        }
      }
    }
  }

  private int countTasksByStatus(RoomStatus status) {
    int count = 0;
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      if (taskList.getEntry(i).getCurrentStatus() == status) {
        count++;
      }
    }
    return count;
  }

  private int countTasksForStaff(String staffId) {
    int count = 0;
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      if (taskList.getEntry(i).getAssignedStaff().equals(staffId)) {
        count++;
      }
    }
    return count;
  }

  private int countPendingTasksForStaff(String staffId) {
    int count = 0;
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      HousekeepingTask task = taskList.getEntry(i);
      if (task.getAssignedStaff().equals(staffId)
          && task.getCurrentStatus() != RoomStatus.READY_FOR_CHECK_IN) {
        count++;
      }
    }
    return count;
  }

  private void recordStatusChange(String roomNumber, RoomStatus previous,
      RoomStatus current, String reason) {
    StatusChangeRecord record = new StatusChangeRecord(
        roomNumber, previous, current, reason, LocalDateTime.now());
    statusHistory.push(record);
  }

  private void syncTaskStatus(String roomNumber, RoomStatus status) {
    for (int i = taskList.getNumberOfEntries(); i >= 1; i--) {
      HousekeepingTask task = taskList.getEntry(i);
      if (task.getRoomNumber().equals(roomNumber)) {
        task.setCurrentStatus(status);
        break;
      }
    }
  }

  private Room findRoom(String roomNumber) {
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room room = roomList.getEntry(i);
      if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        return room;
      }
    }
    return null;
  }

  public String getAllTasks() {
    StringBuilder output = new StringBuilder();
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      output.append(taskList.getEntry(i)).append("\n");
    }
    return output.toString();
  }

  public String getAllRooms() {
    StringBuilder output = new StringBuilder();
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      output.append(roomList.getEntry(i)).append("\n");
    }
    return output.toString();
  }

  private void seedSampleRooms() {
    roomList.add(new Room("R101", "Standard", 1, RoomStatus.DIRTY));
    roomList.add(new Room("R102", "Standard", 1, RoomStatus.CLEANING_IN_PROGRESS));
    roomList.add(new Room("R201", "Deluxe", 2, RoomStatus.INSPECTED));
    roomList.add(new Room("R301", "Suite", 3, RoomStatus.READY_FOR_CHECK_IN));
    roomList.add(new Room("R302", "Suite", 3, RoomStatus.DIRTY));
    saveData();
  }

  private void loadData() {
    ListInterface<Room> loadedRooms = housekeepingDAO.retrieveRooms();
    ListInterface<HousekeepingTask> loadedTasks = housekeepingDAO.retrieveTasks();
    StackInterface<StatusChangeRecord> loadedHistory = housekeepingDAO.retrieveHistory();

    roomList.clear();
    for (int i = 1; i <= loadedRooms.getNumberOfEntries(); i++) {
      roomList.add(loadedRooms.getEntry(i));
    }

    taskList.clear();
    for (int i = 1; i <= loadedTasks.getNumberOfEntries(); i++) {
      taskList.add(loadedTasks.getEntry(i));
      String taskId = loadedTasks.getEntry(i).getTaskId();
      if (taskId.startsWith("HK")) {
        try {
          int id = Integer.parseInt(taskId.substring(2));
          if (id >= taskCounter) {
            taskCounter = id;
          }
        } catch (NumberFormatException ex) {
          MessageUI.displayErrorMessage("Ignoring invalid task ID in saved data: " + taskId);
        }
      }
    }

    statusHistory.clear();
    StackInterface<StatusChangeRecord> tempStack = new LinkedStack<>();
    while (!loadedHistory.isEmpty()) {
      tempStack.push(loadedHistory.pop());
    }
    while (!tempStack.isEmpty()) {
      statusHistory.push(tempStack.pop());
    }
  }

  private void saveData() {
    housekeepingDAO.saveRooms(roomList);
    housekeepingDAO.saveTasks(taskList);
    housekeepingDAO.saveHistory(statusHistory);
  }
}
