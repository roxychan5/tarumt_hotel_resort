package boundary;

import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatus;
import java.util.Scanner;
import utility.ConsoleUI;

/**
 * Boundary class for the Housekeeping and Task Log module.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTaskLogUI {

  private final Scanner scanner = new Scanner(System.in);

  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("HOUSEKEEPING & TASK LOG MODULE");
    System.out.println("  1. View Task Queue (Sequential Log)");
    System.out.println("  2. Add New Cleaning Task");
    System.out.println("  3. Advance Room Status");
    System.out.println("  4. Roll Back Last Status Change  [Stack ADT]");
    System.out.println("  5. Handle Late Check-Out");
    System.out.println("  6. View All Room Statuses");
    System.out.println("  7. Report: Tasks by Status");
    System.out.println("  8. Report: Staff Workload Summary");
    System.out.println("  0. Back to Main Menu");
    System.out.print("\nEnter choice: ");
    int choice = scanner.nextInt();
    scanner.nextLine();
    return choice;
  }

  public String inputRoomNumber() {
    System.out.print("Enter room number (e.g. R101): ");
    return scanner.nextLine().trim().toUpperCase();
  }

  public String inputAssignedStaff() {
    System.out.print("Enter assigned staff ID (e.g. HK001): ");
    return scanner.nextLine().trim().toUpperCase();
  }

  public String inputTaskType() {
    System.out.println("Task types: CHECKOUT_CLEAN, DEEP_CLEAN, TURNDOWN, INSPECTION");
    System.out.print("Enter task type: ");
    return scanner.nextLine().trim().toUpperCase();
  }

  public String inputRollbackReason() {
    System.out.print("Enter reason for rollback: ");
    return scanner.nextLine().trim();
  }

  public RoomStatus inputTargetStatus() {
    System.out.println("\nSelect target status:");
    System.out.println("  1. Dirty");
    System.out.println("  2. Cleaning In Progress");
    System.out.println("  3. Inspected");
    System.out.println("  4. Ready for Check-In");
    System.out.print("Enter choice: ");
    int choice = scanner.nextInt();
    scanner.nextLine();
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
    System.out.println("\nTask Details");
    System.out.println("  Task ID   : " + task.getTaskId());
    System.out.println("  Room      : " + task.getRoomNumber());
    System.out.println("  Staff     : " + task.getAssignedStaff());
    System.out.println("  Task Type : " + task.getTaskType());
    System.out.println("  Status    : " + task.getCurrentStatus().getLabel());
    System.out.println("  Logged At : " + task.getLoggedAt());
  }

  public void displayRoomDetails(Room room) {
    System.out.println("\nRoom Details");
    System.out.println("  Room No. : " + room.getRoomNumber());
    System.out.println("  Type     : " + room.getRoomType());
    System.out.println("  Floor    : " + room.getFloor());
    System.out.println("  Status   : " + room.getStatus().getLabel());
  }

  public void displayReport(String title, String content) {
    ConsoleUI.displaySubHeader(title);
    System.out.println(content);
  }

  public void displayStatusFlowGuide() {
    System.out.println("\nStatus Flow: Dirty -> Cleaning In Progress -> Inspected -> Ready for Check-In");
  }
}
