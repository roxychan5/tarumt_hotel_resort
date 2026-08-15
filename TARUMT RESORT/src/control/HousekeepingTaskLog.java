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
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import utility.MessageUI;
import utility.PdfReportEngine;

/**
 * Control class for Housekeeping and Task Log module.
 * Uses Linear ADTs: an ArrayList for the sequential task/room log and a
 * two LinkedStacks for LIFO undo/redo, bulk rollback and history preview.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTaskLog {

  // Linear List ADT: rooms remain in their registered sequential order.
  private final ListInterface<Room> roomList = new ArrayList<>();
  // Linear List ADT: each new cleaning task is appended to the task log.
  private final ListInterface<HousekeepingTask> taskList = new ArrayList<>();
  // Linear Stack ADT: newest applied change is at the top for immediate LIFO undo.
  private final StackInterface<StatusChangeRecord> undoStack = new LinkedStack<>();
  // Second Linear Stack ADT: undone changes are held here for LIFO redo.
  private final StackInterface<StatusChangeRecord> redoStack = new LinkedStack<>();
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
          undoLastChange();
          break;
        case 5:
          redoLastChange();
          break;
        case 6:
          rollbackMultipleChanges();
          break;
        case 7:
          rollbackSpecificRoom();
          break;
        case 8:
          displayStatusHistory();
          break;
        case 9:
          displayStackStatistics();
          break;
        case 10:
          handleLateCheckout();
          break;
        case 11:
          housekeepingUI.listRoomStatuses(getAllRooms());
          MessageUI.pressEnterToContinue();
          break;
        case 12:
          generateTasksByStatusReport();
          break;
        case 13:
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

  /** Moves the latest applied change from the undo stack to the redo stack. */
  private void undoLastChange() {
    if (undoStack.isEmpty()) {
      MessageUI.displayErrorMessage("No housekeeping changes available to undo.");
      return;
    }

    String reason = housekeepingUI.inputRollbackReason();
    StatusChangeRecord record = undoStack.pop();
    Room room = findRoom(record.getRoomNumber());
    if (room == null) {
      MessageUI.displayErrorMessage("Room no longer exists. Rollback cancelled.");
      undoStack.push(record);
      return;
    }

    room.setStatus(record.getPreviousStatus());
    syncTaskStatus(record.getRoomNumber(), record.getPreviousStatus());
    redoStack.push(record);
    saveData();

    System.out.println("\nUndone: " + record);
    MessageUI.displaySuccessMessage("Latest change undone. Reason: " + reason);
    housekeepingUI.displayRoomDetails(room);
    MessageUI.pressEnterToContinue();
  }

  /** Reapplies the latest undone change, moving it back to the undo stack. */
  private void redoLastChange() {
    if (redoStack.isEmpty()) {
      MessageUI.displayErrorMessage("No housekeeping changes available to redo.");
      return;
    }
    StatusChangeRecord record = redoStack.pop();
    Room room = findRoom(record.getRoomNumber());
    if (room == null) {
      MessageUI.displayErrorMessage("Room no longer exists. Redo cancelled.");
      redoStack.push(record);
      return;
    }
    room.setStatus(record.getNewStatus());
    syncTaskStatus(record.getRoomNumber(), record.getNewStatus());
    undoStack.push(record);
    saveData();
    MessageUI.displaySuccessMessage("Latest change redone.");
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

  /** Restores several consecutive status changes in LIFO order. */
  private void rollbackMultipleChanges() {
    if (undoStack.isEmpty()) {
      MessageUI.displayErrorMessage("No status changes to roll back.");
      return;
    }

    int count = housekeepingUI.inputRollbackCount(undoStack.getSize());
    for (int i = 0; i < count; i++) {
      StatusChangeRecord record = undoStack.pop();
      Room room = findRoom(record.getRoomNumber());
      if (room != null) {
        room.setStatus(record.getPreviousStatus());
        syncTaskStatus(record.getRoomNumber(), record.getPreviousStatus());
      }
      redoStack.push(record);
    }
    saveData();
    MessageUI.displaySuccessMessage(
        count + " latest status change(s) rolled back in LIFO order.");
    MessageUI.pressEnterToContinue();
  }

  /** Displays the rollback stack without changing its order or contents. */
  private void displayStatusHistory() {
    if (undoStack.isEmpty()) {
      MessageUI.displayInfoMessage("No status change history available.");
      MessageUI.pressEnterToContinue();
      return;
    }

    StackInterface<StatusChangeRecord> temporaryStack = new LinkedStack<>();
    StringBuilder output = new StringBuilder();
    output.append("TOP (latest applied change / next item to undo)\n");
    output.append("--------------------------------------------------\n");
    int position = 1;
    while (!undoStack.isEmpty()) {
      StatusChangeRecord record = undoStack.pop();
      output.append(position++).append(". ").append(record).append('\n');
      temporaryStack.push(record);
    }
    while (!temporaryStack.isEmpty()) {
      undoStack.push(temporaryStack.pop());
    }
    output.append("BOTTOM\n");
    output.append("\nTotal changes available for undo: ").append(undoStack.getSize());
    housekeepingUI.displayReport("HOUSEKEEPING STATUS CHANGE HISTORY", output.toString());
    MessageUI.pressEnterToContinue();
  }

  /** Uses O(1) stack sizes and peek to make the two-stack design visible. */
  private void displayStackStatistics() {
    StringBuilder output = new StringBuilder();
    output.append("Undoable changes : ").append(undoStack.getSize()).append('\n');
    output.append("Redoable changes : ").append(redoStack.getSize()).append('\n');
    output.append("Latest undo item : ");
    output.append(undoStack.isEmpty() ? "None" : undoStack.peek()).append('\n');
    output.append("Latest redo item : ");
    output.append(redoStack.isEmpty() ? "None" : redoStack.peek());
    housekeepingUI.displayReport("UNDO / REDO STACK STATISTICS", output.toString());
    MessageUI.pressEnterToContinue();
  }

  /** Removes only the latest status change for the requested room. */
  private void rollbackSpecificRoom() {
    String roomNumber = housekeepingUI.inputRoomNumber();
    Room room = findRoom(roomNumber);
    if (room == null) {
      MessageUI.displayErrorMessage("Room not found. Rollback cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }
    StackInterface<StatusChangeRecord> temporaryStack = new LinkedStack<>();
    StatusChangeRecord target = null;
    while (!undoStack.isEmpty()) {
      StatusChangeRecord record = undoStack.pop();
      if (record.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        target = record;
        break;
      }
      temporaryStack.push(record);
    }
    // Restoring temporary entries preserves the original order of all other records.
    while (!temporaryStack.isEmpty()) {
      undoStack.push(temporaryStack.pop());
    }
    if (target == null) {
      MessageUI.displayErrorMessage("No undoable status history was found for " + roomNumber + ".");
      MessageUI.pressEnterToContinue();
      return;
    }
    room.setStatus(target.getPreviousStatus());
    syncTaskStatus(roomNumber, target.getPreviousStatus());
    redoStack.push(target);
    saveData();
    MessageUI.displaySuccessMessage("Latest status change for " + roomNumber + " was rolled back.");
    housekeepingUI.displayRoomDetails(room);
    MessageUI.pressEnterToContinue();
  }

  // ═══════════════════════════════════════════════════════════════════════
  // REPORT 1 — Housekeeping Operational Summary
  //   Algorithm: Binary search (date range) + Bubble sort (status priority)
  //              Multi-criteria filter (date + status + room type)
  // ═══════════════════════════════════════════════════════════════════════
  private void generateTasksByStatusReport() {
    housekeepingUI.displayReportIntro(
        "REPORT 1: HOUSEKEEPING OPERATIONAL SUMMARY",
        "Combines binary search on sorted tasks and bubble sort by status priority.\n"
        + "  Filters tasks by date range, status, and room type for targeted analysis.");

    // ── Step 1: Collect filter criteria from supervisor ───────────────────
    String[] filters = housekeepingUI.inputReport1Filters();
    String fromDateStr  = filters[0];   // "yyyy-MM-dd" or empty
    String toDateStr    = filters[1];   // "yyyy-MM-dd" or empty
    String statusFilter = filters[2];   // RoomStatus name or "ALL"
    String roomTypeFilter = filters[3]; // "Standard","Deluxe","Suite" or "ALL"

    LocalDate fromDate = fromDateStr.isEmpty() ? null : LocalDate.parse(fromDateStr);
    LocalDate toDate   = toDateStr.isEmpty()   ? null : LocalDate.parse(toDateStr);

    // ── Step 2: Sort all tasks by loggedAt (bubble sort) ─────────────────
    //           Required so binary search can work on a sorted array.
    int n = taskList.getNumberOfEntries();
    HousekeepingTask[] sorted = new HousekeepingTask[n];
    for (int i = 0; i < n; i++) sorted[i] = taskList.getEntry(i + 1);

    // Bubble sort ascending by loggedAt
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < n - i - 1; j++) {
        if (sorted[j].getLoggedAt().isAfter(sorted[j + 1].getLoggedAt())) {
          HousekeepingTask tmp = sorted[j];
          sorted[j] = sorted[j + 1];
          sorted[j + 1] = tmp;
        }
      }
    }

    // ── Step 3: Binary search to find date range boundaries ───────────────
    //           Locate the first index >= fromDate and last index <= toDate.
    int lo = 0, hi = n - 1;
    int startIdx = 0, endIdx = n - 1;

    if (fromDate != null) {
      // Binary search: find leftmost task with loggedAt >= fromDate
      lo = 0; hi = n - 1; startIdx = n;
      while (lo <= hi) {
        int mid = (lo + hi) / 2;
        if (!sorted[mid].getLoggedAt().toLocalDate().isBefore(fromDate)) {
          startIdx = mid; hi = mid - 1;
        } else {
          lo = mid + 1;
        }
      }
    }

    if (toDate != null) {
      // Binary search: find rightmost task with loggedAt <= toDate
      lo = 0; hi = n - 1; endIdx = -1;
      while (lo <= hi) {
        int mid = (lo + hi) / 2;
        if (!sorted[mid].getLoggedAt().toLocalDate().isAfter(toDate)) {
          endIdx = mid; lo = mid + 1;
        } else {
          hi = mid - 1;
        }
      }
    }

    // ── Step 4: Multi-criteria filter within the date range ───────────────
    java.util.List<HousekeepingTask> filtered = new java.util.ArrayList<>();
    for (int i = startIdx; i <= endIdx && i < n; i++) {
      HousekeepingTask t = sorted[i];
      boolean passStatus   = statusFilter.equals("ALL")
          || t.getCurrentStatus().name().equals(statusFilter);
      boolean passRoomType = roomTypeFilter.equals("ALL");
      if (!passRoomType) {
        Room room = findRoom(t.getRoomNumber());
        passRoomType = room != null && room.getRoomType().equalsIgnoreCase(roomTypeFilter);
      }
      if (passStatus && passRoomType) filtered.add(t);
    }

    // ── Step 5: Bubble sort the filtered tasks by status priority ─────────
    //           Priority: DIRTY > CLEANING_IN_PROGRESS > INSPECTED > READY
    int fn = filtered.size();
    for (int i = 0; i < fn - 1; i++) {
      for (int j = 0; j < fn - i - 1; j++) {
        if (filtered.get(j).getCurrentStatus().ordinal()
            > filtered.get(j + 1).getCurrentStatus().ordinal()) {
          HousekeepingTask tmp = filtered.get(j);
          filtered.set(j, filtered.get(j + 1));
          filtered.set(j + 1, tmp);
        }
      }
    }

    // ── Step 6: Build console report ─────────────────────────────────────
    StringBuilder consoleReport = new StringBuilder();
    consoleReport.append(String.format("  Filter — Date: %s to %s | Status: %s | Room Type: %s%n",
        fromDateStr.isEmpty() ? "(any)" : fromDateStr,
        toDateStr.isEmpty()   ? "(any)" : toDateStr,
        statusFilter, roomTypeFilter));
    consoleReport.append("\n");
    consoleReport.append(String.format("  %-8s %-8s %-10s %-16s %-22s %s%n",
        "Task ID", "Room", "Staff", "Task Type", "Status", "Logged"));
    consoleReport.append("  " + "-".repeat(88) + "\n");

    Map<String,Integer> statusCount  = new LinkedHashMap<>();
    Map<String,Integer> roomTypeCount = new LinkedHashMap<>();
    for (RoomStatus rs : RoomStatus.values()) statusCount.put(rs.getLabel(), 0);

    for (HousekeepingTask t : filtered) {
      Room room = findRoom(t.getRoomNumber());
      String rType = room != null ? room.getRoomType() : "Unknown";
      consoleReport.append(String.format("  %-8s %-8s %-10s %-16s %-22s %s%n",
          t.getTaskId(), t.getRoomNumber(), t.getAssignedStaff(),
          t.getTaskType(), t.getCurrentStatus().getLabel(),
          t.getLoggedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
      statusCount.merge(t.getCurrentStatus().getLabel(), 1, Integer::sum);
      roomTypeCount.merge(rType, 1, Integer::sum);
    }

    consoleReport.append("\n  STATUS SUMMARY\n");
    consoleReport.append("  " + "-".repeat(40) + "\n");
    for (Map.Entry<String,Integer> e : statusCount.entrySet()) {
      consoleReport.append(String.format("  %-24s %4d%n", e.getKey(), e.getValue()));
    }
    consoleReport.append(String.format("%n  Total tasks matching criteria: %d%n", filtered.size()));

    housekeepingUI.displayReport("REPORT 1: HOUSEKEEPING OPERATIONAL SUMMARY",
        consoleReport.toString());

    // ── Step 7: Export to PDF ─────────────────────────────────────────────
    if (housekeepingUI.confirmPdfExport()) {
      exportReport1ToPdf(filtered, statusCount, roomTypeCount,
          fromDateStr, toDateStr, statusFilter, roomTypeFilter);
    }
    MessageUI.pressEnterToContinue();
  }

  private void exportReport1ToPdf(java.util.List<HousekeepingTask> filtered,
      Map<String,Integer> statusCount, Map<String,Integer> roomTypeCount,
      String fromDate, String toDate, String statusFilter, String roomTypeFilter) {
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String outPath = outDir + File.separator + "housekeeping_summary_" + timestamp + ".pdf";

      PdfReportEngine pdf = new PdfReportEngine();

      // Cover page
      String period = (fromDate.isEmpty() ? "All dates" : fromDate)
          + " to " + (toDate.isEmpty() ? "All dates" : toDate);
      pdf.addCoverPage(
          "Housekeeping Operational Summary",
          "Tasks by Status | Room Type Distribution | Filtered Analysis",
          period, "Housekeeping Supervisor");

      // Content page 1 — KPIs + bar chart
      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type",   "Housekeeping Operational Summary", null);
      pdf.addKpiRow("Date Range",    period, null);
      pdf.addKpiRow("Status Filter", statusFilter, null);
      pdf.addKpiRow("Room Type Filter", roomTypeFilter, null);
      pdf.addKpiRow("Tasks Matched", String.valueOf(filtered.size()),
          filtered.isEmpty() ? PdfReportEngine.DANGER : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      // KPI cards
      long dirty    = statusCount.getOrDefault("Dirty", 0);
      long cleaning = statusCount.getOrDefault("Cleaning In Progress", 0);
      long inspected= statusCount.getOrDefault("Inspected", 0);
      long ready    = statusCount.getOrDefault("Ready for Check-In", 0);
      pdf.addSectionHeading("Key Performance Indicators");
      pdf.addKpiCards(
          new String[]{"Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"},
          new String[]{String.valueOf(dirty), String.valueOf(cleaning),
                       String.valueOf(inspected), String.valueOf(ready)},
          new Color[]{ PdfReportEngine.DANGER, PdfReportEngine.WARNING,
                       PdfReportEngine.ACCENT_BLUE, PdfReportEngine.SUCCESS });
      pdf.addSpace(10);

      // Bar chart — tasks per status
      String[] sLabels = statusCount.keySet().toArray(new String[0]);
      double[] sValues = statusCount.values().stream()
          .mapToDouble(Integer::doubleValue).toArray();
      pdf.addBarChart("Tasks by Status", sLabels, sValues, "Number of Tasks");

      // Donut chart — room type distribution
      if (!roomTypeCount.isEmpty()) {
        String[] rtLabels = roomTypeCount.keySet().toArray(new String[0]);
        double[] rtValues = roomTypeCount.values().stream()
            .mapToDouble(Integer::doubleValue).toArray();
        pdf.addSectionHeading("Room Type Distribution");
        pdf.addDonutChart("Tasks by Room Type", rtLabels, rtValues);
      }

      // Detailed data table
      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed Task List (Sorted by Status Priority)");
      pdf.addBodyText(
          "Tasks are sorted using Bubble Sort by status urgency: Dirty > Cleaning > Inspected > Ready.",
          9);
      pdf.addSpace(6);

      String[] headers = {"Task ID","Room","Staff","Task Type","Status","Logged At"};
      float[] colW = {60, 50, 60, 90, 110, 120};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      for (HousekeepingTask t : filtered) {
        rows.add(new String[]{
            t.getTaskId(), t.getRoomNumber(), t.getAssignedStaff(),
            t.getTaskType(), t.getCurrentStatus().getLabel(),
            t.getLoggedAt().format(dtFmt)
        });
      }
      if (rows.isEmpty()) {
        pdf.addBodyText("  No tasks match the selected filter criteria.", 10);
      } else {
        pdf.addTable(headers, rows, colW);
      }

      pdf.save(outPath);
      housekeepingUI.displayPdfExportSuccess(outPath);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // REPORT 2 — Staff Workload & Performance Analysis
  //   Algorithm: Linear search (collect unique staff IDs + filter by prefix)
  //              Insertion sort (workload descending)
  //              Multi-criteria filter (staff prefix + status completion rate)
  // ═══════════════════════════════════════════════════════════════════════
  private void generateStaffWorkloadReport() {
    housekeepingUI.displayReportIntro(
        "REPORT 2: STAFF WORKLOAD & PERFORMANCE ANALYSIS",
        "Uses insertion sort to rank staff by total workload (descending).\n"
        + "  Filters by staff ID prefix and completion threshold for targeted review.");

    // ── Step 1: Collect filter criteria ───────────────────────────────────
    String[] filters = housekeepingUI.inputReport2Filters();
    String staffPrefix       = filters[0]; // e.g. "HK" or empty for all
    int    minTasksThreshold = Integer.parseInt(filters[1]); // minimum tasks

    // ── Step 2: Linear search — collect unique staff IDs matching prefix ──
    ListInterface<String> staffIds = new ArrayList<>();
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      String staffId = taskList.getEntry(i).getAssignedStaff();
      boolean matchPrefix = staffPrefix.isEmpty()
          || staffId.toUpperCase().startsWith(staffPrefix.toUpperCase());
      if (matchPrefix && !staffIds.contains(staffId)) {
        staffIds.add(staffId);
      }
    }

    // ── Step 3: Insertion sort — rank staff by total tasks (descending) ───
    int m = staffIds.getNumberOfEntries();
    String[] staffArr = new String[m];
    for (int i = 0; i < m; i++) staffArr[i] = staffIds.getEntry(i + 1);

    for (int i = 1; i < m; i++) {
      String key = staffArr[i];
      int keyTasks = countTasksForStaff(key);
      int j = i - 1;
      while (j >= 0 && countTasksForStaff(staffArr[j]) < keyTasks) {
        staffArr[j + 1] = staffArr[j];
        j--;
      }
      staffArr[j + 1] = key;
    }

    // ── Step 4: Multi-criteria filter — apply minimum tasks threshold ──────
    java.util.List<String> qualifiedStaff = new java.util.ArrayList<>();
    for (String s : staffArr) {
      if (countTasksForStaff(s) >= minTasksThreshold) {
        qualifiedStaff.add(s);
      }
    }

    // ── Step 5: Build console report ──────────────────────────────────────
    StringBuilder consoleReport = new StringBuilder();
    consoleReport.append(String.format(
        "  Filter — Staff Prefix: \"%s\" | Min Tasks: %d%n%n",
        staffPrefix.isEmpty() ? "(all)" : staffPrefix, minTasksThreshold));
    consoleReport.append(String.format("  %-3s %-12s %6s %8s %12s %s%n",
        "#", "Staff ID", "Total", "Pending", "Completed", "Load Status"));
    consoleReport.append("  " + "-".repeat(60) + "\n");

    int rank = 1;
    int totalTasks = 0, totalPending = 0;
    for (String staffId : qualifiedStaff) {
      int tasks   = countTasksForStaff(staffId);
      int pending = countPendingTasksForStaff(staffId);
      int completed = tasks - pending;
      totalTasks += tasks; totalPending += pending;
      String flag = tasks > 3 ? "[OVERLOADED]" : tasks > 1 ? "[OPTIMAL]" : "[LIGHT]";
      consoleReport.append(String.format("  %-3d %-12s %6d %8d %12d %s%n",
          rank++, staffId, tasks, pending, completed, flag));
    }
    if (qualifiedStaff.isEmpty()) {
      consoleReport.append("  (No staff matching filter criteria)\n");
    } else {
      consoleReport.append("  " + "-".repeat(60) + "\n");
      consoleReport.append(String.format("  %-3s %-12s %6d %8d%n",
          "", "TOTAL", totalTasks, totalPending));
    }

    housekeepingUI.displayReport("REPORT 2: STAFF WORKLOAD & PERFORMANCE ANALYSIS",
        consoleReport.toString());

    // ── Step 6: Export to PDF ─────────────────────────────────────────────
    if (housekeepingUI.confirmPdfExport()) {
      exportReport2ToPdf(qualifiedStaff, staffPrefix, minTasksThreshold,
          totalTasks, totalPending);
    }
    MessageUI.pressEnterToContinue();
  }

  private void exportReport2ToPdf(java.util.List<String> qualifiedStaff,
      String staffPrefix, int minTasks, int totalTasks, int totalPending) {
    try {
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String outPath = outDir + File.separator + "staff_workload_" + timestamp + ".pdf";

      PdfReportEngine pdf = new PdfReportEngine();

      // Cover page
      pdf.addCoverPage(
          "Staff Workload & Performance Analysis",
          "Insertion Sort Ranking | Pending vs Completed | Load Status Flags",
          "Current business cycle", "Housekeeping Supervisor");

      // Content page 1 — summary + horizontal bar chart
      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type",       "Staff Workload & Performance Analysis", null);
      pdf.addKpiRow("Staff Prefix Filter", staffPrefix.isEmpty() ? "All Staff" : staffPrefix, null);
      pdf.addKpiRow("Min Tasks Threshold", String.valueOf(minTasks), null);
      pdf.addKpiRow("Staff Evaluated",    String.valueOf(qualifiedStaff.size()),
          PdfReportEngine.ACCENT_BLUE);
      pdf.addKpiRow("Total Tasks",        String.valueOf(totalTasks), null);
      pdf.addKpiRow("Total Pending",      String.valueOf(totalPending),
          totalPending > 0 ? PdfReportEngine.WARNING : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      // KPI cards
      pdf.addSectionHeading("Key Performance Indicators");
      int totalCompleted = totalTasks - totalPending;
      int pct = totalTasks > 0 ? (totalCompleted * 100 / totalTasks) : 0;
      pdf.addKpiCards(
          new String[]{"Staff Evaluated", "Total Tasks", "Pending", "Completion Rate"},
          new String[]{String.valueOf(qualifiedStaff.size()),
                       String.valueOf(totalTasks),
                       String.valueOf(totalPending),
                       pct + "%"},
          new Color[]{ PdfReportEngine.BRAND_TEAL, PdfReportEngine.ACCENT_BLUE,
                       PdfReportEngine.WARNING, PdfReportEngine.SUCCESS });
      pdf.addSpace(10);

      // Horizontal bar chart — total vs pending per staff
      if (!qualifiedStaff.isEmpty()) {
        String[] labels = qualifiedStaff.toArray(new String[0]);
        double[] totals  = new double[labels.length];
        double[] pending = new double[labels.length];
        for (int i = 0; i < labels.length; i++) {
          totals[i]  = countTasksForStaff(labels[i]);
          pending[i] = countPendingTasksForStaff(labels[i]);
        }
        pdf.addSectionHeading("Staff Workload Comparison (Insertion Sort — Highest First)");
        pdf.addBodyText(
            "Staff ranked by total tasks using Insertion Sort (descending). "
            + "Blue = Total Tasks, Orange = Pending Tasks.", 9);
        pdf.addSpace(4);
        pdf.addHorizontalBarChart("Total vs Pending Tasks per Staff",
            labels, new double[][]{totals, pending},
            new String[]{"Total Tasks", "Pending Tasks"});
      }

      // Detailed ranked table
      pdf.beginContentPage();
      pdf.addSectionHeading("Staff Performance Ranking");
      pdf.addBodyText(
          "Sorted by total workload (descending). Flags: [OVERLOADED] >3 tasks, "
          + "[OPTIMAL] 2-3 tasks, [LIGHT] 1 task.", 9);
      pdf.addSpace(6);

      String[] headers = {"Rank","Staff ID","Total Tasks","Pending","Completed","Load Status"};
      float[] colW = {35, 70, 70, 60, 70, 90};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      int rank = 1;
      for (String staffId : qualifiedStaff) {
        int tasks     = countTasksForStaff(staffId);
        int pendingN  = countPendingTasksForStaff(staffId);
        int completedN= tasks - pendingN;
        String flag   = tasks > 3 ? "OVERLOADED" : tasks > 1 ? "OPTIMAL" : "LIGHT";
        rows.add(new String[]{
            String.valueOf(rank++), staffId,
            String.valueOf(tasks), String.valueOf(pendingN),
            String.valueOf(completedN), flag
        });
      }
      if (rows.isEmpty()) {
        pdf.addBodyText("No staff matched the filter criteria.", 10);
      } else {
        pdf.addTable(headers, rows, colW);
      }

      // Recommendations section
      pdf.addSpace(12);
      pdf.addSectionHeading("Management Recommendations");
      long overloaded = qualifiedStaff.stream()
          .filter(s -> countTasksForStaff(s) > 3).count();
      long light = qualifiedStaff.stream()
          .filter(s -> countTasksForStaff(s) == 1).count();
      pdf.addBodyText(
          overloaded > 0
          ? overloaded + " staff member(s) are OVERLOADED. Consider task redistribution."
          : "All staff are within manageable workload limits.", 10);
      pdf.addBodyText(
          light > 0
          ? light + " staff member(s) have LIGHT workloads and may accept additional tasks."
          : "No staff with light workload detected.", 10);
      pdf.addBodyText(
          totalPending > 0
          ? "Action required: " + totalPending + " task(s) remain pending. "
            + "Review priority rooms with Dirty or Cleaning status."
          : "All tasks are completed. Excellent housekeeping performance!", 10);

      pdf.save(outPath);
      housekeepingUI.displayPdfExportSuccess(outPath);
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    }
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
    // Push every change so rollback always starts with the latest action.
    StatusChangeRecord record = new StatusChangeRecord(
        roomNumber, previous, current, reason, LocalDateTime.now());
    undoStack.push(record);
    // A new change makes previously undone actions invalid, as in standard undo/redo.
    redoStack.clear();
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
    StackInterface<StatusChangeRecord> loadedRedoHistory = housekeepingDAO.retrieveRedoHistory();

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

    undoStack.clear();
    StackInterface<StatusChangeRecord> tempStack = new LinkedStack<>();
    while (!loadedHistory.isEmpty()) {
      tempStack.push(loadedHistory.pop());
    }
    while (!tempStack.isEmpty()) {
      undoStack.push(tempStack.pop());
    }

    redoStack.clear();
    while (!loadedRedoHistory.isEmpty()) {
      tempStack.push(loadedRedoHistory.pop());
    }
    while (!tempStack.isEmpty()) {
      redoStack.push(tempStack.pop());
    }
  }

  private void saveData() {
    housekeepingDAO.saveRooms(roomList);
    housekeepingDAO.saveTasks(taskList);
    housekeepingDAO.saveHistory(undoStack);
    housekeepingDAO.saveRedoHistory(redoStack);
  }
}