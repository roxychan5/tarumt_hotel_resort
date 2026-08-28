package boundary;

import entity.RewardsMember;
import utility.ConsoleUI;

/**
 * Boundary class for the Front-Desk Service module.
 *
 * @author
 */
public class FrontDeskServiceUI {

  private static final int BOX_W = 78;
  private static final int LABEL_W = 30;
  private static final char HL = '-';
  private static final char VL = '|';

  private static final String R = ConsoleUI.RESET;
  private static final String B = ConsoleUI.BOLD;
  private static final String C = ConsoleUI.CYAN;
  private static final String IB = ConsoleUI.ICE_BLUE;
  private static final String SB = ConsoleUI.SKY_BLUE;
  private static final String DM = ConsoleUI.DIM;
  private static final String WH = ConsoleUI.WHITE;
  private static final String RD = ConsoleUI.RED;

  public int getMenuChoice() {
    ConsoleUI.clearScreen();
    printMenu();
    return ConsoleUI.readMenuChoice(
        "  " + SB + B + "  Select option (0-10) > " + R + " ");
  }

  private void printMenu() {
    System.out.println();
    printBorder();
    printTitle("FRONT-DESK  SERVICE", "Module : Member Search & Room Status Management");
    printBorder();

    printSectionLabel("MEMBER SEARCH  &  ROOM STATUS");
    printEntry(" 1", "Search Member", "Find member by name or loyalty member ID");
    printEntry(" 2", "Check Room Availability", "Review occupancy and housekeeping status");
    printEntry(" 3", "Check-Out Room", "Release occupied room to housekeeping");
    printEntry(" 4", "Handle Late Checkout", "Extend expected check-out for an occupied room");
    printBorder();

    printSectionLabel("ACCOUNT  &  RECORDS");
    printEntry(" 5", "View Member Account", "Show tier, points, expiry and promotion");
    printEntry(" 6", "List All Member Records", "Display records in member ID order");
    printEntry(" 7", "Check-Out History", "Review recent room check-out records");
    printEntry(" 8", "Late Check-Out History", "Review late check-out extension records");
    printBorder();

    printSectionLabel("MANAGEMENT  REPORTS");
    printEntryHighlight(" 9", "Report 1: Member Accounts", "BST traversal | loyalty KPI | PDF");
    printEntryHighlight("10", "Report 2: Room Availability", "LCO | today | overdue | PDF");
    printBorder();

    printBack(" 0", "Back to Main Menu");
    printBorder();
    System.out.println();
  }

  public String inputMemberId() {
    System.out.print("  " + SB + "Member ID" + R + " (0 to cancel) > ");
    return ConsoleUI.readLine().trim().toUpperCase();
  }

  public String inputMemberSearchKey() {
    System.out.print("  " + SB + "Member name / ID" + R
        + " (e.g. Andy or LM001, 0 to cancel) > ");
    return ConsoleUI.readLine().trim();
  }

  public String inputRoomNumber() {
    System.out.print("  " + SB + "Room No." + R + " (e.g. R101, 0 to cancel) > ");
    return ConsoleUI.readLine().trim().toUpperCase();
  }

  public String inputCheckoutHistoryDate(String label) {
    System.out.print("  " + SB + label + R
        + " (yyyy-MM-dd, blank for all, 0 to cancel) > ");
    return ConsoleUI.readLine().trim();
  }

  public String inputNewExpectedCheckoutDate() {
    System.out.print("  " + SB + "New expected check-out date" + R
        + " (yyyy-MM-dd, 0 to cancel) > ");
    return ConsoleUI.readLine().trim();
  }

  public String inputNewExpectedCheckoutTime(String defaultTime) {
    System.out.print("  " + SB + "New expected check-out time" + R
        + " (HH:mm, blank for " + defaultTime + ", 0 to cancel) > ");
    return ConsoleUI.readLine().trim();
  }

  public void displaySearchResult(String result) {
    sectionHeader("MEMBER SEARCH RESULT", "Binary Search Tree lookup by loyalty member ID.");
    for (String line : result.split("\r?\n")) {
      System.out.println(RD + line + R);
    }
  }

  public void displayMemberDetails(RewardsMember memberRecord) {
    sectionHeader("COMPLETE MEMBER INFORMATION", "Linked loyalty member record.");
    System.out.println(toMemberDetailsString(memberRecord));
  }

  public void displayMemberSearchResults(String output) {
    sectionHeader("MEMBER SEARCH RESULTS", "Matches by loyalty member ID or guest name.");
    System.out.println(output);
  }

  public void displayMemberAccountDetails(RewardsMember memberRecord) {
    sectionHeader("MEMBER ACCOUNT DETAILS", "Front desk check-only account summary.");
    System.out.println(toMemberAccountString(memberRecord));
  }

  public void displayRoomAvailability(String output) {
    sectionHeader("ROOM AVAILABILITY", "Front-desk room check against housekeeping status.");
    for (String line : output.split("\r?\n")) {
      System.out.println(line + R);
    }
  }

  public void displayCheckoutResult(String output) {
    sectionHeader("ROOM CHECK-OUT", "Occupied room release to housekeeping.");
    for (String line : output.split("\r?\n")) {
      System.out.println(line + R);
    }
  }

  public void displayLateCheckoutResult(String output) {
    sectionHeader("HANDLE LATE CHECKOUT", "Extend an occupied guest stay by member ID.");
    for (String line : output.split("\r?\n")) {
      System.out.println(line + R);
    }
  }

