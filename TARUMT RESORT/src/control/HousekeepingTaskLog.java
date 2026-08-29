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
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import utility.MalaysiaTime;
import utility.MessageUI;
import utility.PdfReportEngine;

/**
 * The BRAIN of the Housekeeping & Task Log module.
 *
 * This class controls three collections:
 *   1. The room list       - a Linear List ADT (ArrayList), rooms keep
 *                            their registered order.
 *   2. The task log        - a Linear List ADT (ArrayList), tasks keep
 *                            the order they were created in.
 *   3. Undo / Redo history - TWO Stack ADTs (LinkedStack).
 *
 * Easy way to think about each ADT:
 *   - ArrayList (List)  : like a queue of papers in the order you added them.
 *   - LinkedStack (LIFO): like a pile of plates - you always take the TOP
 *                         one first (the most recent change).
 *
 * The boundary class (HousekeepingTaskLogUI) collects the user's input,
 * then calls the methods here to do the actual work. After anything
 * changes, this class saves the data back to disk through HousekeepingDAO.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingTaskLog {

  // Linear List ADT: keeps rooms in their original registered sequence.
  private final ListInterface<Room> roomList = new ArrayList<>();
  // Linear List ADT: each new cleaning task is APPENDED to the task log.
  private final ListInterface<HousekeepingTask> taskList = new ArrayList<>();
  // Stack ADT #1: most recent status change sits on TOP -> undo pops it first.
  private final StackInterface<StatusChangeRecord> undoStack = new LinkedStack<>();

  // The DAO reads/saves our data from/to text files (rooms.txt, tasks.txt, etc.).
  private final HousekeepingDAO housekeepingDAO = new HousekeepingDAO();
  // The UI prints menus and collects the user's answers.
  private final HousekeepingTaskLogUI housekeepingUI = new HousekeepingTaskLogUI();

  // This number is used to build unique task IDs, e.g. T1001, T1002...
  // It starts at 1000 so the very first task becomes T1001.
  private int taskCounter = 1000;

  /**
   * Constructor - called automatically once when the module starts.
   *   1. Loads all saved data from the text files.
   *   2. If there are no rooms yet, creates 5 sample rooms (first run).
   */
  public HousekeepingTaskLog() {
    loadData(); // step 1: bring data back from disk
    if (roomList.isEmpty()) {
      seedSampleRooms(); // step 2: first-time setup with example rooms
    }
  }

  /** Java 8-compatible version of String.repeat(): builds a character n times. */
  private static String repeatChar(char c, int count) {
    StringBuilder sb = new StringBuilder(count); // make a builder of that size
    for (int i = 0; i < count; i++) {
      sb.append(c); // add one character each time
    }
    return sb.toString(); // e.g. repeatChar('-', 5) -> "-----"
  }

  /**
   * The main menu loop of the housekeeping module.
   * Keeps asking for a choice (0-15) and runs the matching function,
   * until the user chooses 0 to go back to the main system menu.
   */
  public void runHousekeepingModule() {
    loadData();
    if (roomList.isEmpty()) {
      seedSampleRooms();
    }
    int choice; // what the user picked
    do {
      choice = housekeepingUI.getMenuChoice(); // ask the user what to do
      switch (choice) { // run the right action for that choice
        case 0:
          // User wants to leave this module:
          MessageUI.displayInfoMessage("Returning to main menu...");
          break;
        // ── Task Management (1-5) ──────────────────────────────────────
        case 1:
          housekeepingUI.listTaskQueue(getAllTasks()); // show all tasks
          MessageUI.pressEnterToContinue();           // wait for a key press
          break;
        case 2:
          addCleaningTask(); // create a new cleaning task
          break;
        case 3:
          advanceRoomStatus(); // move a room to its next cleaning stage
          break;
        case 4:
          searchTaskById(); // find one task by its ID
          break;
        case 5:
          deleteTaskById(); // remove a task after confirmation
          break;
        // ── Status Change Control (6-9) ────────────────────────────────
        case 6:
          undoLastChange(); // reverse the latest status change
          break;
        case 7:
          rollbackSpecificRoom(); // undo one specific room's latest change
          break;
        case 8:
          displayStatusHistory(); // show the whole change history
          break;
        case 9:
          displayStackSummary(); // show how many undo/redo entries exist
          break;
        // ── Operations & Reports (10-13) ───────────────────────────────
        case 10:
          handleLateCheckout();
          break;
        case 11:
          housekeepingUI.listRoomStatuses(getAllRooms()); // room status board
          MessageUI.pressEnterToContinue();
          break;
        case 12:
          generateTasksByStatusReport(); // report 1 (console + PDF)
          break;
        case 13:
          generateStaffWorkloadReport(); // report 2 (console + PDF)
          break;
        default:
          MessageUI.displayInvalidChoiceMessage(); // number outside 0-13
      }
    } while (choice != 0); // keep looping until the user says 0 (exit)
  }

  /**
   * Option 2 - Create a brand new cleaning task.
   * Flow:
   *   1. Show which rooms are free / busy.
   *   2. Ask for a room number.
   *   3. Validate: room must exist, be DIRTY, and have no active task.
   *   4. Auto-pick the staff member with the FEWEST active tasks.
   *   5. Next unique task ID (T1001, T1002, ...).
   *   6. Add the task to the ArrayList (task log).
   *   7. Change the room from DIRTY to CLEANING_IN_PROGRESS.
   *   8. Save everything and show the user the new task.
   */
  private void addCleaningTask() {
    // ── Show the occupied rooms so the supervisor knows what's unavailable ──
    housekeepingUI.displayActiveRooms(getActiveRoomSummary());

    String roomNumber = housekeepingUI.inputRoomNumber(); // e.g. R101
    if (roomNumber == null) {
      MessageUI.displayInfoMessage("Add cleaning task cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }
    Room room = findRoom(roomNumber); // find the Room object
    if (room == null) {
      MessageUI.displayErrorMessage("Room " + roomNumber + " not found.");
      MessageUI.pressEnterToContinue();
      return; // stop - no such room
    }

    // ── Guard: only DIRTY rooms with no existing task can get a task ──
    // Search the task list BACKWARDS to find the newest task for this room.
    HousekeepingTask activeTask = findActiveTaskForRoom(roomNumber);

    // Refuse if: room is not Dirty, OR it already has an active task.
    if (room.getStatus() != RoomStatus.DIRTY || activeTask != null) {
      MessageUI.displayErrorMessage(
          "Room " + roomNumber + " already has an active assignment - status: "
          + room.getStatus().getLabel()
          + (activeTask == null ? ""
              : ". Task: " + activeTask.getTaskId()
                  + " (Staff: " + activeTask.getAssignedStaff() + ")"));
      MessageUI.pressEnterToContinue();
      return;
    }

    // ── Auto-assign the staff member with the fewest active tasks ─────────
    // Example: HK001 has 2 tasks, HK002 has 0 -> HK002 is chosen.
    String staffId = autoAssignStaff();
    housekeepingUI.displayAutoAssign(staffId); // tell the user who was picked

    String taskType = housekeepingUI.inputTaskType(); // what kind of cleaning
    if (taskType == null) {
      MessageUI.displayInfoMessage("Add cleaning task cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }
    taskCounter++;                        // 1000 -> 1001, then 1002, ...
    String taskId = "T" + taskCounter;    // build the new ID: e.g. "T1001"

    // Build the task object and APPEND it to the sequential task log:
    HousekeepingTask task = new HousekeepingTask(
        taskId, roomNumber, staffId, taskType, room.getStatus(), MalaysiaTime.now());
    taskList.add(task); // List ADT - goes to the end (creation order)

    // The room is now "occupied", so move it out of DIRTY.
    // This prevents a second task for the same room.
    RoomStatus previousStatus = room.getStatus(); // remember old status
    if (previousStatus == RoomStatus.DIRTY) {
      recordStatusChange(roomNumber, previousStatus, RoomStatus.CLEANING_IN_PROGRESS,
          "Cleaning task " + taskId + " assigned"); // remember for undo
      room.setStatus(RoomStatus.CLEANING_IN_PROGRESS); // update room
      syncTaskStatus(roomNumber, RoomStatus.CLEANING_IN_PROGRESS); // update task too
    }

    saveData(); // persist all lists/stacks to disk
    housekeepingUI.displayTaskDetails(task); // show the new task
    MessageUI.displaySuccessMessage("Cleaning task added to sequential log.");
    MessageUI.pressEnterToContinue();
  }

  /**
   * Chooses the staff member with the LOWEST number of active tasks.
   * "Active" means their room is not Dirty (it is being cleaned/inspected).
   * Falls back to a default pool HK001-HK005 when there is no history.
   */
  private String autoAssignStaff() {
    // A small table: staffId -> how many active tasks they have.
    java.util.LinkedHashMap<String, Integer> workload = new java.util.LinkedHashMap<>();
    // Add the default staff so everyone shows up even with 0 tasks.
    String[] defaultStaff = {"HK001", "HK002", "HK003", "HK004", "HK005"};
    for (String s : defaultStaff) workload.put(s, 0); // start everyone at 0

    // Count how many ACTIVE tasks every staff member has.
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      HousekeepingTask t = taskList.getEntry(i); // one task
      if (isActiveHousekeepingTask(t)) {
        workload.merge(t.getAssignedStaff(), 1, Integer::sum); // +1 task
      } else if (!workload.containsKey(t.getAssignedStaff())) {
        workload.put(t.getAssignedStaff(), 0); // staff seen, 0 tasks so far
      }
    }

    // Pick the person with the fewest tasks (alphabetical order breaks ties).
    String best = null;              // best staff member so far
    int min = Integer.MAX_VALUE;     // their task count (start huge)
    for (java.util.Map.Entry<String, Integer> e : workload.entrySet()) {
      if (e.getValue() < min || (e.getValue() == min && e.getKey().compareTo(best) < 0)) {
        min  = e.getValue(); // better (lower) count found
        best = e.getKey();   // remember that staff member
      }
    }
    return best != null ? best : "HK001"; // safety net
  }

  /**
   * Builds a display table of every room and whether a task can be assigned.
   * Used ONLY for display - this method changes nothing.
   */
  private String getActiveRoomSummary() {
    StringBuilder sb = new StringBuilder(); // we build the text here
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room r = roomList.getEntry(i); // one room
      HousekeepingTask activeTask = findActiveTaskForRoom(r.getRoomNumber());
      String taskId = activeTask == null ? "-" : activeTask.getTaskId();
      String staff  = activeTask == null ? "-" : activeTask.getAssignedStaff();
      // Decide the availability reason:
      String reason;
      if (activeTask != null) {
        reason = "ACTIVE TASK " + taskId;            // already has a task
      } else if (r.getStatus() == RoomStatus.DIRTY) {
        reason = "AVAILABLE";                         // can assign
      } else {
        reason = "STATUS: " + r.getStatus().getLabel(); // cleaning etc.
      }
      // Add one neat row to the table text:
      sb.append(String.format("%-8s %-8s %-8s %-21s %s\n",
          taskId, r.getRoomNumber(), staff,
          r.getStatus().getLabel(), reason));
    }
    return sb.toString(); // whole table as a String
  }

  /** Option 4 - Search for a task by its ID using a simple LINEAR search. */
  private void searchTaskById() {
    // Show all tasks first so the user knows which IDs exist.
    housekeepingUI.listTaskQueue(getAllTasks());
    String query = housekeepingUI.inputTaskId("Search"); // e.g. T1002
    if (query == null) {
      MessageUI.displayInfoMessage("Search cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }
    HousekeepingTask found = null; // nothing found yet
    // Linear search: check position 1, 2, 3... until a match.
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      HousekeepingTask t = taskList.getEntry(i);
      if (t.getTaskId().equalsIgnoreCase(query)) {
        found = t; // match!
        break;     // stop looking
      }
    }
    if (found == null) {
      MessageUI.displayErrorMessage("No task found with ID: " + query);
    } else {
      housekeepingUI.displayTaskDetails(found, "SEARCH TASK RESULT",
          "Task found using linear search.");
      MessageUI.displaySuccessMessage("Task found.");
    }
    MessageUI.pressEnterToContinue();
  }

  /** Option 5 - Delete a task by its ID, but ONLY after the user confirms. */
  private void deleteTaskById() {
    housekeepingUI.listTaskQueue(getAllTasks()); // show what is available
    String query = housekeepingUI.inputTaskId("Delete"); // ask for the ID
    if (query == null) {
      MessageUI.displayInfoMessage("Delete cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }

    // Linear search for the task to delete:
    int foundIndex = -1;       // position in the list (-1 = not found)
    HousekeepingTask found = null; // the task object itself
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      HousekeepingTask t = taskList.getEntry(i);
      if (t.getTaskId().equalsIgnoreCase(query)) {
        foundIndex = i; // remember position
        found = t;      // remember the object
        break;
      }
    }

    if (found == null) {
      MessageUI.displayErrorMessage("No task found with ID: " + query);
      MessageUI.pressEnterToContinue();
      return;
    }

    // Show what would be deleted, then ask for a YES/NO confirmation.
    housekeepingUI.displayTaskDetails(found, "TASK SELECTED FOR DELETION",
        "Review the task details before confirming deletion.");
    if (!housekeepingUI.confirmDelete("task " + found.getTaskId())) {
      MessageUI.displayInfoMessage("Delete cancelled.");
      MessageUI.pressEnterToContinue();
      return; // user said no
    }

    String roomNumber = found.getRoomNumber(); // the room of the deleted task
    taskList.remove(foundIndex);               // remove from list (ArrayList ADT)

    // If the room now has NO tasks left, put it back to DIRTY so
    // a new task can be created for it later.
    boolean hasOtherTasks = false;
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      if (taskList.getEntry(i).getRoomNumber().equalsIgnoreCase(roomNumber)) {
        hasOtherTasks = true; // another task exists for this room
        break;
      }
    }
    if (!hasOtherTasks) {
      Room room = findRoom(roomNumber);
      if (room != null) {
        room.setStatus(RoomStatus.DIRTY); // back to dirty
      }
    }

    saveData(); // write the change to disk
    MessageUI.displaySuccessMessage(
        "Task " + found.getTaskId() + " deleted from the log.");
    MessageUI.pressEnterToContinue();
  }

  /**
   * Option 3 - Move a room to its NEXT cleaning stage.
   * Flow: DIRTY -> CLEANING_IN_PROGRESS -> INSPECTED -> READY_FOR_CHECK_IN.
   */
  private void advanceRoomStatus() {
    // First show which rooms can be advanced.
    housekeepingUI.displayAdvanceableRooms(getAdvanceableRoomSummary());

    String roomNumber = housekeepingUI.inputRoomNumber();
    if (roomNumber == null) {
      MessageUI.displayInfoMessage("Room status update cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }
    Room room = findRoom(roomNumber);
    if (room == null) {
      MessageUI.displayErrorMessage("Room not found.");
      MessageUI.pressEnterToContinue();
      return; // no such room, stop
    }

    // A room that is already READY cannot move forward anymore.
    if (!room.getStatus().canAdvance()) {
      String extraMessage = room.getStatus() == RoomStatus.OCCUPIED
          || room.getStatus() == RoomStatus.LCO
        ? " Room is occupied and cannot enter the cleaning workflow."
        : " Room is already Ready for Check-In.";
      MessageUI.displayErrorMessage(
        "Room " + roomNumber + " cannot be advanced - status: "
        + room.getStatus().getLabel() + "." + extraMessage);
      MessageUI.pressEnterToContinue();
      return;
    }

    housekeepingUI.displayStatusFlowGuide(); // show the allowed flow
    RoomStatus previousStatus = room.getStatus(); // remember status BEFORE
    RoomStatus newStatus = previousStatus.nextStatus(); // status AFTER

    // Save this change to the undo stack so it can be undone later.
    recordStatusChange(roomNumber, previousStatus, newStatus, "Status advanced by supervisor");
    room.setStatus(newStatus);              // update the Room object
    syncTaskStatus(roomNumber, newStatus); // update the matching task too
    saveData();                            // save to disk

    housekeepingUI.displayRoomDetails(room); // show the result
    MessageUI.displaySuccessMessage("Room status updated successfully.");
    MessageUI.pressEnterToContinue();
  }

  /**
   * Builds a display of the rooms that CAN be advanced
   * (i.e. NOT Dirty and NOT Ready - they are in the middle stages).
   * Display only - nothing is changed here.
   */
  private String getAdvanceableRoomSummary() {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room r = roomList.getEntry(i);
      RoomStatus s = r.getStatus();
      if (s.canAdvance()) {
        String nextLabel = s.nextStatus() != null ? s.nextStatus().getLabel() : "-";
        sb.append(String.format("%-8s %-21s -> %s\n",
            r.getRoomNumber(), s.getLabel(), nextLabel));
      }
    }
    return sb.toString();
  }

  /** Shows which rooms are eligible for the Ready-for-Check-In rollback. */
  private String getLateCheckoutRoomSummary() {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room r = roomList.getEntry(i);
      RoomStatus s = r.getStatus();
      String action = (s == RoomStatus.READY_FOR_CHECK_IN)
          ? "-> Restore Occupied" : "Not eligible";
      // Same column layout as the Room Status Board + an Action column:
      sb.append(String.format("%-8s %-12s %-6d %-22s %s\n",
          r.getRoomNumber(), r.getRoomType(), r.getFloor(),
          s.getLabel(), action));
    }
    return sb.toString();
  }

  /** Option 6 - Undo the latest change, after showing the user what it is. */
  private void undoLastChange() {
    // Nothing to undo? Nothing to do.
    if (undoStack.isEmpty()) {
      MessageUI.displayErrorMessage(
          "No housekeeping changes are currently available to undo.");
      MessageUI.pressEnterToContinue();
      return;
    }

    // Step 1: look at the TOP of the stack (peek = view but do NOT remove).
    StatusChangeRecord latest = undoStack.peek();
    Room room = findRoom(latest.getRoomNumber());
    if (!canApplyStatusRecord(room, latest.getNewStatus(), "undo")) {
      MessageUI.pressEnterToContinue();
      return;
    }
    housekeepingUI.displayUndoLatest(latest);

    // Step 2: ask the user before changing anything.
    if (!housekeepingUI.confirmAction(
        "Are you sure you want to undo this change?")) {
      MessageUI.displayInfoMessage("Undo cancelled. No changes were made.");
      MessageUI.pressEnterToContinue();
      return;
    }

    // Step 3: POP (remove) the top record and reverse the change.
    StatusChangeRecord record = undoStack.pop();

    room.setStatus(record.getPreviousStatus());  // go back to the OLD status
    syncTaskStatus(record.getRoomNumber(), record.getPreviousStatus()); // keep in sync
    saveData(); // save everything

    System.out.println("\nUndone: " + record);
    MessageUI.displaySuccessMessage("Latest change undone successfully.");
    housekeepingUI.displayRoomDetails(room);
    MessageUI.pressEnterToContinue();
  }

  /**
   * Option 10 - roll a completed housekeeping schedule back when the guest
   * requests a late check-out. Only READY_FOR_CHECK_IN rooms are restored
   * to OCCUPIED; Front Desk then records the new checkout date and marks LCO.
   */
  private void handleLateCheckout() {
    // ── Show ALL rooms first so the supervisor picks the right one ─────────
    housekeepingUI.displayLateCheckoutRooms(getLateCheckoutRoomSummary());

    String roomNumber = housekeepingUI.inputRoomNumber(); // e.g. R101
    if (roomNumber == null) {
      MessageUI.displayInfoMessage("Late check-out handling cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }
    Room room = findRoom(roomNumber);
    if (room == null) {
      MessageUI.displayErrorMessage("Room not found.");
      return; // stop - no such room
    }

    RoomStatus previousStatus = room.getStatus();
    if (previousStatus != RoomStatus.READY_FOR_CHECK_IN) {
      MessageUI.displayErrorMessage(
          "Only a room with status Ready for Check-In can be restored to Occupied. "
          + "Current status: " + previousStatus.getLabel() + ".");
      MessageUI.pressEnterToContinue();
      return;
    }

    // Save the rollback so the supervisor can undo it if the request was incorrect.
    recordStatusChange(roomNumber, previousStatus, RoomStatus.OCCUPIED,
        "Late check-out requested - schedule rolled back");
    room.setStatus(RoomStatus.OCCUPIED);
    syncTaskStatus(roomNumber, RoomStatus.OCCUPIED);
    saveData();

    housekeepingUI.displayRoomDetails(room);
    MessageUI.displaySuccessMessage(
        "Schedule rolled back. Room restored to Occupied; continue in Front Desk to set LCO.");
    MessageUI.pressEnterToContinue();
  }

  /** Option 8 - Show the whole change history, newest to oldest. */
  private void displayStatusHistory() {
    List<StatusChangeRecord> history = copyUndoHistory(); // safe copy
    housekeepingUI.displayChangeHistory(history);          // show it
    MessageUI.pressEnterToContinue();
  }

  /** Shows the number of available rollback changes and the latest entry. */
  private void displayStackSummary() {
    housekeepingUI.displayUndoSummary(
        undoStack.getSize(),
        undoStack.isEmpty() ? null : undoStack.peek());
    MessageUI.pressEnterToContinue();
  }

  /** Option 9 - Undo only the NEWEST change for ONE given room. */
  private void rollbackSpecificRoom() {
    String roomNumber = housekeepingUI.inputRoomNumber();
    if (roomNumber == null) {
      MessageUI.displayInfoMessage("Room-specific undo cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }
    Room room = findRoom(roomNumber);
    if (room == null) {
      MessageUI.displayErrorMessage("Room not found. Undo cancelled.");
      MessageUI.pressEnterToContinue();
      return;
    }

    // Search for the newest change of this room, holding other records aside.
    StackInterface<StatusChangeRecord> temporaryStack = new LinkedStack<>();
    StatusChangeRecord target = null; // the one we want to undo
    while (!undoStack.isEmpty()) {
      StatusChangeRecord record = undoStack.pop();
      if (record.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        target = record; // found it
        break;
      }
      temporaryStack.push(record); // not ours - set it aside
    }
    if (target == null) {
      restoreStackRecords(temporaryStack);
      MessageUI.displayErrorMessage(
          "No undoable change was found for Room " + roomNumber + ".");
      MessageUI.pressEnterToContinue();
      return;
    }

    if (!canApplyStatusRecord(room, target.getNewStatus(), "undo")) {
      undoStack.push(target);
      restoreStackRecords(temporaryStack);
      MessageUI.pressEnterToContinue();
      return;
    }

    // Step 1 - show the change, then ask the user.
    housekeepingUI.displayRoomUndo(roomNumber, target);
    if (!housekeepingUI.confirmAction(
        "Are you sure you want to undo the latest change for Room "
            + roomNumber + "?")) {
      undoStack.push(target);
      restoreStackRecords(temporaryStack);
      MessageUI.displayInfoMessage("Undo cancelled. No changes were made.");
      MessageUI.pressEnterToContinue();
      return;
    }

    // Step 2 - apply the undo.
    restoreStackRecords(temporaryStack);
    room.setStatus(target.getPreviousStatus());
    syncTaskStatus(roomNumber, target.getPreviousStatus());
    saveData();
    MessageUI.displaySuccessMessage(
        "Latest status change for " + roomNumber + " was rolled back.");
    housekeepingUI.displayRoomDetails(room);
    MessageUI.pressEnterToContinue();
  }

  /**
   * Makes a safe copy of the undo stack from top (newest) to bottom (oldest).
   * The original stack is left unchanged - we only "peek" through it.
   */
  private List<StatusChangeRecord> copyUndoHistory() {
    List<StatusChangeRecord> history = new java.util.ArrayList<>();
    StackInterface<StatusChangeRecord> temporaryStack = new LinkedStack<>();
    // Pop every record off, add it to the list, and keep it aside.
    while (!undoStack.isEmpty()) {
      StatusChangeRecord record = undoStack.pop();
      history.add(record);
      temporaryStack.push(record); // hold it
    }
    // Put everything back exactly as it was.
    while (!temporaryStack.isEmpty()) {
      undoStack.push(temporaryStack.pop());
    }
    return history;
  }

  // ═══════════════════════════════════════════════════════════════════════
  // REPORT 1 — Housekeeping Operational Summary
  //   Algorithms demonstrated: BINARY SEARCH (date range), BUBBLE SORT
  //   (status priority), MULTI-CRITERIA FILTER (date + status + room type).
  // ═══════════════════════════════════════════════════════════════════════
  private void generateTasksByStatusReport() {
    housekeepingUI.displayReportIntro(
        "REPORT 1: HOUSEKEEPING OPERATIONAL SUMMARY",
        "Combines binary search on sorted tasks and bubble sort by status priority.\n"
        + "  Filters tasks by date range, status, and room type for targeted analysis.");

    // ── Step 1: Get the filter choices from the supervisor ────────────────
    String[] filters = housekeepingUI.inputReport1Filters();
    String fromDateStr  = filters[0];   // "yyyy-MM-dd" or empty
    String toDateStr    = filters[1];   // "yyyy-MM-dd" or empty
    String statusFilter = filters[2];   // RoomStatus name, or "ALL"
    String roomTypeFilter = filters[3]; // "Standard"/"Deluxe"/"Suite"/"ALL"

    // Convert the date strings into LocalDate objects (null = no limit).
    LocalDate fromDate;
    LocalDate toDate;
    try {
      fromDate = fromDateStr.isEmpty() ? null : LocalDate.parse(fromDateStr);
      toDate = toDateStr.isEmpty() ? null : LocalDate.parse(toDateStr);
    } catch (DateTimeParseException ex) {
      MessageUI.displayErrorMessage("Report dates must be valid calendar dates.");
      MessageUI.pressEnterToContinue();
      return;
    }
    if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
      MessageUI.displayErrorMessage("Report end date must not be before the start date.");
      MessageUI.pressEnterToContinue();
      return;
    }

    // ── Step 2: Bubble-sort ALL tasks by date (ascending) ───────────────
    //          Sorting is REQUIRED before we can use binary search.
    int n = taskList.getNumberOfEntries();           // how many tasks
    HousekeepingTask[] sorted = new HousekeepingTask[n];
    // Copy the task list into an array so we can sort it.
    for (int i = 0; i < n; i++) sorted[i] = taskList.getEntry(i + 1);

    // Bubble sort: repeatedly swap neighbours that are out of order.
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < n - i - 1; j++) {
        if (sorted[j].getLoggedAt().isAfter(sorted[j + 1].getLoggedAt())) {
          HousekeepingTask tmp = sorted[j];      // swap
          sorted[j] = sorted[j + 1];
          sorted[j + 1] = tmp;
        }
      }
    }

    // ── Step 3: Binary search for the date-range boundaries ──────────────
    //          This finds the first and last index of the used date range.
    //          Rather than scanning the whole list, we "jump" left/right.
    int lo = 0, hi = n - 1;
    int startIdx = 0, endIdx = n - 1;

    if (fromDate != null) {
      // Find the leftmost task whose date is >= fromDate.
      lo = 0; hi = n - 1; startIdx = n;
      while (lo <= hi) {
        int mid = (lo + hi) / 2; // middle position
        if (!sorted[mid].getLoggedAt().toLocalDate().isBefore(fromDate)) {
          startIdx = mid; hi = mid - 1; // look in the left half next
        } else {
          lo = mid + 1;                 // look in the right half
        }
      }
    }

    if (toDate != null) {
      // Find the rightmost task whose date is <= toDate.
      lo = 0; hi = n - 1; endIdx = -1;
      while (lo <= hi) {
        int mid = (lo + hi) / 2;
        if (!sorted[mid].getLoggedAt().toLocalDate().isAfter(toDate)) {
          endIdx = mid; lo = mid + 1; // look in the right half
        } else {
          hi = mid - 1;               // look in the left half
        }
      }
    }

    // ── Step 4: Multi-criteria filter within that date range ─────────────
    java.util.List<HousekeepingTask> filtered = new java.util.ArrayList<>();
    for (int i = startIdx; i <= endIdx && i < n; i++) {
      HousekeepingTask t = sorted[i]; // one task in range
      boolean passStatus   = statusFilter.equals("ALL")
          || t.getCurrentStatus().name().equals(statusFilter);
      boolean passRoomType = roomTypeFilter.equals("ALL");
      if (!passRoomType) {
        Room room = findRoom(t.getRoomNumber());
        passRoomType = room != null && room.getRoomType().equalsIgnoreCase(roomTypeFilter);
      }
      if (passStatus && passRoomType) filtered.add(t); // both match
    }

    // ── Step 5: Bubble sort the FILTERED tasks by status urgency ─────────
    //          Priority: DIRTY > CLEANING > INSPECTED > READY.
    int fn = filtered.size();
    for (int i = 0; i < fn - 1; i++) {
      for (int j = 0; j < fn - i - 1; j++) {
        if (filtered.get(j).getCurrentStatus().ordinal()
            > filtered.get(j + 1).getCurrentStatus().ordinal()) {
          HousekeepingTask tmp = filtered.get(j); // swap
          filtered.set(j, filtered.get(j + 1));
          filtered.set(j + 1, tmp);
        }
      }
    }

    // ── Step 6: Optional binary search for a specific task or room ───────
    int searchOption = housekeepingUI.inputReport1SearchOption(); // 0/1/2
    String searchResult = ""; // message to append at the end
    if (searchOption == 1) {
      // Search by Task ID - binary search needs the list SORTED by ID.
      String searchId = housekeepingUI.inputSearchTaskId();
      if (searchId == null) {
        MessageUI.displayInfoMessage("Report search cancelled.");
        MessageUI.pressEnterToContinue();
        return;
      }
      HousekeepingTask[] byId = filtered.toArray(new HousekeepingTask[0]);
      // Bubble sort ascending by Task ID:
      for (int i = 0; i < byId.length - 1; i++) {
        for (int j = 0; j < byId.length - i - 1; j++) {
          if (byId[j].getTaskId().compareToIgnoreCase(byId[j + 1].getTaskId()) > 0) {
            HousekeepingTask tmp = byId[j]; // swap
            byId[j] = byId[j + 1];
            byId[j + 1] = tmp;
          }
        }
      }
      // Binary search for the Task ID:
      int lo1 = 0, hi1 = byId.length - 1;
      HousekeepingTask found1 = null;
      while (lo1 <= hi1) {
        int mid = (lo1 + hi1) / 2;
        int cmp = byId[mid].getTaskId().compareToIgnoreCase(searchId);
        if (cmp == 0) { found1 = byId[mid]; break; }       // found
        else if (cmp < 0) lo1 = mid + 1;                    // go right
        else hi1 = mid - 1;                                 // go left
      }
      if (found1 != null) {
        searchResult = "  [BINARY SEARCH] Task " + searchId + " FOUND -> Room: "
            + found1.getRoomNumber() + " | Staff: " + found1.getAssignedStaff()
            + " | Status: " + found1.getCurrentStatus().getLabel();
      } else {
        searchResult = "  [BINARY SEARCH] Task " + searchId + " NOT FOUND in filtered results.";
      }
    } else if (searchOption == 2) {
      // Binary search by Room Number - same idea, sort by room first.
      String searchRoom = housekeepingUI.inputSearchRoomNumber();
      if (searchRoom == null) {
        MessageUI.displayInfoMessage("Report search cancelled.");
        MessageUI.pressEnterToContinue();
        return;
      }
      HousekeepingTask[] byRoom = filtered.toArray(new HousekeepingTask[0]);
      // Bubble sort ascending by Room Number:
      for (int i = 0; i < byRoom.length - 1; i++) {
        for (int j = 0; j < byRoom.length - i - 1; j++) {
          if (byRoom[j].getRoomNumber().compareToIgnoreCase(byRoom[j + 1].getRoomNumber()) > 0) {
            HousekeepingTask tmp = byRoom[j]; // swap
            byRoom[j] = byRoom[j + 1];
            byRoom[j + 1] = tmp;
          }
        }
      }
      // Binary search for the Room Number:
      int lo2 = 0, hi2 = byRoom.length - 1;
      HousekeepingTask found2 = null;
      while (lo2 <= hi2) {
        int mid = (lo2 + hi2) / 2;
        int cmp = byRoom[mid].getRoomNumber().compareToIgnoreCase(searchRoom);
        if (cmp == 0) { found2 = byRoom[mid]; break; }       // found
        else if (cmp < 0) lo2 = mid + 1;                     // go right
        else hi2 = mid - 1;                                  // go left
      }
      if (found2 != null) {
        searchResult = "  [BINARY SEARCH] Room " + searchRoom + " FOUND -> Task: "
            + found2.getTaskId() + " | Staff: " + found2.getAssignedStaff()
            + " | Status: " + found2.getCurrentStatus().getLabel();
      } else {
        searchResult = "  [BINARY SEARCH] Room " + searchRoom + " NOT FOUND in filtered results.";
      }
    }

    // ── Step 7: Build the report text ──────────────────────────────────
    StringBuilder consoleReport = new StringBuilder();
    consoleReport.append("  " + repeatChar('-', 66) + "\n");
    consoleReport.append("  HOUSEKEEPING OPERATIONAL SUMMARY\n");
    consoleReport.append("  " + repeatChar('-', 66) + "\n\n");

    // Report Overview (which filters were used)
    consoleReport.append("  REPORT OVERVIEW\n");
    consoleReport.append("  " + repeatChar('-', 40) + "\n");
    consoleReport.append(String.format("  Date Range      : %s to %s%n",
        fromDateStr.isEmpty() ? "(any)" : fromDateStr,
        toDateStr.isEmpty()   ? "(any)" : toDateStr));
    consoleReport.append(String.format("  Status Filter   : %s%n", statusFilter));
    consoleReport.append(String.format("  Room Type Filter: %s%n", roomTypeFilter));
    consoleReport.append(String.format("  Tasks Matched   : %d%n", filtered.size()));
    consoleReport.append("\n");

    // Key Performance Indicators - count tasks by the status label.
    Map<String,Integer> statusCount  = new LinkedHashMap<>();
    Map<String,Integer> roomTypeCount = new LinkedHashMap<>();
    // Start every status count at 0.
    for (RoomStatus rs : RoomStatus.values()) statusCount.put(rs.getLabel(), 0);
    // For every filtered task, count its status and its room type.
    for (HousekeepingTask t : filtered) {
      Room room = findRoom(t.getRoomNumber());
      String rType = room != null ? room.getRoomType() : "Unknown";
      statusCount.merge(t.getCurrentStatus().getLabel(), 1, Integer::sum);
      roomTypeCount.merge(rType, 1, Integer::sum);
    }

    // Print the status counts as KPI rows.
    consoleReport.append("  KEY PERFORMANCE INDICATORS\n");
    consoleReport.append("  " + repeatChar('-', 40) + "\n");
    for (Map.Entry<String,Integer> e : statusCount.entrySet()) {
      consoleReport.append(String.format("  %-24s %4d%n", e.getKey(), e.getValue()));
    }
    consoleReport.append("\n");

    // Room Type Distribution
    consoleReport.append("  ROOM TYPE DISTRIBUTION\n");
    consoleReport.append("  " + repeatChar('-', 40) + "\n");
    if (roomTypeCount.isEmpty()) {
      consoleReport.append("  (No tasks match the selected criteria)\n");
    } else {
      for (Map.Entry<String,Integer> e : roomTypeCount.entrySet()) {
        consoleReport.append(String.format("  %-24s %4d%n", e.getKey(), e.getValue()));
      }
    }
    consoleReport.append("\n");

    // Detailed Task List (already sorted by status priority in step 5)
    consoleReport.append("  DETAILED TASK LIST (Sorted by Status Priority)\n");
    consoleReport.append("  " + repeatChar('-', 66) + "\n");
    consoleReport.append(String.format("  %-8s %-8s %-10s %-16s %-22s %s%n",
        "Task ID", "Room", "Staff", "Task Type", "Status", "Logged At"));
    consoleReport.append("  " + repeatChar('-', 66) + "\n");
    if (filtered.isEmpty()) {
      consoleReport.append("  (No tasks match the selected filter criteria)\n");
    } else {
      for (HousekeepingTask t : filtered) {
        consoleReport.append(String.format("  %-8s %-8s %-10s %-16s %-22s %s%n",
            t.getTaskId(), t.getRoomNumber(), t.getAssignedStaff(),
            t.getTaskType(), t.getCurrentStatus().getLabel(),
            t.getLoggedAt().format(MalaysiaTime.FORMATTER)));
      }
    }
    consoleReport.append("\n");

    // Append the binary-search result, if one was done.
    if (!searchResult.isEmpty()) {
      consoleReport.append(searchResult + "\n\n");
    }

    // Management insights - quick numbers for the manager
    long dirty    = statusCount.getOrDefault("Dirty", 0);
    long cleaning = statusCount.getOrDefault("Cleaning In Progress", 0);
    long inspected= statusCount.getOrDefault("Inspected", 0);
    long ready    = statusCount.getOrDefault("Ready for Check-In", 0);
    consoleReport.append("  MANAGEMENT INSIGHTS\n");
    consoleReport.append("  " + repeatChar('-', 40) + "\n");
    consoleReport.append(String.format("  Rooms requiring immediate attention : %d (Dirty + Cleaning)%n",
        dirty + cleaning));
    consoleReport.append(String.format("  Rooms ready for guests              : %d (Ready for Check-In)%n",
        ready));
    consoleReport.append(String.format("  Rooms under inspection             : %d%n", inspected));
    consoleReport.append("\n");

    // Show the report on the console screen.
    housekeepingUI.displayReport("REPORT 1: HOUSEKEEPING OPERATIONAL SUMMARY",
        consoleReport.toString());

    // ── Step 8: Offer to export this report to PDF ───────────────────────
    if (housekeepingUI.confirmPdfExport()) {
      exportReport1ToPdf(filtered, statusCount, roomTypeCount,
          fromDateStr, toDateStr, statusFilter, roomTypeFilter);
    }
    MessageUI.pressEnterToContinue();
  }

  /** Exports Report 1 to a professional PDF using PdfReportEngine. */
  private void exportReport1ToPdf(java.util.List<HousekeepingTask> filtered,
      Map<String,Integer> statusCount, Map<String,Integer> roomTypeCount,
      String fromDate, String toDate, String statusFilter, String roomTypeFilter) {
    PdfReportEngine pdf = null; // will manage the PDF
    try {
      // Create the output folder if it does not exist.
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs(); // make the folder if missing
      // Unique timestamp for the filename, e.g. 20260823_173512 (Malaysia time).
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
      String outPath = outDir + File.separator + "housekeeping_summary_" + timestamp + ".pdf";

      pdf = new PdfReportEngine(); // create the PDF engine

      // Cover page with the title and period.
      String period = (fromDate.isEmpty() ? "All dates" : fromDate)
          + " to " + (toDate.isEmpty() ? "All dates" : toDate);
      pdf.addCoverPage(
          "Housekeeping Operational Summary",
          "Tasks by Status | Room Type Distribution | Filtered Analysis",
          period, "Housekeeping Supervisor");

      // Page 1 - the overview + KPI cards + charts.
      pdf.beginContentPage();
      pdf.addSectionHeading("Report Overview");
      pdf.addKpiRow("Report Type",   "Housekeeping Operational Summary", null);
      pdf.addKpiRow("Date Range",    period, null);
      pdf.addKpiRow("Status Filter", statusFilter, null);
      pdf.addKpiRow("Room Type Filter", roomTypeFilter, null);
      pdf.addKpiRow("Tasks Matched", String.valueOf(filtered.size()),
          filtered.isEmpty() ? PdfReportEngine.DANGER : PdfReportEngine.SUCCESS);
      pdf.addDivider();

      // KPI cards (quick colored glance).
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

      // Bar chart: number of tasks per status.
      String[] sLabels = statusCount.keySet().toArray(new String[0]);
      double[] sValues = statusCount.values().stream()
          .mapToDouble(Integer::doubleValue).toArray();
      pdf.addBarChart("Tasks by Status", sLabels, sValues, "Number of Tasks");

      // Donut chart: tasks by room type.
      if (!roomTypeCount.isEmpty()) {
        String[] rtLabels = roomTypeCount.keySet().toArray(new String[0]);
        double[] rtValues = roomTypeCount.values().stream()
            .mapToDouble(Integer::doubleValue).toArray();
        pdf.addSectionHeading("Room Type Distribution");
        pdf.addDonutChart("Tasks by Room Type", rtLabels, rtValues);
      }

      // Page 2 - the detailed table of matching tasks.
      pdf.beginContentPage();
      pdf.addSectionHeading("Detailed Task List (Sorted by Status Priority)");
      pdf.addBodyText(
          "Tasks are sorted using Bubble Sort by status urgency: Dirty > Cleaning > Inspected > Ready.",
          9);
      pdf.addSpace(6);

      String[] headers = {"Task ID","Room","Staff","Task Type","Status","Logged At"};
      float[] colW = {60, 50, 60, 90, 110, 120};
      java.util.List<String[]> rows = new java.util.ArrayList<>();
      // Turn each task into a String[] row for the PDF table (24-hour + AM/PM).
      for (HousekeepingTask t : filtered) {
        rows.add(new String[]{
            t.getTaskId(), t.getRoomNumber(), t.getAssignedStaff(),
            t.getTaskType(), t.getCurrentStatus().getLabel(),
            t.getLoggedAt().format(MalaysiaTime.FORMATTER)
        });
      }
      // If no rows, print a friendly note instead of an empty table.
      if (rows.isEmpty()) {
        pdf.addBodyText("  No tasks match the selected filter criteria.", 10);
      } else {
        pdf.addTable(headers, rows, colW);
      }

      pdf.save(outPath); // save the PDF to disk
      housekeepingUI.displayPdfExportSuccess(outPath); // tell the user where
    } catch (IOException ex) {
      MessageUI.displayErrorMessage("PDF export failed: " + ex.getMessage());
    } finally {
      try {
        if (pdf != null) pdf.close(); // always close the PDF engine
      } catch (IOException ignored) {
        // Cleanup errors are not important - the main error was already shown.
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // REPORT 2 — Staff Workload & Performance Analysis
  //   Algorithms used: LINEAR SEARCH (unique staff), INSERTION SORT
  //   (workload ranking), MULTI-CRITERIA FILTER (prefix + threshold).
  // ═══════════════════════════════════════════════════════════════════════
  private void generateStaffWorkloadReport() {
    housekeepingUI.displayReportIntro(
        "REPORT 2: STAFF WORKLOAD & PERFORMANCE ANALYSIS",
        "Uses insertion sort to rank staff by total workload (descending).\n"
        + "  Filters by staff ID prefix and completion threshold for targeted review.");

    // ── Step 1: Ask for the report filters ───────────────────────────────
    String[] filters = housekeepingUI.inputReport2Filters();
    String staffPrefix       = filters[0]; // e.g. "HK" or empty for everyone
    int    minTasksThreshold = Integer.parseInt(filters[1]); // e.g. 2

    // ── Step 2: Linear search - collect UNIQUE staff IDs matching the prefix ──
    ListInterface<String> staffIds = new ArrayList<>();
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      String staffId = taskList.getEntry(i).getAssignedStaff();
      boolean matchPrefix = staffPrefix.isEmpty()
          || staffId.toUpperCase().startsWith(staffPrefix.toUpperCase());
      // Only add the staff member if they were not seen before.
      if (matchPrefix && !staffIds.contains(staffId)) {
        staffIds.add(staffId);
      }
    }

    // ── Step 3: Insertion sort - rank staff by task count (descending) ───
    int m = staffIds.getNumberOfEntries(); // how many staff we found
    String[] staffArr = new String[m];     // copy to an array to sort
    for (int i = 0; i < m; i++) staffArr[i] = staffIds.getEntry(i + 1);

    // Insertion sort - pick up each "card" and place it in the right spot.
    for (int i = 1; i < m; i++) {
      String key = staffArr[i];               // the current staff card
      int keyTasks = countTasksForStaff(key); // how many tasks they have
      int j = i - 1;
      // Shift staff with fewer tasks one step to the right.
      while (j >= 0 && countTasksForStaff(staffArr[j]) < keyTasks) {
        staffArr[j + 1] = staffArr[j];
        j--;
      }
      staffArr[j + 1] = key; // insert the card in position
    }

    // ── Step 4: Multi-criteria filter - keep only staff meeting threshold ──
    java.util.List<String> qualifiedStaff = new java.util.ArrayList<>();
    for (String s : staffArr) {
      if (countTasksForStaff(s) >= minTasksThreshold) {
        qualifiedStaff.add(s); // this staff member qualifies
      }
    }

    // ── Step 5: Build the report text ────────────────────────────────────
    StringBuilder consoleReport = new StringBuilder();
    consoleReport.append("  " + repeatChar('-', 66) + "\n");
    consoleReport.append("  STAFF WORKLOAD & PERFORMANCE ANALYSIS\n");
    consoleReport.append("  " + repeatChar('-', 66) + "\n\n");

    // Overview
    consoleReport.append("  REPORT OVERVIEW\n");
    consoleReport.append("  " + repeatChar('-', 40) + "\n");
    consoleReport.append(String.format("  Staff Prefix Filter : %s%n",
        staffPrefix.isEmpty() ? "(all)" : staffPrefix));
    consoleReport.append(String.format("  Min Tasks Threshold : %d%n", minTasksThreshold));
    consoleReport.append(String.format("  Staff Evaluated     : %d%n", qualifiedStaff.size()));
    consoleReport.append("\n");

    // Key Performance Indicators (totals) - sum across all qualified staff.
    int totalTasks = 0, totalPending = 0;
    for (String staffId : qualifiedStaff) {
      totalTasks += countTasksForStaff(staffId);
      totalPending += countPendingTasksForStaff(staffId);
    }
    int totalCompleted = totalTasks - totalPending;   // done = total - pending
    int pct = totalTasks > 0 ? (totalCompleted * 100 / totalTasks) : 0;

    consoleReport.append("  KEY PERFORMANCE INDICATORS\n");
    consoleReport.append("  " + repeatChar('-', 40) + "\n");
    consoleReport.append(String.format("  %-24s %4d%n", "Staff Evaluated", qualifiedStaff.size()));
    consoleReport.append(String.format("  %-24s %4d%n", "Total Tasks", totalTasks));
    consoleReport.append(String.format("  %-24s %4d%n", "Pending Tasks", totalPending));
    consoleReport.append(String.format("  %-24s %4d%%%n", "Completion Rate", pct));
    consoleReport.append("\n");

    // Comparison table - each staff member's numbers + a flag.
    consoleReport.append("  STAFF WORKLOAD COMPARISON (Insertion Sort - Highest First)\n");
    consoleReport.append("  " + repeatChar('-', 66) + "\n");
    if (qualifiedStaff.isEmpty()) {
      consoleReport.append("  (No staff matching filter criteria)\n");
    } else {
      for (String staffId : qualifiedStaff) {
        int tasks = countTasksForStaff(staffId);
        int pending = countPendingTasksForStaff(staffId);
        int completed = tasks - pending;
        String flag = tasks > 3 ? "[OVERLOADED]" : tasks > 1 ? "[OPTIMAL]" : "[LIGHT]";
        consoleReport.append(String.format("  %-12s Total: %-3d Pending: %-3d Completed: %-3d %s%n",
            staffId, tasks, pending, completed, flag));
      }
    }
    consoleReport.append("\n");

    // Ranked table - 1, 2, 3... by workload.
    consoleReport.append("  STAFF PERFORMANCE RANKING\n");
    consoleReport.append("  " + repeatChar('-', 66) + "\n");
    consoleReport.append(String.format("  %-5s %-12s %-12s %-8s %-10s %s%n",
        "Rank", "Staff ID", "Total Tasks", "Pending", "Completed", "Status"));
    consoleReport.append("  " + repeatChar('-', 66) + "\n");
    int rank = 1; // start number one
    for (String staffId : qualifiedStaff) {
      int tasks = countTasksForStaff(staffId);
      int pending = countPendingTasksForStaff(staffId);
      int completed = tasks - pending;
      String flag = tasks > 3 ? "OVERLOADED" : tasks > 1 ? "OPTIMAL" : "LIGHT";
      consoleReport.append(String.format("  %-5d %-12s %-12d %-8d %-10d %s%n",
          rank++, staffId, tasks, pending, completed, flag));
    }
    if (qualifiedStaff.isEmpty()) {
      consoleReport.append("  (No staff matching filter criteria)\n");
    }
    consoleReport.append("\n");

    // Management recommendations - based on the counts.
    long overloaded = qualifiedStaff.stream()
        .filter(s -> countTasksForStaff(s) > 3).count(); // too many tasks
    long light = qualifiedStaff.stream()
        .filter(s -> countTasksForStaff(s) == 1).count(); // too few tasks
    consoleReport.append("  MANAGEMENT RECOMMENDATIONS\n");
    consoleReport.append("  " + repeatChar('-', 40) + "\n");
    consoleReport.append(overloaded > 0
        ? "  " + overloaded + " staff member(s) are OVERLOADED. Consider task redistribution.\n"
        : "  All staff are within manageable workload limits.\n");
    consoleReport.append(light > 0
        ? "  " + light + " staff member(s) have LIGHT workloads and may accept additional tasks.\n"
        : "  No staff with light workload detected.\n");
    consoleReport.append(totalPending > 0
        ? "  Action required: " + totalPending + " task(s) remain pending. "
          + "Review priority rooms with Dirty or Cleaning status.\n"
        : "  All tasks are completed. Excellent housekeeping performance!\n");
    consoleReport.append("\n");

    // Show the report on the screen.
    housekeepingUI.displayReport("REPORT 2: STAFF WORKLOAD & PERFORMANCE ANALYSIS",
        consoleReport.toString());

    // ── Step 6: Offer to export to PDF ────────────────────────────────────
    if (housekeepingUI.confirmPdfExport()) {
      exportReport2ToPdf(qualifiedStaff, staffPrefix, minTasksThreshold,
          totalTasks, totalPending);
    }
    MessageUI.pressEnterToContinue();
  }

  /** Exports Report 2 to a PDF. */
  private void exportReport2ToPdf(java.util.List<String> qualifiedStaff,
      String staffPrefix, int minTasks, int totalTasks, int totalPending) {
    PdfReportEngine pdf = null;
    try {
      // Create the output folder and a unique filename.
      String outDir = "output" + File.separator + "pdf";
      new File(outDir).mkdirs();
      String timestamp = MalaysiaTime.now().format(MalaysiaTime.FILE_FORMATTER);
      String outPath = outDir + File.separator + "staff_workload_" + timestamp + ".pdf";

      pdf = new PdfReportEngine();

      // Cover page
      pdf.addCoverPage(
          "Staff Workload & Performance Analysis",
          "Insertion Sort Ranking | Pending vs Completed | Load Status Flags",
          "Current business cycle", "Housekeeping Supervisor");

      // Page 1 - overview + KPI cards.
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

      // KPI cards.
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

      // Horizontal bar chart - total vs pending per staff.
      if (!qualifiedStaff.isEmpty()) {
        String[] labels = qualifiedStaff.toArray(new String[0]);
        double[] totals  = new double[labels.length];
        double[] pending = new double[labels.length];
        for (int i = 0; i < labels.length; i++) {
          totals[i]  = countTasksForStaff(labels[i]);
          pending[i] = countPendingTasksForStaff(labels[i]);
        }
        pdf.addSectionHeading("Staff Workload Comparison (Insertion Sort - Highest First)");
        pdf.addBodyText(
            "Staff ranked by total tasks using Insertion Sort (descending). "
            + "Blue = Total Tasks, Orange = Pending Tasks.", 9);
        pdf.addSpace(4);
        pdf.addHorizontalBarChart("Total vs Pending Tasks per Staff",
            labels, new double[][]{totals, pending},
            new String[]{"Total Tasks", "Pending Tasks"});
      }

      // Page 2 - ranked table.
      pdf.beginContentPage();
      pdf.addSectionHeading("Staff Performance Ranking");
      pdf.addBodyText(
          "Sorted by total workload (descending). Flags: [OVERLOADED] >3 tasks, "
          + "[OPTIMAL] 2-3 tasks, [LIGHT] 1 task.", 9);
      pdf.addSpace(6);

      String[] headers = {"Rank","Staff ID","Total Tasks","Pending","Completed","Status"};
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
      // Empty-state text vs the table:
      if (rows.isEmpty()) {
        pdf.addBodyText("No staff matched the filter criteria.", 10);
      } else {
        pdf.addTable(headers, rows, colW);
      }

      // Recommendations page section.
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
    } finally {
      try {
        if (pdf != null) pdf.close(); // always close
      } catch (IOException ignored) {
        // Not important - main error message already shown
      }
    }
  }

  /** Counts how many tasks a given staff member has (all of them). */
  private int countTasksForStaff(String staffId) {
    int count = 0;
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      if (taskList.getEntry(i).getAssignedStaff().equals(staffId)) {
        count++; // this task belongs to that staff member
      }
    }
    return count;
  }

  /** Counts how many tasks a staff member still has PENDING (not ready). */
  private int countPendingTasksForStaff(String staffId) {
    int count = 0;
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      HousekeepingTask task = taskList.getEntry(i);
      // Task is pending only while cleaning/inspection work is still active.
      if (task.getAssignedStaff().equals(staffId) && isActiveHousekeepingTask(task)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Pushes a status change onto the undo STACK.
   * IMPORTANT: we also CLEAR the redo stack, because after a brand-new
   * change, the old "redo" actions no longer make sense (standard undo/redo).
   */
  private void recordStatusChange(String roomNumber, RoomStatus previous,
      RoomStatus current, String reason) {
    // Wrap the change information in a small object.
    StatusChangeRecord record = new StatusChangeRecord(
        roomNumber, previous, current, reason, MalaysiaTime.now());
    undoStack.push(record);  // push - newest goes on TOP
  }

  /**
   * Ensures an undo/redo record still applies to the room's present state.
   * A room may have been changed by Front Desk or VIP allocation after the
   * record was created, in which case applying an older record would corrupt
   * the shared room status.
   */
  private boolean canApplyStatusRecord(Room room, RoomStatus expectedStatus, String action) {
    if (room == null) {
      MessageUI.displayErrorMessage("Room no longer exists. " + action + " cancelled.");
      return false;
    }
    if (room.getStatus() != expectedStatus) {
      MessageUI.displayErrorMessage("Cannot " + action + " room " + room.getRoomNumber()
          + ": its status changed to " + room.getStatus().getLabel()
          + " after this record was created.");
      return false;
    }
    return true;
  }

  /** Restores temporarily popped history entries without changing their LIFO order. */
  private void restoreStackRecords(StackInterface<StatusChangeRecord> temporaryStack) {
    while (!temporaryStack.isEmpty()) {
      undoStack.push(temporaryStack.pop());
    }
  }

  /**
   * Keeps the task log in sync with the room list.
   * Finds the (newest) task that matches a room and updates its status too.
   */
  private void syncTaskStatus(String roomNumber, RoomStatus status) {
    for (int i = taskList.getNumberOfEntries(); i >= 1; i--) {
      HousekeepingTask task = taskList.getEntry(i);
      if (task.getRoomNumber().equals(roomNumber)) {
        task.setCurrentStatus(status); // update to the same status
        break; // only update the newest matching task
      }
    }
  }

  private HousekeepingTask findActiveTaskForRoom(String roomNumber) {
    for (int i = taskList.getNumberOfEntries(); i >= 1; i--) {
      HousekeepingTask task = taskList.getEntry(i);
      if (task.getRoomNumber().equalsIgnoreCase(roomNumber) && isActiveHousekeepingTask(task)) {
        return task;
      }
    }
    return null;
  }

  private boolean isActiveHousekeepingTask(HousekeepingTask task) {
    RoomStatus status = task.getCurrentStatus();
    return status == RoomStatus.CLEANING_IN_PROGRESS || status == RoomStatus.INSPECTED;
  }

  /** Finds a room by its room number using a simple linear search. */
  private Room findRoom(String roomNumber) {
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      Room room = roomList.getEntry(i);
      if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
        return room; // found it
      }
    }
    return null; // not found
  }

  /** Returns ALL tasks as ready-to-print lines (for the UI table). */
  public String getAllTasks() {
    StringBuilder output = new StringBuilder();
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      output.append(taskList.getEntry(i)).append("\n");
    }
    return output.toString();
  }

  /** Returns ALL rooms as ready-to-print lines (for the UI table). */
  public String getAllRooms() {
    StringBuilder output = new StringBuilder();
    for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
      output.append(roomList.getEntry(i)).append("\n");
    }
    return output.toString();
  }

  /**
   * Creates 5 sample rooms the very first time the module runs,
   * with all four status types so everything can be demonstrated.
   */
  private void seedSampleRooms() {
    roomList.add(new Room("R101", "Standard", 1, RoomStatus.DIRTY));
    roomList.add(new Room("R102", "Standard", 1, RoomStatus.CLEANING_IN_PROGRESS));
    roomList.add(new Room("R201", "Deluxe", 2, RoomStatus.INSPECTED));
    roomList.add(new Room("R301", "Suite", 3, RoomStatus.READY_FOR_CHECK_IN));
    roomList.add(new Room("R302", "Suite", 3, RoomStatus.DIRTY));
    saveData(); // save the sample rooms right away
  }

  /**
   * Loads everything back from the saved text files when the program starts:
   *   - room list        <- rooms.txt
   *   - task log        <- housekeeping_tasks.txt
   *   - undo stack      <- status_history.txt
   *   - redo stack      <- redo_history.txt
   *
   * IMPORTANT BUG FIX: while loading tasks, we remember the HIGHEST task
   * ID number, so new tasks keep counting up (T1004, T1005...) instead of
   * restarting at T1001 and creating duplicates.
   */
  private void loadData() {
    // Load the four collections from disk via the DAO.
    ListInterface<Room> loadedRooms = housekeepingDAO.retrieveRooms();
    ListInterface<HousekeepingTask> loadedTasks = housekeepingDAO.retrieveTasks();
    StackInterface<StatusChangeRecord> loadedHistory = housekeepingDAO.retrieveHistory();

    // Fill the room list with the loaded rooms.
    roomList.clear(); // keep it empty first
    for (int i = 1; i <= loadedRooms.getNumberOfEntries(); i++) {
      roomList.add(loadedRooms.getEntry(i));
    }

    // Fill the task list, and NOTE: recover the highest task-ID number used.
    taskList.clear();
    for (int i = 1; i <= loadedTasks.getNumberOfEntries(); i++) {
      taskList.add(loadedTasks.getEntry(i)); // add the loaded task
      String taskId = loadedTasks.getEntry(i).getTaskId();
      // Read the number that follows "T" (or the old "HK") and remember the max.
      if (taskId.startsWith("T") || taskId.startsWith("HK")) {
        try {
          int id = Integer.parseInt(taskId.substring(1));
          if (id >= taskCounter) {
            taskCounter = id; // keep the highest number seen
          }
        } catch (NumberFormatException ex) {
          // If a saved ID is not a number, just ignore it (no crash).
          MessageUI.displayErrorMessage("Ignoring invalid task ID in saved data: " + taskId);
        }
      }
    }

    // Restore the undo stack, preserving LIFO order.
    // (We pop from the loaded stack and push into a temp, then push back.)
    undoStack.clear();
    StackInterface<StatusChangeRecord> tempStack = new LinkedStack<>();
    while (!loadedHistory.isEmpty()) {
      tempStack.push(loadedHistory.pop()); // set aside
    }
    while (!tempStack.isEmpty()) {
      undoStack.push(tempStack.pop()); // back into undo, reversed correctly
    }

  }

  /** Saves rooms, tasks, and the rollback history to disk. */
  private void saveData() {
    housekeepingDAO.saveRooms(roomList);        // rooms
    housekeepingDAO.saveTasks(taskList);        // tasks
    housekeepingDAO.saveHistory(undoStack);     // undo stack
  }
}
