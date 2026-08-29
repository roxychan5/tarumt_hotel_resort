package boundary;

import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatus;
import entity.StatusChangeRecord;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import utility.ConsoleUI;
import utility.MalaysiaTime;
import utility.MessageUI;

/**
 * The "screen & keyboard" part of the Housekeeping module.
 *
 * This class ONLY handles what the user sees and types:
 *   - prints the menu and pretty boxes
 *   - asks questions (room number, task type, etc.)
 *   - shows task / room / report tables
 *
 * It does NOT make any decisions. For example, when the user asks to
 * create a task, this class just gets the room number and task type,
 * then passes them to the controller (HousekeepingTaskLog) which does
 * the real work. This separation keeps the code clean and easy to change.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTaskLogUI {

  private static final int  BOX_W  = 86;   // total visible chars inside | |
  private static final int  ACTIVE_ROOMS_BOX_W = 100;
  private static final int  LABEL_W = 30;   // visible width of label column
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
        "  " + SB + B + "  Select option (0-14) > " + R + " ");
  }

  private void printMenu() {
    System.out.println();
    printBorder();
    printTitle("HOUSEKEEPING  &  TASK LOG", "Module : Task Log & Room Workflow Management");
    printBorder();

    printSectionLabel("TASK MANAGEMENT");
    printEntry(" 1", "Open Task Log",            "Review tasks in logging order");
    printEntry(" 2", "Create Cleaning Task",     "Assign a cleaning task to staff");
    printEntry(" 3", "Advance Room Workflow",    "Move room to its next valid stage");
    printEntry(" 4", "Search Task by ID",        "Linear search -> lookup any task");
    printEntry(" 5", "Delete Task by ID",        "Remove a task with confirmation");
    printBorder();

    printSectionLabel("STATUS CHANGE CONTROL");
    printEntry(" 6", "Undo Latest Change",       "Reverse the most recent update");
    printEntry(" 7", "Undo Change for Room",     "Reverse a specific room's latest update");
    printEntry(" 8", "View Change History",      "See undo stack from TOP to BOTTOM");
    printEntry(" 9", "View Undo Summary",        "Count of available rollback changes");
    printBorder();

    printSectionLabel("OPERATIONS  &  REPORTS");
    printEntry("10", "Record Late Check-Out",    "Ready -> Occupied; Front Desk sets new date");
    printEntry("11", "Room Status Board",        "Monitor every room at a glance");
    printEntryHighlight("12", "Report 1: Operational Summary",
        "Binary search + bubble sort | PDF");
    printEntryHighlight("13", "Report 2: Staff Workload",
        "Insertion sort ranking      | PDF");
    printEntryHighlight("14", "Report 3: Room Readiness",
        "Room readiness | status by room type | PDF");
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

  private void printBorder(int width) {
    System.out.println("  " + SB + B + "+" + rep(HL, width) + "+" + R);
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
    // vis uses max so overlong labels don't break padding
    int labelVis = Math.max(label.length(), LABEL_W);
    int vis = 6 + labelVis + 2 + desc.length();
    rowV(" " + SB + B + "[" + num + "]" + R + " "
        + WH + B + padR(label, LABEL_W) + R
        + "  " + DM + desc + R, vis);
  }

  private void printEntryHighlight(String num, String label, String desc) {
    int labelVis = Math.max(label.length(), LABEL_W);
    int vis = 6 + labelVis + 2 + desc.length();
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
    // Exact visible char count for each badge: " TEXT "
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
    rowV(content, visibleLen, BOX_W);
  }

  private void rowV(String content, int visibleLen, int width) {
    int pad = width - visibleLen;
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

  /**
   * Shows which rooms can receive a cleaning task.
   * Green = available (Dirty and no active task), Red = unavailable.
   * Uses the text summary built by the controller.
   */
  public void displayActiveRooms(String summary) {
    System.out.println();
    printBorder(ACTIVE_ROOMS_BOX_W);
    rowV("  " + C + B + "ROOM AVAILABILITY FOR NEW TASK ASSIGNMENT" + R,
        2 + "ROOM AVAILABILITY FOR NEW TASK ASSIGNMENT".length(), ACTIVE_ROOMS_BOX_W);
    printBorder(ACTIVE_ROOMS_BOX_W);

    if (summary == null || summary.isEmpty()) {
      String msg = "  No rooms registered.";
      rowV(DM + msg + R, msg.length(), ACTIVE_ROOMS_BOX_W);
    } else {
      // Legend — explains the Availability column
      String legend = "  " + "\033[92m" + "AVAILABLE" + R + " = can assign a task   "
          + RD + "UNAVAILABLE" + R + " = cannot assign a task";
      rowV(legend, visLen(legend), ACTIVE_ROOMS_BOX_W);
      rowV("", 0, ACTIVE_ROOMS_BOX_W);

      // Header — must match control format: Task(8) Room(8) Staff(8) Status(21) Reason
      String hdr = String.format("  %-8s %-8s %-8s %-21s %s",
          "Task ID", "Room", "Staff", "Status", "Availability");
      rowV(IB + B + hdr + R, hdr.length(), ACTIVE_ROOMS_BOX_W);
      rowV("  " + rep('-', 96), 2 + 96, ACTIVE_ROOMS_BOX_W);

      // Data rows — color-coded: green = available, red = unavailable
      for (String line : summary.split("\r?\n")) {
        if (line.trim().isEmpty()) continue;
        line = line.replace("\r", "");
        if (line.contains("AVAILABLE")) {
          rowV("  " + "\033[92m" + line + R, 2 + line.length(), ACTIVE_ROOMS_BOX_W);
        } else {
          rowV("  " + RD + line + R, 2 + line.length(), ACTIVE_ROOMS_BOX_W);
        }
      }
    }
    printBorder(ACTIVE_ROOMS_BOX_W);
    System.out.println();
  }

  /** Displays which staff member was auto-assigned and their current workload. */
  public void displayAutoAssign(String staffId) {
    System.out.println();
    System.out.println("  " + C + B + "+" + rep('-', BOX_W - 2) + "+" + R);
    int vis = 2 + "Auto-Assigned Staff: ".length() + staffId.length();
    rowV("  " + IB + "Auto-Assigned Staff: " + C + B + staffId + R, vis);
    rowV("  " + DM
        + "(Selected based on lowest active task count)"
        + R, 2 + "(Selected based on lowest active task count)".length());
    System.out.println("  " + C + B + "+" + rep('-', BOX_W - 2) + "+" + R);
    System.out.println();
  }

  /** Shows rooms eligible for workflow advancement before asking for room number. */
  public void displayAdvanceableRooms(String summary) {
    System.out.println();
    printBorder();
    rowV("  " + C + B + "ROOMS ELIGIBLE TO ADVANCE" + R,
        2 + "ROOMS ELIGIBLE TO ADVANCE".length());
    printBorder();

    if (summary == null || summary.isEmpty()) {
      String msg = "  No rooms are currently eligible to advance.";
      rowV(DM + msg + R, msg.length());
    } else {
      String hdr = String.format("  %-8s %-21s %s", "Room", "Current Status", "Will Advance To");
      rowV(IB + B + hdr + R, hdr.length());
      rowV("  " + rep('-', 50), 2 + 50);
      for (String line : summary.split("\r?\n")) {
        if (line.trim().isEmpty()) continue;
        line = line.replace("\r", "");
        rowV("  " + WH + line + R, 2 + line.length());
      }
    }
    printBorder();
    System.out.println();
  }


  /**
   * Shows EVERY room with its current status before recording a late
   * check-out. Green = Ready for Check-In and can roll back to Occupied.
   * Uses the text summary built by the controller.
   */
  public void displayLateCheckoutRooms(String summary) {
    System.out.println();
    printBorder();
    rowV("  " + C + B + "ALL ROOMS - RECORD LATE CHECK-OUT" + R,
        2 + "ALL ROOMS - RECORD LATE CHECK-OUT".length());
    printBorder();

    if (summary == null || summary.isEmpty()) {
      String msg = "  No rooms registered.";
      rowV(DM + msg + R, msg.length());
    } else {
      // Legend - explains the Action column
      String legend = "  " + "\033[92m" + "Ready -> Occupied" + R
          + " = roll back schedule   " + DM + "Not eligible" + R;
      rowV(legend, visLen(legend));
      row();

      // Header - must match control format: Room(8) Type(12) Floor(6) Status(22) Action
      String hdr = String.format("  %-8s %-12s %-6s %-22s %s",
          "Room", "Type", "Floor", "Current Status", "Late Check-Out");
      rowV(IB + B + hdr + R, hdr.length());
      rowV("  " + rep('-', 66), 2 + 66);

      // Data rows - green = eligible, dim = already unavailable
      for (String line : summary.split("\r?\n")) {
        if (line.trim().isEmpty()) continue;
        line = line.replace("\r", "");
        if (line.contains("Not eligible")) {
          rowV("  " + DM + line + R, 2 + line.length());
        } else {
          rowV("  " + "\033[92m" + line + R, 2 + line.length());
        }
      }
    }
    printBorder();
    System.out.println();
  }

  /** Asks for a Task ID (e.g. T1001), or returns null when 0 cancels. */
  public String inputTaskId(String action) {
    System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
    while (true) {
      System.out.print("  " + SB + action + " Task ID" + R + " (e.g. T1001, 0 to cancel) > ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.equals("0")) return null;
      // Accept both old HK-prefix and new T-prefix task IDs
      if (value.matches("(T|HK)[0-9]+")) return value;
      MessageUI.displayErrorMessage(
          "Task ID must start with T (or HK) followed by digits. Enter 0 to cancel.");
    }
  }

  /** Asks "are you sure?" before deleting something (only 'y' or 'yes' confirms). */
  public boolean confirmDelete(String target) {
    System.out.println();
    printBorder();
    rowV("  " + RD + B + "DELETE TASK CONFIRMATION" + R,
        2 + "DELETE TASK CONFIRMATION".length());
    printBorder();
    String warning = "  Delete " + target + "? This cannot be undone.";
    rowV(RD + warning + R, warning.length());
    printBorder();
    System.out.print("  " + RD + B + "Confirm (y/n) > " + R);
    String ans = ConsoleUI.readLine().trim().toLowerCase();
    return ans.equals("y") || ans.equals("yes");
  }

  /** Asks for a room number (e.g. R101), or returns null when 0 cancels. */
  public String inputRoomNumber() {
    System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
    while (true) {
      System.out.print("  " + SB + "Room No." + R + " (e.g. R101, 0 to cancel) > ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.equals("0")) return null;
      if (value.matches("R[0-9]{3,4}")) return value;
      MessageUI.displayErrorMessage(
          "Room number must be R followed by 3-4 digits (e.g. R101). Enter 0 to cancel.");
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

  /** Asks what kind of cleaning task it is (1-4 menu), or returns null when 0 cancels. */
  public String inputTaskType() {
    System.out.println();
    System.out.println("  " + SB + B + "TASK TYPE" + R);
    System.out.println("  " + rep('-', 40));
    System.out.println("  " + SB + "[1]" + R + " Checkout Clean    "
        + SB + "[2]" + R + " Deep Clean");
    System.out.println("  " + SB + "[3]" + R + " Turndown          "
        + SB + "[4]" + R + " Inspection");
    System.out.println("  " + RD + "[0]" + R + " Cancel");
    System.out.println();
    while (true) {
      switch (ConsoleUI.readMenuChoice("  Select task type (0-4) > ")) {
        case 0: return null;
        case 1: return "CHECKOUT_CLEAN";
        case 2: return "DEEP_CLEAN";
        case 3: return "TURNDOWN";
        case 4: return "INSPECTION";
        default: MessageUI.displayErrorMessage("Enter a number from 0 to 4.");
      }
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

  /** Shows all cleaning tasks in the order they were created. */
  public void listTaskQueue(String output) {
    System.out.println();
    printBorder(ACTIVE_ROOMS_BOX_W);
    rowV("  " + C + B + "HOUSEKEEPING TASK QUEUE" + R,
        2 + "HOUSEKEEPING TASK QUEUE".length(), ACTIVE_ROOMS_BOX_W);
    printBorder(ACTIVE_ROOMS_BOX_W);

    String hdr = String.format("  %-8s %-8s %-10s %-16s %-22s %s",
        "Task ID", "Room", "Staff", "Task Type", "Status", "Logged At");
    rowV(IB + B + hdr + R, hdr.length(), ACTIVE_ROOMS_BOX_W);
    rowV("  " + rep('-', 96), 2 + 96, ACTIVE_ROOMS_BOX_W);
    if (output.isEmpty()) {
      String message = "  (No tasks in queue)";
      rowV(DM + message + R, message.length(), ACTIVE_ROOMS_BOX_W);
    } else {
      for (String aux : output.split("\n")) {
        if (aux.isEmpty()) continue;
        rowV("  " + WH + aux + R, 2 + aux.length(), ACTIVE_ROOMS_BOX_W);
      }
    }
    printBorder(ACTIVE_ROOMS_BOX_W);
    System.out.println();
  }

  /** Shows the current status of every room at a glance. */
  public void listRoomStatuses(String output) {
    System.out.println();
    printBorder();
    rowV("  " + C + B + "ROOM STATUS BOARD" + R,
        2 + "ROOM STATUS BOARD".length());
    printBorder();

    String hdr = String.format("  %-12s %-16s %-12s %-30s",
        "Room", "Type", "Floor", "Status");
    rowV(IB + B + hdr + R, hdr.length());
    rowV("  " + rep('-', 73), 2 + 73);
    if (output.isEmpty()) {
      String message = "  (No rooms registered)";
      rowV(DM + message + R, message.length());
    } else {
      for (String aux : output.split("\n")) {
        if (aux.isEmpty()) continue;
        rowV("  " + WH + aux + R, 2 + aux.length());
      }
    }
    printBorder();
    System.out.println();
  }

  /** Shows the full details of one cleaning task. */
  public void displayTaskDetails(HousekeepingTask task) {
    displayTaskDetails(task, "TASK CREATED", "Added to sequential log.");
  }

  /** Shows a task in a bordered card for create, search, and delete actions. */
  public void displayTaskDetails(HousekeepingTask task, String title, String subtitle) {
    System.out.println();
    printBorder();
    rowV("  " + C + B + title + R, 2 + title.length());
    if (subtitle != null && !subtitle.isEmpty()) {
      rowV("  " + DM + subtitle + R, 2 + subtitle.length());
    }
    printBorder();

    String taskId = "  Task ID   : " + task.getTaskId();
    String room = "  Room      : " + task.getRoomNumber();
    String staff = "  Staff     : " + task.getAssignedStaff();
    String taskType = "  Task Type : " + task.getTaskType();
    String status = "  Status    : " + task.getCurrentStatus().getLabel();
    String loggedAt = "  Logged At : " + task.getLoggedAt().format(MalaysiaTime.FORMATTER);
    rowV("  Task ID   : " + C + B + task.getTaskId() + R, taskId.length());
    rowV(room, room.length());
    rowV(staff, staff.length());
    rowV(taskType, taskType.length());
    rowV("  Status    : " + colorStatus(task.getCurrentStatus()), status.length());
    rowV("  Logged At : " + DM + task.getLoggedAt().format(MalaysiaTime.FORMATTER) + R,
        loggedAt.length());
    printBorder();
    System.out.println();
  }

  /** Shows the full details of one room. */
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

  /** Asks for Report 1 filters: from/to dates, status, room type. */
  public String[] inputReport1Filters() {
    sectionHeader("REPORT 1 - SET FILTERS", "Leave date blank to include all dates.");
    System.out.println("  " + DM
        + "Date: yyyy-MM-dd  |  Status: 0=ALL 1=Dirty 2=Cleaning 3=Inspected 4=Ready 5=LCO"
        + R);
    System.out.println("  " + DM
        + "Room Type: 0=ALL  1=Standard  2=Deluxe  3=Suite" + R);
    System.out.println();

    String from = inputOptionalDate("From date");
    String to;
    while (true) {
      to = inputOptionalDate("To date  ");
      if (from.isEmpty() || to.isEmpty() || !LocalDate.parse(to).isBefore(LocalDate.parse(from))) {
        break;
      }
      MessageUI.displayErrorMessage("To date must be on or after the from date.");
    }

    String statusFilter;
    switch (readChoiceInRange("  Status filter     (0-5) > ", 0, 5)) {
      case 1:  statusFilter = "DIRTY"; break;
      case 2:  statusFilter = "CLEANING_IN_PROGRESS"; break;
      case 3:  statusFilter = "INSPECTED"; break;
      case 4:  statusFilter = "READY_FOR_CHECK_IN"; break;
      case 5:  statusFilter = "LCO"; break;
      default: statusFilter = "ALL";
    }

    String roomTypeFilter;
    switch (readChoiceInRange("  Room type filter  (0-3) > ", 0, 3)) {
      case 1:  roomTypeFilter = "Standard"; break;
      case 2:  roomTypeFilter = "Deluxe"; break;
      case 3:  roomTypeFilter = "Suite"; break;
      default: roomTypeFilter = "ALL";
    }

    return new String[]{from, to, statusFilter, roomTypeFilter};
  }

  /** Asks if the user wants to binary search for a specific task or room. */
  public int inputReport1SearchOption() {
    System.out.println();
    System.out.println("  " + SB + B + "BINARY SEARCH OPTION" + R);
    System.out.println("  " + rep('-', 40));
    System.out.println("  " + SB + "[0]" + R + " None - show full report");
    System.out.println("  " + SB + "[1]" + R + " Search by Task ID");
    System.out.println("  " + SB + "[2]" + R + " Search by Room Number");
    System.out.println();
    while (true) {
      int choice = ConsoleUI.readMenuChoice("  Select search option (0-2) > ");
      if (choice >= 0 && choice <= 2) return choice;
      MessageUI.displayErrorMessage("Enter a number from 0 to 2.");
    }
  }

  public String inputSearchTaskId() {
    System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
    while (true) {
      System.out.print("  " + SB + "Task ID to search" + R + " (e.g. T1001, 0 to cancel) > ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.equals("0")) return null;
      if (value.matches("(T|HK)[0-9]+")) return value;
      MessageUI.displayErrorMessage(
          "Task ID must start with T (or HK) followed by digits. Enter 0 to cancel.");
    }
  }

  public String inputSearchRoomNumber() {
    System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
    while (true) {
      System.out.print("  " + SB + "Room No. to search" + R + " (e.g. R101, 0 to cancel) > ");
      String value = ConsoleUI.readLine().trim().toUpperCase();
      if (value.equals("0")) return null;
      if (value.matches("R[0-9]{3,4}")) return value;
      MessageUI.displayErrorMessage(
          "Room number must be R followed by 3-4 digits (e.g. R101). Enter 0 to cancel.");
    }
  }

  /** Asks for Report 2 filters: staff prefix + minimum task count. */
  public String[] inputReport2Filters() {
    sectionHeader("REPORT 2 - SET FILTERS",
        "Filter by staff prefix and minimum task count.");
    System.out.println("  " + DM
        + "Prefix e.g. HK matches HK001, HK002...  |  Min tasks: 0 = show all"
        + R);
    System.out.println();

    String prefix;
    while (true) {
      System.out.print("  " + SB + "Staff prefix" + R + " (HK, HK001, or ENTER for all) > ");
      prefix = ConsoleUI.readLine().trim().toUpperCase();
      if (prefix.isEmpty() || prefix.matches("HK[0-9]{0,5}")) break;
      MessageUI.displayErrorMessage("Staff prefix must be HK followed by up to 5 digits.");
    }

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

  /** Reads an integer menu option and keeps prompting until it is in range. */
  private int readChoiceInRange(String prompt, int minimum, int maximum) {
    while (true) {
      int choice = ConsoleUI.readMenuChoice(prompt);
      if (choice >= minimum && choice <= maximum) return choice;
      MessageUI.displayErrorMessage("Enter a number from " + minimum + " to " + maximum + ".");
    }
  }

  /** Reads an optional calendar date and rejects impossible dates such as 2026-02-30. */
  private String inputOptionalDate(String label) {
    while (true) {
      System.out.print("  " + SB + label + R + " (yyyy-MM-dd or ENTER) > ");
      String value = ConsoleUI.readLine().trim();
      if (value.isEmpty()) return "";
      try {
        return LocalDate.parse(value).toString();
      } catch (DateTimeParseException ex) {
        MessageUI.displayErrorMessage("Enter a valid calendar date in yyyy-MM-dd format.");
      }
    }
  }

  /** Asks whether the user wants to save the report as a PDF. */
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

  /** Shows the allowed room cleaning stages in order. */
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
  // Status Change Control — show-then-confirm flow (options 6-11)
  // ======================================================================

  /** Asks the user to confirm an action with a clear yes/no prompt. */
  public boolean confirmAction(String prompt) {
    System.out.println();
    System.out.print("  " + WH + B + prompt + "  "
        + SB + B + "[Y] Yes" + R + "    "
        + RD + B + "[N] No" + R + " > ");
    String answer = ConsoleUI.readLine().trim().toLowerCase();
    return answer.equals("y") || answer.equals("yes");
  }

  /** [6] Shows the latest change that will be undone before asking for confirmation. */
  public void displayUndoLatest(StatusChangeRecord record) {
    openPanel("UNDO LATEST CHANGE");
    panelText("The latest status change is:", SB);
    row();
    rowKV("Room", record.getRoomNumber(), true);
    rowKV("Previous", record.getPreviousStatus().getLabel(), false);
    rowKV("Current", record.getNewStatus().getLabel(), false);
    rowKVPair("Change", record.getPreviousStatus().getLabel(),
        record.getNewStatus().getLabel());
    closePanel();
  }

  /** [7] Shows the latest change for a specific room before confirmation. */
  public void displayRoomUndo(String roomNumber, StatusChangeRecord record) {
    openPanel("UNDO ROOM CHANGE");
    rowKV("Room", roomNumber, true);
    row();
    panelText("Latest Change:", SB);
    rowKV("Previous Status", record.getPreviousStatus().getLabel(), false);
    rowKV("Current Status", record.getNewStatus().getLabel(), false);
    rowKVPair("Change", record.getPreviousStatus().getLabel(),
        record.getNewStatus().getLabel());
    closePanel();
  }

  /** [9] Shows the complete change history from newest (top) to oldest (bottom). */
  public void displayChangeHistory(List<StatusChangeRecord> history) {
    openPanel("CHANGE HISTORY");
    panelText("Newest / Latest", "");
    row();
    if (history.isEmpty()) {
      panelText("(No changes available)", DM);
    } else {
      printHistoryRows(history, true);
    }
    row();
    panelText("Oldest", "");
    panelText("Viewing history does not modify any room status.", DM);
    closePanel();
  }

  /** [10] Shows the current number of available rollback changes and the latest entry. */
  public void displayUndoSummary(int undoCount, StatusChangeRecord latestUndo) {
    openPanel("UNDO SUMMARY");
    row();
    rowKV("Available Undo Changes", String.valueOf(undoCount), true);
    row();
    panelText("Latest Undoable Change:", "");
    if (latestUndo == null) {
      panelText("None", WH);
    } else {
      rowText("Room " + latestUndo.getRoomNumber() + ": "
          + latestUndo.getPreviousStatus().getLabel() + " -> "
          + latestUndo.getNewStatus().getLabel(), WH);
    }
    closePanel();
  }

  // ── Private panel helpers (options 6-11) ──────────────────────────────

  private void openPanel(String title) {
    System.out.println();
    printBorder();
    rowV(centerPad(B + C + title + R, title.length()), BOX_W);
    printBorder();
  }

  private void closePanel() {
    printBorder();
    System.out.println();
  }

  /** Prints an empty box row used as spacing inside a panel. */
  private void row() {
    rowV("", 0);
  }

  /** Prints a plain or colored line inside a panel (content starts right after the box bar). */
  private void panelText(String text, String color) {
    if (color == null || color.isEmpty()) {
      rowV(text, text.length());
    } else {
      rowV(color + text + R, text.length());
    }
  }

  /** Prints a line indented two spaces from the box bar. */
  private void rowText(String text, String color) {
    if (color == null || color.isEmpty()) {
      rowV("  " + text, 2 + text.length());
    } else {
      rowV("  " + color + text + R, 2 + text.length());
    }
  }

  /** Prints an aligned "Key : value" row inside a panel. */
  private void rowKV(String key, String value, boolean highlight) {
    String prefix = " " + String.format("%-12s : ", key);
    String colored = highlight ? prefix + WH + B + value + R : prefix + value;
    rowV(colored, visLen(colored));
  }

  /** Prints an aligned "Key : A -> B" change row inside a panel. */
  private void rowKVPair(String key, String from, String to) {
    String prefix = " " + String.format("%-12s : ", key);
    String value = from + " -> " + to;
    rowV(prefix + C + B + value + R, prefix.length() + value.length());
  }

  /** Prints the numbered history entries; withRoomWord adds "Room" before the room number. */
  private void printHistoryRows(List<StatusChangeRecord> history, boolean withRoomWord) {
    int position = 1;
    for (StatusChangeRecord record : history) {
      String roomPart = (withRoomWord ? "Room " : "") + record.getRoomNumber();
      String entry = String.format(" [%d] %-8s %s -> %s", position++,
          roomPart, record.getPreviousStatus().getLabel(),
          record.getNewStatus().getLabel());
      rowV(WH + entry + R, entry.length());
    }
  }

  /** Returns the visible length of a colored string (ANSI escape codes excluded). */
  private int visLen(String content) {
    return content.replaceAll("\033\\[[0-9;]*m", "").length();
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
      case OCCUPIED:             return "\u001B[45m\u001B[97m OCCUPIED \u001B[0m";
      case LCO:                  return "\u001B[46m\u001B[30m LCO \u001B[0m";
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
