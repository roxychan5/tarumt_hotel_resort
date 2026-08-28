package boundary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import utility.ConsoleUI;
import utility.MalaysiaTime;

// Boundary class for VIP and loyalty tier room allocation. //
public class VipLoyaltyAllocationUI {

  private static final int BOX_WIDTH = 76;
  private static final int LABEL_WIDTH = 29;
  private static final String RESET = ConsoleUI.RESET;
  private static final String BOLD = ConsoleUI.BOLD;
  private static final String CYAN = ConsoleUI.CYAN;
  private static final String ICE_BLUE = ConsoleUI.ICE_BLUE;
  private static final String SKY_BLUE = ConsoleUI.SKY_BLUE;
  private static final String DIM = ConsoleUI.DIM;
  private static final String WHITE = ConsoleUI.WHITE;
  private static final String RED = ConsoleUI.RED;

  public int getMenuChoice() {
    displayMenu();
    return ConsoleUI.readMenuChoice("  " + SKY_BLUE + BOLD + "Select option (0-7) > " + RESET);
  }

  // Displays the VIP Allocation menu without requesting another menu selection. //
  public void displayMenu() {
    ConsoleUI.clearScreen();
    System.out.println();
    printBorder();
    printTitle("VIP & LOYALTY TIER ALLOCATION", "Module : Priority Queue Management");
    printBorder();

    printSection("VIP GUEST QUEUE");
    printEntry("1", "Add Priority Guest", "Verify member ID and add guest to queue");
    printEntry("2", "View Next Priority Guest", "Show highest-tier waiting guest");
    printEntry("3", "View Full Priority Queue", "Review current allocation order");
    printBorder();

    printSection("ROOM ALLOCATION");
    printEntry("4", "Allocate Available Room", "Assign room to next priority guest");
    printEntry("5", "View Allocated Room Board", "View member allocated rooms details");
    printBorder();

    printSection("REPORTS & ANALYTICS");
    printHighlightEntry("6", "Priority Waiting List Report", "Filter guests by tier and room type");
    printHighlightEntry("7", "Allocation Performance Report", "Review completed VIP allocations");
    printBorder();

    printBack();
    printBorder();
    System.out.println();
  }

  private void printBorder() {
    System.out.println("  " + SKY_BLUE + BOLD + "+" + repeat('-', BOX_WIDTH) + "+" + RESET);
  }

  private void printTitle(String title, String subtitle) {
    printRow(center(title, CYAN + BOLD), BOX_WIDTH);
    printRow(center(subtitle, DIM), BOX_WIDTH);
  }

  private void printSection(String title) {
    printRow("  " + ICE_BLUE + BOLD + title + RESET, 2 + title.length());
  }

  private void printEntry(String number, String label, String description) {
    String content = " [" + number + "] " + WHITE + BOLD + padRight(label, LABEL_WIDTH) + RESET
        + "  " + DIM + description + RESET;
    printRow(content, 5 + LABEL_WIDTH + 2 + description.length());
  }

  private void printHighlightEntry(String number, String label, String description) {
    String content = " [" + number + "] " + CYAN + BOLD + padRight(label, LABEL_WIDTH) + RESET
        + "  " + ICE_BLUE + description + RESET;
    printRow(content, 5 + LABEL_WIDTH + 2 + description.length());
  }

  private void printBack() {
    String text = " [0]  Back to Main Menu";
    printRow(RED + BOLD + text + RESET, text.length());
  }

  private void printRow(String content, int visibleLength) {
    System.out.println("  " + SKY_BLUE + BOLD + "|" + RESET + content
        + repeat(' ', Math.max(0, BOX_WIDTH - visibleLength)) + SKY_BLUE + BOLD + "|" + RESET);
  }

  private String center(String text, String style) {
    int left = (BOX_WIDTH - text.length()) / 2;
    int right = BOX_WIDTH - text.length() - left;
    return repeat(' ', left) + style + text + RESET + repeat(' ', right);
  }

