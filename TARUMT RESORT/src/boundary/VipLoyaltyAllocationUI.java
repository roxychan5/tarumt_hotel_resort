package boundary;

import utility.ConsoleUI;

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
    return ConsoleUI.readMenuChoice("  " + SKY_BLUE + BOLD + "Select option (0-6) > " + RESET);
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
    printBorder();

    printSection("REPORTS & ANALYTICS");
    printHighlightEntry("5", "Priority Waiting List Report", "Filter guests by tier and room type");
    printHighlightEntry("6", "Allocation Performance Report", "Review completed VIP allocations");
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

  public String inputMemberId() { return readText("Enter member ID: ").toUpperCase(); }

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
    return inputRoomType("REQUESTED ROOM TYPE", "Select requested room type (1-6): ");
  }

  public int inputNumberOfNights() {
    while (true) {
      int nights = readInt("How many nights will the VIP stay: ");
      if (nights > 0 && nights <= 365) return nights;
      System.out.println("Please enter a stay between 1 and 365 nights.");
    }
  }

  // Selects the room type that should be automatically assigned. //
  public String inputRoomTypeToAllocate() {
    return inputRoomType("ROOM TYPE TO ALLOCATE", "Select room type to allocate (1-6): ");
  }

  private String inputRoomType(String title, String prompt) {
    System.out.println(title + ":");
    System.out.println("  1. Standard");
    System.out.println("  2. Deluxe");
    System.out.println("  3. Suite");
    System.out.println("  4. Family");
    System.out.println("  5. Executive");
    System.out.println("  6. Presidential");
    while (true) {
      switch (readInt(prompt)) {
        case 1: return "Standard";
        case 2: return "Deluxe";
        case 3: return "Suite";
        case 4: return "Family";
        case 5: return "Executive";
        case 6: return "Presidential";
        default: System.out.println("Please select a room type from 1 to 6.");
      }
    }
  }

  public int inputMinimumTier() {
    System.out.println("Minimum tier filter: 1=Silver, 2=Gold, 3=Platinum, 4=Diamond, 5=Elite");
    return readInt("Enter minimum tier: ");
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