package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.GuestRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FrontDeskDAO {

  private final String guestFile = "guest_records.dat";

  public void saveGuestRecords(ListInterface<GuestRecord> guestRecords) {
    File file = new File(guestFile);
    try (ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file))) {
      ooStream.writeObject(guestRecords);
    } catch (FileNotFoundException ex) {
      System.out.println("\nFile not found: " + guestFile);
    } catch (IOException ex) {
      System.out.println("\nCannot save to file: " + guestFile);
    }
  }

  @SuppressWarnings("unchecked")
  public ListInterface<GuestRecord> retrieveGuestRecords() {
    File file = new File(guestFile);
    if (!file.exists()) {
      return new ArrayList<>();
    }

    try (ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file))) {
      return (ListInterface<GuestRecord>) oiStream.readObject();
    } catch (FileNotFoundException ex) {
      System.out.println("\nNo such file: " + guestFile);
    } catch (IOException ex) {
      System.out.println("\nCannot read from file: " + guestFile);
    } catch (ClassNotFoundException ex) {
      System.out.println("\nClass not found while reading: " + guestFile);
    }
    return new ArrayList<>();
  }
}
