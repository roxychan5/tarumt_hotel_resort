package dao;

import adt.ArrayList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import entity.HousekeepingTask;
import entity.Room;
import entity.StatusChangeRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Data access for housekeeping module collections.
 *
 * @author Your Name
 */
public class HousekeepingDAO {

  private final String roomFile = "rooms.dat";
  private final String taskFile = "housekeeping_tasks.dat";
  private final String historyFile = "status_history.dat";

  public void saveRooms(ListInterface<Room> roomList) {
    saveObject(roomList, roomFile);
  }

  public ListInterface<Room> retrieveRooms() {
    ListInterface<Room> roomList = retrieveObject(roomFile);
    if (roomList == null) {
      roomList = new ArrayList<>();
    }
    return roomList;
  }

  public void saveTasks(ListInterface<HousekeepingTask> taskList) {
    saveObject(taskList, taskFile);
  }

  public ListInterface<HousekeepingTask> retrieveTasks() {
    ListInterface<HousekeepingTask> taskList = retrieveObject(taskFile);
    if (taskList == null) {
      taskList = new ArrayList<>();
    }
    return taskList;
  }

  public void saveHistory(StackInterface<StatusChangeRecord> historyStack) {
    saveObject(historyStack, historyFile);
  }

  public StackInterface<StatusChangeRecord> retrieveHistory() {
    StackInterface<StatusChangeRecord> historyStack = retrieveObject(historyFile);
    if (historyStack == null) {
      historyStack = new LinkedStack<>();
    }
    return historyStack;
  }

  private void saveObject(Object data, String fileName) {
    File file = new File(fileName);
    try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file))) {
      ooStream.writeObject(data);
    } catch (FileNotFoundException ex) {
      System.out.println("\nFile not found: " + fileName);
    } catch (IOException ex) {
      System.out.println("\nCannot save to file: " + fileName);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T retrieveObject(String fileName) {
    File file = new File(fileName);
    if (!file.exists()) {
      return null;
    }
    try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
      return (T) oiStream.readObject();
    } catch (FileNotFoundException ex) {
      System.out.println("\nNo such file: " + fileName);
    } catch (IOException ex) {
      System.out.println("\nCannot read from file: " + fileName);
    } catch (ClassNotFoundException ex) {
      System.out.println("\nClass not found while reading: " + fileName);
    }
    return null;
  }
}
