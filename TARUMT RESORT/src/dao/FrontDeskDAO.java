package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.LoyaltyTier;
import entity.RewardsMember;
import entity.VipCheckInRecord;
import entity.VipPaymentRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import utility.CsvUtils;
import utility.DataFiles;

/** Reads shared member records for front-desk lookup. */
public class FrontDeskDAO {

  private static final Path LOYALTY_MEMBER_FILE = DataFiles.resolve("loyalty_members.txt");
  private static final Path VIP_PAYMENT_HISTORY_FILE = DataFiles.resolve("vip_payment_history.txt");
  private static final Path VIP_CHECKIN_HISTORY_FILE = DataFiles.resolve("vip_checkin_history.txt");

  public ListInterface<RewardsMember> retrieveMemberRecords() {
    ListInterface<RewardsMember> memberRecords = new ArrayList<>();
    if (!Files.exists(LOYALTY_MEMBER_FILE)) return memberRecords;
    try (BufferedReader reader = Files.newBufferedReader(LOYALTY_MEMBER_FILE, StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) continue;
        String[] fields = CsvUtils.parse(line);
        if (fields.length == 6) {
          memberRecords.add(new RewardsMember(fields[0], fields[1], fields[2],
              LoyaltyTier.valueOf(fields[3]), Integer.parseInt(fields[4]),
              parseExpiryDate(fields[5])));
        }
      }
    } catch (IOException | IllegalArgumentException ex) {
      System.out.println("\n[ERROR] Cannot read loyalty members from "
          + LOYALTY_MEMBER_FILE + ": " + ex.getMessage());
      memberRecords.clear();
    }
    return memberRecords;
  }

  public ListInterface<VipPaymentRecord> retrieveVipPaymentRecords() {
    ListInterface<VipPaymentRecord> paymentRecords = new ArrayList<>();
    if (!Files.exists(VIP_PAYMENT_HISTORY_FILE)) return paymentRecords;
    try (BufferedReader reader = Files.newBufferedReader(VIP_PAYMENT_HISTORY_FILE,
        StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) continue;
        String[] fields = CsvUtils.parse(line);
        if (fields.length < 12) continue;
        try {
          paymentRecords.add(new VipPaymentRecord(fields[0], fields[1], fields[2],
              fields[3], fields[4], LocalDate.parse(fields[5]), LocalDate.parse(fields[6]),
              Integer.parseInt(fields[7]), Double.parseDouble(fields[8]),
              Double.parseDouble(fields[9]), fields[10], LocalDateTime.parse(fields[11])));
        } catch (IllegalArgumentException ex) {
          // Skip malformed history rows so one old record does not block front-desk lookup.
        }
      }
    } catch (IOException ex) {
      System.out.println("\n[ERROR] Cannot read VIP payment history from "
          + VIP_PAYMENT_HISTORY_FILE + ": " + ex.getMessage());
      paymentRecords.clear();
    }
    return paymentRecords;
  }

  public ListInterface<VipCheckInRecord> retrieveVipCheckInRecords() {
    ListInterface<VipCheckInRecord> checkInRecords = new ArrayList<>();
    if (!Files.exists(VIP_CHECKIN_HISTORY_FILE)) return checkInRecords;
    try (BufferedReader reader = Files.newBufferedReader(VIP_CHECKIN_HISTORY_FILE,
        StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) continue;
        String[] fields = CsvUtils.parse(line);
        if (fields.length < 12) continue;
        try {
          checkInRecords.add(new VipCheckInRecord(fields[0], fields[1], fields[2],
              fields[3], fields[4], Integer.parseInt(fields[5]),
              Integer.parseInt(fields[6]), LocalDate.parse(fields[7]),
              LocalDate.parse(fields[8]), fields[9], LocalDateTime.parse(fields[10]),
              LocalDateTime.parse(fields[11])));
        } catch (IllegalArgumentException ex) {
          // Skip malformed history rows so one old record does not block front-desk lookup.
        }
      }
    } catch (IOException ex) {
      System.out.println("\n[ERROR] Cannot read VIP check-in history from "
          + VIP_CHECKIN_HISTORY_FILE + ": " + ex.getMessage());
      checkInRecords.clear();
    }
    return checkInRecords;
  }

  /** Supports loyalty records whose optional expiry date has not been set. */
  private LocalDate parseExpiryDate(String value) {
    if (value == null || value.trim().isEmpty()
        || value.trim().equalsIgnoreCase("null")) {
      return null;
    }
    return LocalDate.parse(value.trim());
  }
}
