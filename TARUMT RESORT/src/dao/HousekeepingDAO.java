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
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Text-file persistence for the Housekeeping and Task Log module. */
public class HousekeepingDAO {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final Path DATA_DIRECTORY = Paths.get("data");
  private static final Path ROOM_FILE = DATA_DIRECTORY.resolve("rooms.txt");
  private static final Path TASK_FILE = DATA_DIRECTORY.resolve("housekeeping_tasks.txt");
  private static final Path HISTORY_FILE = DATA_DIRECTORY.resolve("status_history.txt");

  public void saveRooms(ListInterface<Room> rooms) {
    try (BufferedWriter writer = openWriter(ROOM_FILE)) {
      writer.write("roomNumber\troomType\tfloor\tstatus");
      writer.newLine();
      for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
        Room room = rooms.getEntry(i);
        writer.write(room.getRoomNumber() + "\t" + clean(room.getRoomType()) + "\t"
            + room.getFloor() + "\t" + room.getStatus().name());
        writer.newLine();
      }
    } catch (IOException ex) {
      displaySaveError(ROOM_FILE, ex);
    }
  }

  public ListInterface<Room> retrieveRooms() {
    ListInterface<Room> rooms = new ArrayList<>();
    if (!Files.exists(ROOM_FILE)) return rooms;
    try (BufferedReader reader = Files.newBufferedReader(ROOM_FILE, StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = line.split("\\t", -1);
        if (fields.length == 4) {
          rooms.add(new Room(fields[0], fields[1], Integer.parseInt(fields[2]), RoomStatus.valueOf(fields[3])));
        }
      }
    } catch (IOException | IllegalArgumentException ex) {
      displayReadError(ROOM_FILE, ex);
      rooms.clear();
    }
    return rooms;
  }

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

  public ListInterface<HousekeepingTask> retrieveTasks() {
    ListInterface<HousekeepingTask> tasks = new ArrayList<>();
    if (!Files.exists(TASK_FILE)) return tasks;
    try (BufferedReader reader = Files.newBufferedReader(TASK_FILE, StandardCharsets.UTF_8)) {
      reader.readLine();
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

  public void saveHistory(StackInterface<StatusChangeRecord> history) {
    ListInterface<StatusChangeRecord> records = new ArrayList<>();
    StackInterface<StatusChangeRecord> restore = new LinkedStack<>();
    while (!history.isEmpty()) {
      StatusChangeRecord record = history.pop();
      records.add(record);
      restore.push(record);
    }
    while (!restore.isEmpty()) history.push(restore.pop());

    try (BufferedWriter writer = openWriter(HISTORY_FILE)) {
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
      displaySaveError(HISTORY_FILE, ex);
    }
  }

  public StackInterface<StatusChangeRecord> retrieveHistory() {
    ListInterface<StatusChangeRecord> records = new ArrayList<>();
    StackInterface<StatusChangeRecord> history = new LinkedStack<>();
    if (!Files.exists(HISTORY_FILE)) return history;
    try (BufferedReader reader = Files.newBufferedReader(HISTORY_FILE, StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = line.split("\\t", -1);
        if (fields.length == 5) {
          records.add(new StatusChangeRecord(fields[0], RoomStatus.valueOf(fields[1]),
              RoomStatus.valueOf(fields[2]), fields[3], LocalDateTime.parse(fields[4], DATE_FORMAT)));
        }
      }
      for (int i = records.getNumberOfEntries(); i >= 1; i--) history.push(records.getEntry(i));
    } catch (IOException | IllegalArgumentException ex) {
      displayReadError(HISTORY_FILE, ex);
      history.clear();
    }
    return history;
  }

  private BufferedWriter openWriter(Path file) throws IOException {
    Files.createDirectories(DATA_DIRECTORY);
    return Files.newBufferedWriter(file, StandardCharsets.UTF_8);
  }

  private String clean(String value) {
    return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
  }

  private void displaySaveError(Path file, Exception ex) {
    System.out.println("\n!! Could not save " + file + ": " + ex.getMessage());
  }

  private void displayReadError(Path file, Exception ex) {
    System.out.println("\n!! Could not read " + file + ". Check the text-file format: " + ex.getMessage());
  }
}
