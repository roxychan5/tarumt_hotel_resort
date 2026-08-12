package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.GuestRecord;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Stores front-desk guest records as a readable tab-separated text file. */
public class FrontDeskDAO {

  private static final Path DATA_DIRECTORY = Paths.get("data");
  private static final Path GUEST_FILE = DATA_DIRECTORY.resolve("guest_records.txt");

  public void saveGuestRecords(ListInterface<GuestRecord> guestRecords) {
    try {
      Files.createDirectories(DATA_DIRECTORY);
      try (BufferedWriter writer = Files.newBufferedWriter(GUEST_FILE, StandardCharsets.UTF_8)) {
        writer.write("confirmationNumber\tguestName\tidentificationNumber\tcontactNumber\troomNumber\troomType\tcheckInDate\tcheckOutDate\tnumberOfNights\tnightlyRate\tpaidAmount");
        writer.newLine();
        for (int i = 1; i <= guestRecords.getNumberOfEntries(); i++) {
          GuestRecord guest = guestRecords.getEntry(i);
          writer.write(guest.getConfirmationNumber() + "\t" + clean(guest.getGuestName()) + "\t"
              + clean(guest.getIdentificationNumber()) + "\t" + clean(guest.getContactNumber()) + "\t"
              + guest.getRoomNumber() + "\t" + clean(guest.getRoomType()) + "\t"
              + clean(guest.getCheckInDate()) + "\t" + clean(guest.getCheckOutDate()) + "\t"
              + guest.getNumberOfNights() + "\t" + guest.getNightlyRate() + "\t" + guest.getPaidAmount());
          writer.newLine();
        }
      }
    } catch (IOException ex) {
      System.out.println("\n[ERROR] Cannot save guest records to " + GUEST_FILE + ": " + ex.getMessage());
    }
  }

  public ListInterface<GuestRecord> retrieveGuestRecords() {
    ListInterface<GuestRecord> guestRecords = new ArrayList<>();
    if (!Files.exists(GUEST_FILE)) return guestRecords;
    try (BufferedReader reader = Files.newBufferedReader(GUEST_FILE, StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        String[] field = line.split("\\t", -1);
        if (field.length == 11) {
          guestRecords.add(new GuestRecord(field[0], field[1], field[2], field[3], field[4], field[5],
              field[6], field[7], Integer.parseInt(field[8]), Double.parseDouble(field[9]),
              Double.parseDouble(field[10])));
        }
      }
    } catch (IOException | IllegalArgumentException ex) {
      System.out.println("\n[ERROR] Cannot read guest records from " + GUEST_FILE + ": " + ex.getMessage());
      guestRecords.clear();
    }
    return guestRecords;
  }

  private String clean(String value) {
    return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
  }
}
