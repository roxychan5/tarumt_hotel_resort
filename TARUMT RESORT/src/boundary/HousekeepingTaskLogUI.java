package boundary;

import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatus;
import utility.ConsoleUI;
import utility.MessageUI;

/**
 * Boundary class for the Housekeeping and Task Log module. It exposes the
 * Linear ADT features to supervisors: the sequential task list and the LIFO
 * status-change stack used for rollback and history preview.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTaskLogUI {

  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("HOUSEKEEPING & TASK LOG MODULE");
    ConsoleUI.displayMenuOption(1, "View Task Queue", "Sequential task log");
    ConsoleUI.displayMenuOption(2, "Add Cleaning Task", "Create and assign a task");
    ConsoleUI.displayMenuOption(3, "Advance Room Status", "Move to next cleaning stage");
    ConsoleUI.displayMenuOption(4, "Undo Last Status Change", "Undo stack (LIFO)");
    ConsoleUI.displayMenuOption(5, "Redo Last Status Change", "Redo stack (LIFO)");
    ConsoleUI.displayMenuOption(6, "Roll Back Multiple Changes", "Bulk LIFO rollback");
    ConsoleUI.displayMenuOption(7, "Roll Back Specific Room", "Latest change for one room");
    ConsoleUI.displayMenuOption(8, "View Status Change History", "Preview undo stack");
    ConsoleUI.displayMenuOption(9, "View Stack Statistics", "Undo/redo stack totals");
    ConsoleUI.displayMenuOption(10, "Handle Late Check-Out", "Reset a room to Dirty");
    ConsoleUI.displayMenuOption(11, "View Room Status Board");
    ConsoleUI.displayMenuOption(12, "Tasks by Status Report");
    ConsoleUI.displayMenuOption(13, "Staff Workload Summary");
    System.out.println("  " + "-".repeat(72));
    ConsoleUI.displayMenuOption(0, "Back to Main Menu");
    return ConsoleUI.readMenuChoice("\nSelect an option > ");
  }

  public String inputRoomNumber() {
    while (true) {
      System.out.print("Enter room number (e.g. R101): ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.matches("R[0-9]{3,4}")) {
        return value;
      }
      MessageUI.displayErrorMessage("Room number must be R followed by 3 or 4 digits (for example, R101).");
    }
  }

  public String inputAssignedStaff() {
    while (true) {
      System.out.print("Enter assigned staff ID (e.g. HK001): ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.matches("HK[0-9]{3,5}")) {
        return value;
      }
      MessageUI.displayErrorMessage("Staff ID must be HK followed by 3 to 5 digits (for example, HK001).");
    }
  }

  public String inputTaskType() {
    System.out.println("Task types: 1. CHECKOUT_CLEAN  2. DEEP_CLEAN  3. TURNDOWN  4. INSPECTION");
    while (true) {
      switch (ConsoleUI.readMenuChoice("Select task type: ")) {
        case 1: return "CHECKOUT_CLEAN";
        case 2: return "DEEP_CLEAN";
        case 3: return "TURNDOWN";
        case 4: return "INSPECTION";
        default: MessageUI.displayErrorMessage("Select a task type from 1 to 4.");
      }
    }
  }

  public String inputRollbackReason() {
    while (true) {
      System.out.print("Enter reason for rollback (at least 5 characters): ");
      String value = ConsoleUI.readLine().trim();
      if (value.length() >= 5) {
        return value;
      }
      MessageUI.displayErrorMessage("A rollback reason must contain at least 5 characters.");
    }
  }

  public int inputRollbackCount(int availableChanges) {
    while (true) {
      int count = ConsoleUI.readMenuChoice(
          "Number of latest changes to roll back (1-" + availableChanges + "): ");
      if (count >= 1 && count <= availableChanges) {
        return count;
      }
      MessageUI.displayErrorMessage(
          "Enter a rollback count from 1 to " + availableChanges + ".");
    }
  }

  public RoomStatus inputTargetStatus() {
    System.out.println("\nSelect target status:");
    System.out.println("  1. Dirty");
    System.out.println("  2. Cleaning In Progress");
    System.out.println("  3. Inspected");
    System.out.println("  4. Ready for Check-In");
    int choice = ConsoleUI.readMenuChoice("Enter choice: ");
    switch (choice) {
      case 1:
        return RoomStatus.DIRTY;
      case 2:
        return RoomStatus.CLEANING_IN_PROGRESS;
      case 3:
        return RoomStatus.INSPECTED;
      case 4:
        return RoomStatus.READY_FOR_CHECK_IN;
      default:
        return null;
    }
  }

  public void listTaskQueue(String output) {
    ConsoleUI.displaySubHeader("HOUSEKEEPING TASK QUEUE");
    ConsoleUI.displayTableHeader(
        String.format("%-8s %-8s %-16s %-14s %-22s %s\n",
            "Task ID", "Room", "Staff", "Task Type", "Status", "Logged At"));
    if (output.isEmpty()) {
      System.out.println("  (No tasks in queue)");
    } else {
      System.out.println(output);
    }
  }

  public void listRoomStatuses(String output) {
    ConsoleUI.displaySubHeader("ROOM STATUS BOARD");
    ConsoleUI.displayTableHeader(
        String.format("%-8s %-12s %-8s %s\n", "Room", "Type", "Floor", "Status"));
    if (output.isEmpty()) {
      System.out.println("  (No rooms registered)");
    } else {
      System.out.println(output);
    }
  }

  public void displayTaskDetails(HousekeepingTask task) {
    ConsoleUI.displayDetailPanel("TASK CREATED",
        "Task ID   : " + task.getTaskId(),
        "Room      : " + task.getRoomNumber(),
        "Staff     : " + task.getAssignedStaff(),
        "Task Type : " + task.getTaskType(),
        "Status    : " + task.getCurrentStatus().getLabel(),
        "Logged At : " + task.getLoggedAt());
  }

  public void displayRoomDetails(Room room) {
    ConsoleUI.displayDetailPanel("ROOM DETAILS",
        "Room No. : " + room.getRoomNumber(),
        "Type     : " + room.getRoomType(),
        "Floor    : " + room.getFloor(),
        "Status   : " + room.getStatus().getLabel());
  }

  public void displayReport(String title, String content) {
    ConsoleUI.displaySubHeader(title);
    System.out.println(content);
  }

  public void displayStatusFlowGuide() {
    ConsoleUI.displayDetailPanel("ROOM STATUS FLOW",
        "Dirty  ->  Cleaning In Progress  ->  Inspected  ->  Ready for Check-In");
  }
}