  private String padRight(String text, int width) {
    return text + repeat(' ', Math.max(0, width - text.length()));
  }

  private String repeat(char character, int count) {
    StringBuilder output = new StringBuilder(Math.max(0, count));
    for (int i = 0; i < count; i++) output.append(character);
    return output.toString();
  }

  public String inputMemberId() {
    while (true) {
      String memberId = readText("Enter member ID (LM001-LM999999, 0 to cancel): ").toUpperCase();
      if (memberId.equals("0")) return "";
      if (memberId.matches("LM[0-9]{3,6}")) return memberId;
      System.out.println("Member ID must be LM followed by 3 to 6 digits.");
    }
  }

  /** Shows registered loyalty members before staff selects a member ID. */
  public void displayRegisteredMembers(String output) {
    ConsoleUI.displaySubHeader("REGISTERED LOYALTY MEMBERS");
    System.out.println(output);
  }

  public void displayVerifiedMember(String memberId, String memberName, String tier) {
    System.out.println();
    System.out.println("Registered member: " + memberName + " (" + memberId + ")");
    System.out.println("Loyalty tier: " + tier);
  }

  // Gives staff a retry or registration route when a member ID is not found.//
  public int inputMissingMemberAction() {
    System.out.println("  1. Try again");
    System.out.println("  2. Register member in Loyalty & Rewards");
    while (true) {
      int choice = readInt("Select option: ");
      if (choice == 1 || choice == 2) return choice;
      System.out.println("Please select 1 or 2.");
    }
  }
  public String inputRequestedRoomType() {
    return inputRoomType("REQUESTED ROOM TYPE", "Select requested room type (1-6): ", false);
  }

  public LocalDate inputRequestedCheckOutDate(LocalDate checkInDate) {
    while (true) {
      String dateText = readText("Enter requested check-out date (yyyy-MM-dd): ");
      try {
        LocalDate checkOutDate = LocalDate.parse(dateText);
        if (!checkOutDate.isAfter(checkInDate)) {
          System.out.println("Check-out date must be after check-in date (" + checkInDate + ").");
        } else {
          return checkOutDate;
        }
      } catch (DateTimeParseException ex) {
        System.out.println("Please enter a valid date in yyyy-MM-dd format.");
      }
    }
  }

  public LocalDate inputRequestedCheckInDate() {
    LocalDate today = MalaysiaTime.now().toLocalDate();
    while (true) {
      String dateText = readText("Enter requested check-in date (yyyy-MM-dd): ");
      try {
        LocalDate checkInDate = LocalDate.parse(dateText);
        if (checkInDate.isBefore(today)) {
          System.out.println("Requested check-in date cannot be before today (" + today + ").");
        } else {
          return checkInDate;
        }
      } catch (DateTimeParseException ex) {
        System.out.println("Please enter a valid date in yyyy-MM-dd format.");
      }
    }
  }

  public void displayBookingSummary(String output) {
    ConsoleUI.displaySubHeader("VIP BOOKING SUMMARY");
    System.out.println(output);
  }

  public LocalDateTime inputCheckInAt() {
    LocalDate today = MalaysiaTime.now().toLocalDate();
    while (true) {
      String dateText = readText("Enter check-in date (yyyy-MM-dd, Enter for today): ");
      LocalDate checkInDate;
      try {
        checkInDate = dateText.isEmpty() ? today : LocalDate.parse(dateText);
      } catch (DateTimeParseException ex) {
        System.out.println("Please enter a valid date in yyyy-MM-dd format.");
        continue;
      }

      if (checkInDate.isBefore(today)) {
        System.out.println("Check-in date cannot be before today (" + today + ").");
        continue;
      }

      LocalTime defaultTime = checkInDate.equals(today)
          ? MalaysiaTime.now().toLocalTime().withSecond(0).withNano(0)
          : LocalTime.of(14, 0);
      while (true) {
        String timeText = readText("Enter check-in time (HH:mm, Enter for "
            + defaultTime + "): ");
        try {
          LocalTime checkInTime = timeText.isEmpty() ? defaultTime : LocalTime.parse(timeText);
          return LocalDateTime.of(checkInDate, checkInTime);
        } catch (DateTimeParseException ex) {
          System.out.println("Please enter a valid time in HH:mm format.");
        }
      }
    }
  }

