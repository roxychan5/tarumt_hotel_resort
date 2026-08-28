package dao;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomStatus;
import entity.StatusChangeRecord;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import utility.DataFiles;

/**
 * DAO = "Data Access Object".
 *
 * This class is the "saving & loading department" for the housekeeping module.
 * It stores all housekeeping data as simple TAB-separated text files, so the
 * data survives even after the program is closed.
 *
 * Files used (all inside the "data" folder):
 *   - rooms.txt                -> the room list (ArrayList ADT)
 *   - housekeeping_tasks.txt  -> the task log (ArrayList ADT)
 *   - status_history.txt      -> the undo stack (Stack ADT)
 *   - redo_history.txt        -> the redo stack (Stack ADT)
 *
 * Each method either SAVES a collection to disk or LOADS it back from disk.
 *
 * @author Chan Rou Xuan
 */
public class HousekeepingDAO {

  /** How dates/times are written in the files, e.g. "2026-08-18T01:08:13". */
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  // Where everything is stored:
  private static final Path DATA_DIRECTORY = DataFiles.directory();
  private static final Path ROOM_FILE = DataFiles.resolve("rooms.txt");
  private static final Path TASK_FILE = DataFiles.resolve("housekeeping_tasks.txt");
  private static final Path HISTORY_FILE = DataFiles.resolve("status_history.txt");
  private static final Path REDO_FILE = DataFiles.resolve("redo_history.txt");

  // ═══════════════════════════════════════════════════════════════
  // ROOMS
  // ═══════════════════════════════════════════════════════════════

