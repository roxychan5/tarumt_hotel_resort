package boundary;

import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatus;
import utility.ConsoleUI;
import utility.MessageUI;

/**
 * Boundary class for the Housekeeping and Task Log module.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTaskLogUI {

  private static final int  BOX_W  = 76;   // total visible chars inside | |
  private static final int  LABEL_W = 26;   // visible width of label column
  private static final char HL    = '-';
  private static final char VL    = '|';

  // Color shortcuts
  private static final String R  = ConsoleUI.RESET;
  private static final String B  = ConsoleUI.BOLD;
  private static final String C  = ConsoleUI.CYAN;
  private static final String IB = ConsoleUI.ICE_BLUE;
  private static final String SB = ConsoleUI.SKY_BLUE;
  private static final String DM = ConsoleUI.DIM;
  private static final String WH = ConsoleUI.WHITE;
  private static final String RD = ConsoleUI.RED;

  // ======================================================================
  // Main Menu
  // ======================================================================

  public int getMenuChoice() {
    ConsoleUI.clearScreen();
    printMenu();
    return ConsoleUI.readMenuChoice(
        "  " + SB + B + "  Select option (0-13) > " + R + " ");
  }

  private void printMenu() {
    System.out.println();
    printBorder();
    printTitle("HOUSEKEEPING  &  TASK LOG", "Module : Chan Rou Xuan");
    printBorder();

    printSectionLabel("TASK MANAGEMENT");
    printEntry(" 1", "Open Task Log",          "Review tasks in logging order");
    printEntry(" 2", "Create Cleaning Task",   "Assign a cleaning task to staff");
    printEntry(" 3", "Advance Room Workflow",  "Move room to its next valid stage");
    printBorder();

    printSectionLabel("STATUS CHANGE CONTROL");
    printEntry(" 4", "Undo Latest Change",     "Reverse the most recent update");
    printEntry(" 5", "Redo Latest Change",     "Reapply the most recently undone update");
    printEntry(" 6", "Undo Multiple Changes",  "Reverse several recent updates at once");
    printEntry(" 7", "Undo Change for Room",   "Reverse a specific room's latest update");
    printEntry(" 8", "View Change History",    "See undo stack from TOP to BOTTOM");
    printEntry(" 9", "View Undo/Redo Summary", "Count of available undo/redo changes");
    printBorder();

    printSectionLabel("OPERATIONS  &  REPORTS");
    printEntry("10", "Record Late Check-Out",  "Reset room back to Dirty status");
    printEntry("11", "Room Status Board",      "Monitor every room at a glance");
    printEntryHighlight("12", "Report 1: Operational Summary",
        "Binary search + bubble sort  | PDF");
    printEntryHighlight("13", "Report 2: Staff Workload",
        "Insertion sort ranking        | PDF");
    printBorder();

    printStatusLegend();
    printBorder();

    printBack(" 0", "Back to Main Menu");
    printBorder();
    System.out.println();
  }

  // ======================================================================
  // Box helpers  (pure ASCII)
  // ======================================================================

  private void printBorder() {
    System.out.println("  " + SB + B + "+" + rep(HL, BOX_W) + "+" + R);
  }

  private void printTitle(String title, String sub) {
    // visible length = title / sub plain lengths; centered
    rowV(centerPad(B + C + title + R, title.length()), BOX_W);
    rowV(centerPad(DM + sub + R,       sub.length()),   BOX_W);
  }

  private void printSectionLabel(String label) {
    // visible: "  " + label  =  2 + label.length()
    int vis = 2 + label.length();
    rowV("  " + IB + B + label + R, vis);
  }

  private void printEntry(String num, String label, String desc) {
    // visible: " [XX] " = 6, label padded to LABEL_W, "  " = 2, desc
    int vis = 6 + LABEL_W + 2 + desc.length();
    rowV(" " + SB + B + "[" + num + "]" + R + " "
        + WH + B + padR(label, LABEL_W) + R
        + "  " + DM + desc + R, vis);
  }

  private void printEntryHighlight(String num, String label, String desc) {
    // same visible layout as printEntry
    int vis = 6 + LABEL_W + 2 + desc.length();
    rowV(" " + C + B + "[" + num + "]" + R + " "
        + C + B + padR(label, LABEL_W) + R
        + "  " + IB + desc + R, vis);
  }

  private void printBack(String num, String label) {
    // visible: " [XX]  " = 7, label
    int vis = 7 + label.length();
    rowV(" " + RD + B + "[" + num + "]  " + label + R, vis);
  }

  private void printStatusLegend() {
    // Count exact visible chars for each badge: " TEXT "
    int vis = 2                  // "  "
            + 8                  // "Status: "
            + 7                  // " DIRTY "
            + 10                 // " CLEANING "
            + 11                 // " INSPECTED "
            + 7;                 // " READY "
    rowV("  Status: "
        + " \u001B[41m\u001B[97mDIRTY\u001B[0m "
        + " \u001B[43m\u001B[30mCLEANING\u001B[0m "
        + " \u001B[44m\u001B[97mINSPECTED\u001B[0m "
        + " \u001B[42m\u001B[97mREADY\u001B[0m ",
        vis);
  }

  /**
   * Print one full box row.  visibleLen = number of printable characters
   * in content (ANSI codes excluded).  Pads to BOX_W before closing |.
   */
  private void rowV(String content, int visibleLen) {
    int pad = BOX_W - visibleLen;
    System.out.println("  " + SB + B + VL + R
        + content + rep(' ', Math.max(0, pad))
        + SB + B + VL + R);
  }

  private String centerPad(String colored, int plainLen) {
    int left  = (BOX_W - plainLen) / 2;
    int right = BOX_W - plainLen - left;
    return rep(' ', left) + colored + rep(' ', right);
  }

  // ======================================================================
  // Input helpers
  // ======================================================================

  public String inputRoomNumber() {
    while (true) {
      System.out.print("  " + SB + "Room No. (e.g. R101)" + R + " > ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.matches("R[0-9]{3,4}")) return value;
      MessageUI.displayErrorMessage(
          "Room number must be R followed by 3-4 digits (e.g. R101).");
    }
  }

  public String inputAssignedStaff() {
    while (true) {
      System.out.print("  " + SB + "Staff ID (e.g. HK001)" + R + " > ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.matches("HK[0-9]{3,5}")) return value;
      MessageUI.displayErrorMessage(
          "Staff ID must be HK followed by 3-5 digits (e.g. HK001).");
    }
  }

  public String inputTaskType() {
    System.out.println();
    System.out.println("  " + SB + B + "TASK TYPE" + R);
    System.out.println("  " + rep('-', 40));
    System.out.println("  " + SB + "[1]" + R + " Checkout Clean    "
        + SB + "[2]" + R + " Deep Clean");
    System.out.println("  " + SB + "[3]" + R + " Turndown          "
        + SB + "[4]" + R + " Inspection");
    System.out.println();
    while (true) {
      switch (ConsoleUI.readMenuChoice("  Select task type (1-4) > ")) {
        case 1: return "CHECKOUT_CLEAN";
        case 2: return "DEEP_CLEAN";
        case 3: return "TURNDOWN";
        case 4: return "INSPECTION";
        default: MessageUI.displayErrorMessage("Enter a number from 1 to 4.");
      }
    }
  }

  public String inputRollbackReason() {
    while (true) {
      System.out.print("  " + SB + "Reason for undo" + R + " (min 5 chars) > ");
      String value = ConsoleUI.readLine().trim();
      if (value.length() >= 5) return value;
      MessageUI.displayErrorMessage("Reason must be at least 5 characters.");
    }
  }

  public int inputRollbackCount(int availableChanges) {
    while (true) {
      int count = ConsoleUI.readMenuChoice(
          "  How many changes to roll back (1-" + availableChanges + ") > ");
      if (count >= 1 && count <= availableChanges) return count;
      MessageUI.displayErrorMessage(
          "Enter a number from 1 to " + availableChanges + ".");
    }
  }

  public RoomStatus inputTargetStatus() {
    System.out.println();
    System.out.println("  " + SB + B + "TARGET STATUS" + R);
    System.out.println("  " + rep('-', 40));
    System.out.println("  " + SB + "[1]" + R + " Dirty              "
        + SB + "[2]" + R + " Cleaning In Progress");
    System.out.println("  " + SB + "[3]" + R + " Inspected          "
        + SB + "[4]" + R + " Ready for Check-In");
    System.out.println();
    switch (ConsoleUI.readMenuChoice("  Select status (1-4) > ")) {
      case 1: return RoomStatus.DIRTY;
      case 2: return RoomStatus.CLEANING_IN_PROGRESS;
      case 3: return RoomStatus.INSPECTED;
      case 4: return RoomStatus.READY_FOR_CHECK_IN;
      default: return null;
    }
  }

  // ======================================================================
  // Display helpers
  // ======================================================================

  public void listTaskQueue(String output) {
    sectionHeader("HOUSEKEEPING TASK QUEUE",
        "Tasks listed in creation order (Linear List ADT).");
    System.out.printf("  %-8s %-8s %-10s %-16s %-22s %s%n",
        "Task ID", "Room", "Staff", "Task Type", "Status", "Logged At");
    System.out.println("  " + rep('-', 84));
    System.out.println(output.isEmpty() ? "  (No tasks in queue)" : output);
  }

  public void listRoomStatuses(String output) {
    sectionHeader("ROOM STATUS BOARD",
        "Coordinate the next valid cleaning action for each room.");
    System.out.printf("  %-8s %-12s %-8s %s%n", "Room", "Type", "Floor", "Status");
    System.out.println("  " + rep('-', 55));
    System.out.println(output.isEmpty() ? "  (No rooms registered)" : output);
  }

  public void displayTaskDetails(HousekeepingTask task) {
    sectionHeader("TASK CREATED", "Added to sequential log.");
    System.out.println("  Task ID   : " + C + B + task.getTaskId() + R);
    System.out.println("  Room      : " + task.getRoomNumber());
    System.out.println("  Staff     : " + task.getAssignedStaff());
    System.out.println("  Task Type : " + task.getTaskType());
    System.out.println("  Status    : " + colorStatus(task.getCurrentStatus()));
    System.out.println("  Logged At : " + DM + task.getLoggedAt() + R);
    System.out.println();
  }

  public void displayRoomDetails(Room room) {
    sectionHeader("ROOM DETAILS", "Current state of the selected room.");
    System.out.println("  Room No.  : " + C + B + room.getRoomNumber() + R);
    System.out.println("  Type      : " + room.getRoomType());
    System.out.println("  Floor     : " + room.getFloor());
    System.out.println("  Status    : " + colorStatus(room.getStatus()));
    System.out.println();
  }

  public void displayReport(String title, String content) {
    sectionHeader(title, "Results sorted and filtered by selected criteria.");
    System.out.println(content);
  }

  public void displayReportIntro(String title, String description) {
    sectionHeader(title, "");
    System.out.println("  " + C + B + "ALGORITHM DETAILS" + R);
    System.out.println("  " + rep('-', 55));
    for (String line : description.split("\n")) {
      System.out.println("  " + IB + line.trim() + R);
    }
    System.out.println();
  }

  public String[] inputReport1Filters() {
    sectionHeader("REPORT 1 - SET FILTERS", "Leave date blank to include all dates.");
    System.out.println("  " + DM
        + "Date: yyyy-MM-dd  |  Status: 0=ALL 1=Dirty 2=Cleaning 3=Inspected 4=Ready"
        + R);
    System.out.println("  " + DM
        + "Room Type: 0=ALL  1=Standard  2=Deluxe  3=Suite" + R);
    System.out.println();

    System.out.print("  " + SB + "From date" + R + " (yyyy-MM-dd or ENTER) > ");
    String from = ConsoleUI.readLine().trim();
    if (!from.isEmpty() && !from.matches("\\d{4}-\\d{2}-\\d{2}")) {
      MessageUI.displayErrorMessage("Invalid date format - filter skipped.");
      from = "";
    }

    System.out.print("  " + SB + "To date  " + R + " (yyyy-MM-dd or ENTER) > ");
    String to = ConsoleUI.readLine().trim();
    if (!to.isEmpty() && !to.matches("\\d{4}-\\d{2}-\\d{2}")) {
      MessageUI.displayErrorMessage("Invalid date format - filter skipped.");
      to = "";
    }

    String statusFilter;
    switch (ConsoleUI.readMenuChoice("  Status filter     (0-4) > ")) {
      case 1:  statusFilter = "DIRTY"; break;
      case 2:  statusFilter = "CLEANING_IN_PROGRESS"; break;
      case 3:  statusFilter = "INSPECTED"; break;
      case 4:  statusFilter = "READY_FOR_CHECK_IN"; break;
      default: statusFilter = "ALL";
    }

    String roomTypeFilter;
    switch (ConsoleUI.readMenuChoice("  Room type filter  (0-3) > ")) {
      case 1:  roomTypeFilter = "Standard"; break;
      case 2:  roomTypeFilter = "Deluxe"; break;
      case 3:  roomTypeFilter = "Suite"; break;
      default: roomTypeFilter = "ALL";
    }

    return new String[]{from, to, statusFilter, roomTypeFilter};
  }

  public String[] inputReport2Filters() {
    sectionHeader("REPORT 2 - SET FILTERS",
        "Filter by staff prefix and minimum task count.");
    System.out.println("  " + DM
        + "Prefix e.g. HK matches HK001, HK002...  |  Min tasks: 0 = show all"
        + R);
    System.out.println();

    System.out.print("  " + SB + "Staff prefix" + R + " (or ENTER for all) > ");
    String prefix = ConsoleUI.readLine().trim().toUpperCase();

    int minTasks = 0;
    while (true) {
      System.out.print("  " + SB + "Min tasks threshold" + R + " (0 = none) > ");
      try {
        minTasks = Integer.parseInt(ConsoleUI.readLine().trim());
        if (minTasks >= 0) break;
      } catch (NumberFormatException ignored) {}
      MessageUI.displayErrorMessage("Enter a whole number >= 0.");
    }

    return new String[]{prefix, String.valueOf(minTasks)};
  }

  public boolean confirmPdfExport() {
    System.out.println();
    System.out.print("  " + C + B + "Export as professional PDF? (y/n) > " + R);
    String answer = ConsoleUI.readLine().trim().toLowerCase();
    return answer.equals("y") || answer.equals("yes");
  }

  public void displayPdfExportSuccess(String filePath) {
    System.out.println();
    System.out.println("  " + C + B + "+" + rep('-', 60) + "+" + R);
    System.out.println("  " + C + B + "|  [OK]  PDF report exported successfully!"
        + rep(' ', 20) + "|" + R);
    System.out.println("  " + C + B + "|  " + R + "Path: " + IB + filePath + R);
    System.out.println("  " + C + B + "+" + rep('-', 60) + "+" + R);
    System.out.println();
  }

  public void displayStatusFlowGuide() {
    System.out.println();
    System.out.println("  " + SB + B + "STATUS FLOW" + R);
    System.out.println("  " + rep('-', 65));
    System.out.println("  "
        + "\u001B[41m\u001B[97m DIRTY \u001B[0m"
        + "  -->  "
        + "\u001B[43m\u001B[30m CLEANING \u001B[0m"
        + "  -->  "
        + "\u001B[44m\u001B[97m INSPECTED \u001B[0m"
        + "  -->  "
        + "\u001B[42m\u001B[97m READY FOR CHECK-IN \u001B[0m");
    System.out.println();
  }

  // ======================================================================
  // Private utilities
  // ======================================================================

  private void sectionHeader(String title, String subtitle) {
    System.out.println();
    System.out.println("  " + SB + B + "+" + rep('-', BOX_W) + "+" + R);
    System.out.println("  " + SB + B + "|" + R + "  " + C + B + title + R
        + rep(' ', Math.max(0, BOX_W - 2 - title.length()))
        + SB + B + "|" + R);
    if (!subtitle.isEmpty()) {
      System.out.println("  " + SB + B + "|" + R + "  " + DM + subtitle + R
          + rep(' ', Math.max(0, BOX_W - 2 - subtitle.length()))
          + SB + B + "|" + R);
    }
    System.out.println("  " + SB + B + "+" + rep('-', BOX_W) + "+" + R);
    System.out.println();
  }

  private String colorStatus(RoomStatus status) {
    switch (status) {
      case DIRTY:                return "\u001B[41m\u001B[97m DIRTY \u001B[0m";
      case CLEANING_IN_PROGRESS: return "\u001B[43m\u001B[30m CLEANING IN PROGRESS \u001B[0m";
      case INSPECTED:            return "\u001B[44m\u001B[97m INSPECTED \u001B[0m";
      case READY_FOR_CHECK_IN:   return "\u001B[42m\u001B[97m READY FOR CHECK-IN \u001B[0m";
      default:                   return status.getLabel();
    }
  }

  private String padR(String s, int w) {
    return s.length() >= w ? s : s + rep(' ', w - s.length());
  }

  private String rep(char c, int n) {
    if (n <= 0) return "";
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) sb.append(c);
    return sb.toString();
  }
}