  // Selects the room type that should be automatically assigned. //
  public String inputRoomTypeToAllocate() {
    return inputRoomType("ROOM TYPE TO ALLOCATE", "Select room type to allocate (0-6): ", true);
  }

  private String inputRoomType(String title, String prompt, boolean allowCancel) {
    System.out.println(title + ":");
    System.out.println("  1. Standard");
    System.out.println("  2. Deluxe");
    System.out.println("  3. Suite");
    System.out.println("  4. Family");
    System.out.println("  5. Executive");
    System.out.println("  6. Presidential");
    if (allowCancel) System.out.println("  0. Cancel");
    while (true) {
      switch (readInt(prompt)) {
        case 0:
          if (allowCancel) return "";
          break;
        case 1: return "Standard";
        case 2: return "Deluxe";
        case 3: return "Suite";
        case 4: return "Family";
        case 5: return "Executive";
        case 6: return "Presidential";
        default: break;
      }
      System.out.println(allowCancel
          ? "Please select a room type from 0 to 6."
          : "Please select a room type from 1 to 6.");
    }
  }

  public int inputMinimumTier() {
    System.out.println("Minimum tier filter: 1=Silver, 2=Gold, 3=Platinum, 4=Diamond, 5=Elite");
    while (true) {
      int tier = readInt("Enter minimum tier: ");
      if (tier >= 1 && tier <= 5) return tier;
      System.out.println("Please select a tier from 1 to 5.");
    }
  }

  public String inputRoomTypeFilter() {
    System.out.println("Requested room type filter:");
    System.out.println("  0. All room types");
    System.out.println("  1. Standard");
    System.out.println("  2. Deluxe");
    System.out.println("  3. Suite");
    System.out.println("  4. Family");
    System.out.println("  5. Executive");
    System.out.println("  6. Presidential");
    while (true) {
      switch (readInt("Select room type filter (0-6): ")) {
        case 0: return "";
        case 1: return "Standard";
        case 2: return "Deluxe";
        case 3: return "Suite";
        case 4: return "Family";
        case 5: return "Executive";
        case 6: return "Presidential";
        default: System.out.println("Please select a room type from 0 to 6.");
      }
    }
  }

  public void displayPriorityQueue(String output) {
    ConsoleUI.displaySubHeader("PRIORITY ALLOCATION QUEUE");
    System.out.println(output);
  }

  public void displayNextGuest(String details) {
    ConsoleUI.displaySubHeader("NEXT PRIORITY GUEST");
    System.out.println(details);
  }

  /** Shows every completed VIP room allocation with the guest stay dates. */
  public void displayAllocatedRoomBoard(String output) {
    ConsoleUI.displaySubHeader("VIP ALLOCATED ROOM BOARD");
    System.out.println(output);
  }

  public void displayReport(String title, String content) {
    ConsoleUI.displayHeader(title);
    System.out.println(content);
  }

  public boolean confirmPdfExport() {
    System.out.println();
    System.out.print("  " + CYAN + BOLD + "Export as professional PDF? (y/n) > " + RESET);
    String answer = ConsoleUI.readLine().trim().toLowerCase();
    return answer.equals("y") || answer.equals("yes");
  }

  public void displayPdfExportSuccess(String filePath) {
    System.out.println();
    System.out.println("  " + CYAN + BOLD + "PDF report exported successfully: " + RESET + filePath);
  }

  private int readInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = ConsoleUI.readLine().trim();
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException ex) {
        System.out.println("Please enter a whole number.");
      }
    }
  }

  private String readText(String prompt) {
    System.out.print(prompt);
    return ConsoleUI.readLine().trim();
  }
}