  /**
   * SAVE all rooms to rooms.txt.
   * Writes one line per room: roomNumber, roomType, floor, status,
   * separated by TAB characters.
   */
  public void saveRooms(ListInterface<Room> rooms) {
    try (BufferedWriter writer = openWriter(ROOM_FILE)) {
      writer.write("roomNumber\troomType\tfloor\tstatus\tcheckInAt\texpectedCheckoutAt\tmemberId"); // header line
      writer.newLine();
      // Write every room in the list, one per line:
      for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
        Room room = rooms.getEntry(i);
        writer.write(room.getRoomNumber() + "\t" + clean(room.getRoomType()) + "\t"
            + room.getFloor() + "\t" + room.getStatus().name() + "\t"
            + formatOptionalDateTime(room.getCheckInAt()) + "\t"
            + formatOptionalDateTime(room.getExpectedCheckoutAt()) + "\t"
            + formatOptionalText(room.getOccupantMemberId()));
        writer.newLine();
      }
    } catch (IOException ex) {
      displaySaveError(ROOM_FILE, ex); // tell the user saving failed
    }
  }

  /**
   * LOAD all rooms back from rooms.txt.
   * Reads each line and rebuilds a Room object from the 4 fields.
   * If the file does not exist yet, returns an empty list.
   */
  public ListInterface<Room> retrieveRooms() {
    ListInterface<Room> rooms = new ArrayList<>();
    if (!Files.exists(ROOM_FILE)) return rooms; // nothing saved yet
    try (BufferedReader reader = Files.newBufferedReader(ROOM_FILE, StandardCharsets.UTF_8)) {
      reader.readLine(); // skip the header line
      String line;
      while ((line = reader.readLine()) != null) { // read every data line
        String[] fields = line.split("\\t", -1);  // split on TAB
        if (fields.length >= 4) {
          rooms.add(new Room(fields[0], fields[1], Integer.parseInt(fields[2]),
              RoomStatus.valueOf(fields[3]), parseOptionalDateTime(fields, 4),
              parseOptionalDateTime(fields, 5), parseOptionalText(fields, 6)));
        }
      }
    } catch (IOException | IllegalArgumentException ex) {
      displayReadError(ROOM_FILE, ex); // file was corrupt - warn the user
      rooms.clear();
    }
    return rooms;
  }

  // ═══════════════════════════════════════════════════════════════
  // TASKS
  // ═══════════════════════════════════════════════════════════════

  /**
   * SAVE all cleaning tasks to housekeeping_tasks.txt.
   * One line per task: taskId, roomNumber, staff, taskType, status, loggedAt.
   */
  public void saveTasks(ListInterface<HousekeepingTask> tasks) {
    try (BufferedWriter writer = openWriter(TASK_FILE)) {
      writer.write("taskId\troomNumber\tassignedStaff\ttaskType\tstatus\tloggedAt");
      writer.newLine();
      for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
        HousekeepingTask task = tasks.getEntry(i);
        writer.write(task.getTaskId() + "\t" + task.getRoomNumber() + "\t" + task.getAssignedStaff()
            + "\t" + task.getTaskType() + "\t" + task.getCurrentStatus().name() + "\t"
            + DATE_FORMAT.format(task.getLoggedAt()));
        writer.newLine();
      }
    } catch (IOException ex) {
      displaySaveError(TASK_FILE, ex);
    }
  }

  /**
   * LOAD all cleaning tasks back from housekeeping_tasks.txt.
   * Rebuilds each HousekeepingTask from its saved line.
   */
  public ListInterface<HousekeepingTask> retrieveTasks() {
    ListInterface<HousekeepingTask> tasks = new ArrayList<>();
    if (!Files.exists(TASK_FILE)) return tasks;
    try (BufferedReader reader = Files.newBufferedReader(TASK_FILE, StandardCharsets.UTF_8)) {
      reader.readLine(); // skip header
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = line.split("\\t", -1);
        if (fields.length == 6) {
          tasks.add(new HousekeepingTask(fields[0], fields[1], fields[2], fields[3],
              RoomStatus.valueOf(fields[4]), LocalDateTime.parse(fields[5], DATE_FORMAT)));
        }
      }
    } catch (IOException | IllegalArgumentException ex) {
      displayReadError(TASK_FILE, ex);
      tasks.clear();
    }
    return tasks;
  }

  // ═══════════════════════════════════════════════════════════════
  // HISTORY (undo / redo stacks)
  // ═══════════════════════════════════════════════════════════════

  /** SAVE the undo stack to status_history.txt. */
  public void saveHistory(StackInterface<StatusChangeRecord> history) {
    saveStack(history, HISTORY_FILE);
  }

  /** SAVE the redo stack to redo_history.txt. */
  public void saveRedoHistory(StackInterface<StatusChangeRecord> history) {
    saveStack(history, REDO_FILE);
  }

  /**
   * The shared saving logic for BOTH the undo and redo stacks:
   * peek at every record (without destroying the stack),
   * then write them to the given file, one per line.
   */
  private void saveStack(StackInterface<StatusChangeRecord> history, Path file) {
    // Copy the stack into a list WITHOUT losing its contents:
    ListInterface<StatusChangeRecord> records = new ArrayList<>();
    StackInterface<StatusChangeRecord> restore = new LinkedStack<>();
    while (!history.isEmpty()) {
      StatusChangeRecord record = history.pop();
      records.add(record);
      restore.push(record); // remember it so we can put it back
    }
    while (!restore.isEmpty()) history.push(restore.pop()); // restore the stack

    try (BufferedWriter writer = openWriter(file)) {
      writer.write("roomNumber\tpreviousStatus\tnewStatus\treason\tchangedAt");
      writer.newLine();
      for (int i = 1; i <= records.getNumberOfEntries(); i++) {
        StatusChangeRecord record = records.getEntry(i);
        writer.write(record.getRoomNumber() + "\t" + record.getPreviousStatus().name() + "\t"
            + record.getNewStatus().name() + "\t" + clean(record.getReason()) + "\t"
            + DATE_FORMAT.format(record.getChangedAt()));
        writer.newLine();
      }
    } catch (IOException ex) {
      displaySaveError(file, ex);
    }
  }

  /** LOAD the undo stack from status_history.txt. */
  public StackInterface<StatusChangeRecord> retrieveHistory() {
    return retrieveStack(HISTORY_FILE);
  }

  /** LOAD the redo stack from redo_history.txt, keeping LIFO order. */
  public StackInterface<StatusChangeRecord> retrieveRedoHistory() {
    return retrieveStack(REDO_FILE);
  }

  /**
   * The shared logic for reading either history stack back from a file.
   * Records are read top-to-bottom, then re-pushed so LIFO order is correct.
   */
  private StackInterface<StatusChangeRecord> retrieveStack(Path file) {
    ListInterface<StatusChangeRecord> records = new ArrayList<>();
    StackInterface<StatusChangeRecord> history = new LinkedStack<>();
    if (!Files.exists(file)) return history;
    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      reader.readLine(); // skip header
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = line.split("\\t", -1);
        if (fields.length == 5) {
          records.add(new StatusChangeRecord(fields[0], RoomStatus.valueOf(fields[1]),
              RoomStatus.valueOf(fields[2]), fields[3], LocalDateTime.parse(fields[4], DATE_FORMAT)));
        }
      }
      // Push in reverse so the MOST RECENT change ends up on top (LIFO):
      for (int i = records.getNumberOfEntries(); i >= 1; i--) history.push(records.getEntry(i));
    } catch (IOException | IllegalArgumentException ex) {
      displayReadError(file, ex);
      history.clear();
    }
    return history;
  }

  // ═══════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════

  /** Opens a file for writing, creating the data folder if needed. */
  private BufferedWriter openWriter(Path file) throws IOException {
    Files.createDirectories(DATA_DIRECTORY);
    return Files.newBufferedWriter(file, StandardCharsets.UTF_8);
  }

  /** Removes TAB / newline characters so a single field stays on one line. */
  private String clean(String value) {
    return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
  }

  private String formatOptionalDateTime(LocalDateTime value) {
    return value == null ? "-" : DATE_FORMAT.format(value);
  }

  private LocalDateTime parseOptionalDateTime(String[] fields, int index) {
    if (fields.length <= index || fields[index].trim().isEmpty()
        || fields[index].trim().equals("-")) {
      return null;
    }
    return LocalDateTime.parse(fields[index], DATE_FORMAT);
  }

  private String formatOptionalText(String value) {
    return value == null || value.trim().isEmpty() ? "-" : clean(value);
  }

  private String parseOptionalText(String[] fields, int index) {
    if (fields.length <= index || fields[index].trim().isEmpty()
        || fields[index].trim().equals("-")) {
      return null;
    }
    return fields[index].trim();
  }

  /** Friendly error message when saving fails. */
  private void displaySaveError(Path file, Exception ex) {
    System.out.println("\n!! Could not save " + file + ": " + ex.getMessage());
  }

  /** Friendly error message when loading fails. */
  private void displayReadError(Path file, Exception ex) {
    System.out.println("\n!! Could not read " + file + ". Check the text-file format: " + ex.getMessage());
  }
}