  public void displayLateCheckoutSummary(String output) {
    System.out.println();
    System.out.println("  --- LATE CHECK-OUT UPDATED SUCCESSFULLY ---");
    for (String line : output.split("\r?\n")) {
      System.out.println("  " + line);
    }
    System.out.println("  -------------------------------------------");
  }

  public void displayCheckoutAvailability(String output) {
    sectionHeader("ROOM AVAILABILITY FOR CHECK-OUT",
        "Current room board from housekeeping records.");
    System.out.println(output);
  }

  public void displayCheckoutHistory(String output) {
    sectionHeader("CHECK-OUT HISTORY", "Newest matching room check-out records first.");
    System.out.println(output);
  }

  public void displayLateCheckoutHistory(String output) {
    sectionHeader("LATE CHECK-OUT HISTORY", "Newest late check-out extension records first.");
    System.out.println(output);
  }

  public void displayCheckoutConfirmationSummary(String output) {
    System.out.println();
    System.out.println("  --- CHECK-OUT CONFIRMATION SUMMARY ---");
    for (String line : output.split("\r?\n")) {
      System.out.println("  " + line);
    }
    System.out.println("  --------------------------------------");
  }

  public boolean confirmLateCheckout(String roomNumber) {
    System.out.println();
    System.out.print("  " + C + B + "Is room " + roomNumber
        + " a late check-out? (y/n) > " + R);
    String answer = ConsoleUI.readLine().trim().toLowerCase();
    return answer.equals("y") || answer.equals("yes");
  }

  public boolean confirmCheckout(String roomNumber) {
    System.out.println();
    System.out.print("  " + C + B + "Confirm check-out for room " + roomNumber
        + "? (y/n) > " + R);
    String answer = ConsoleUI.readLine().trim().toLowerCase();
    return answer.equals("y") || answer.equals("yes");
  }

  public boolean confirmLateCheckoutExtension(String roomNumber) {
    System.out.println();
    System.out.print("  " + C + B + "Confirm late check-out update for room "
        + roomNumber + "? (y/n) > " + R);
    String answer = ConsoleUI.readLine().trim().toLowerCase();
    return answer.equals("y") || answer.equals("yes");
  }

  public void displayReport(String title, String content) {
    sectionHeader(title, "Results sorted and prepared for front-desk reporting.");
    System.out.println(content);
  }

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

  public void displayMemberList(String output) {
    sectionHeader("MEMBER RECORDS", "Records are displayed by member ID.");
    if (output.isEmpty()) {
      System.out.println("  " + DM + "(No member records found)" + R);
    } else {
      System.out.println("  " + IB + B
          + String.format("%-12s %-22s %-12s %-10s %s",
              "Member ID", "Name", "Tier", "Points", "Expiry")
          + R);
      System.out.println("  " + rep('-', 74));
      for (String line : output.split("\r?\n")) {
        if (!line.trim().isEmpty()) {
          System.out.println("  " + line);
        }
      }
    }
  }

  private void printBorder() {
    System.out.println("  " + SB + B + "+" + rep(HL, BOX_W) + "+" + R);
  }

  private void printTitle(String title, String sub) {
    rowV(centerPad(B + C + title + R, title.length()), BOX_W);
    rowV(centerPad(DM + sub + R, sub.length()), BOX_W);
  }

  private void printSectionLabel(String label) {
    rowV("  " + IB + B + label + R, 2 + label.length());
  }

  private void printEntry(String num, String label, String desc) {
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
    int vis = 7 + label.length();
    rowV(" " + RD + B + "[" + num + "]  " + label + R, vis);
  }

  private void rowV(String content, int visibleLen) {
    int pad = BOX_W - visibleLen;
    System.out.println("  " + SB + B + VL + R
        + content + rep(' ', Math.max(0, pad))
        + SB + B + VL + R);
  }

  private String centerPad(String colored, int plainLen) {
    int left = (BOX_W - plainLen) / 2;
    int right = BOX_W - plainLen - left;
    return rep(' ', left) + colored + rep(' ', right);
  }

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

  private String padR(String value, int width) {
    return value.length() >= width ? value : value + rep(' ', width - value.length());
  }

  private String toMemberDetailsString(RewardsMember memberRecord) {
    return "  Member ID        : " + memberRecord.getMemberId()
        + "\n  Name             : " + memberRecord.getName()
        + "\n  Email            : " + memberRecord.getEmail()
        + "\n  Tier             : " + memberRecord.getTier()
        + "\n  Reward Points    : " + memberRecord.getPoints()
        + "\n  Points Expiry    : " + memberRecord.getPointsExpiryDate()
        + "\n  Promotion        : " + memberRecord.getPromotion();
  }

  private String toMemberAccountString(RewardsMember memberRecord) {
    return "  Member ID        : " + memberRecord.getMemberId()
        + "\n  Name             : " + memberRecord.getName()
        + "\n  Tier             : " + memberRecord.getTier()
        + "\n  Reward Points    : " + memberRecord.getPoints()
        + "\n  Points Expiry    : " + memberRecord.getPointsExpiryDate()
        + "\n  Promotion        : " + memberRecord.getPromotion();
  }

  private String rep(char character, int count) {
    if (count <= 0) return "";
    StringBuilder builder = new StringBuilder(count);
    for (int index = 0; index < count; index++) {
      builder.append(character);
    }
    return builder.toString();
  }
}
