package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.LoyaltyTier;
import entity.RewardsMember;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import utility.DataFiles;

/** Reads shared member records for front-desk lookup. */
public class FrontDeskDAO {

  private static final Path LOYALTY_MEMBER_FILE = DataFiles.resolve("loyalty_members.txt");

  public ListInterface<RewardsMember> retrieveMemberRecords() {
    ListInterface<RewardsMember> memberRecords = new ArrayList<>();
    if (!Files.exists(LOYALTY_MEMBER_FILE)) return memberRecords;
    try (BufferedReader reader = Files.newBufferedReader(LOYALTY_MEMBER_FILE, StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) continue;
        String[] fields = line.split("\\t", -1);
        if (fields.length == 6) {
          memberRecords.add(new RewardsMember(fields[0], fields[1], fields[2],
              LoyaltyTier.valueOf(fields[3]), Integer.parseInt(fields[4]),
              LocalDate.parse(fields[5])));
        }
      }
    } catch (IOException | IllegalArgumentException ex) {
      System.out.println("\n[ERROR] Cannot read loyalty members from "
          + LOYALTY_MEMBER_FILE + ": " + ex.getMessage());
      memberRecords.clear();
    }
    return memberRecords;
  }
}
