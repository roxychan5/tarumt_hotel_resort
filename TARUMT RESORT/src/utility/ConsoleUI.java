package utility;

import java.util.Scanner;

/**
 * Reusable console display helpers for menus and reports.
 *
 * @author Your Name
 */
public class ConsoleUI {

  private static final Scanner INPUT = new Scanner(System.in);
  private static final int WIDTH = 78;

  private ConsoleUI() {
  }

  public static void displayHeader(String title) {
    System.out.println();
    printBorder('=');
    printCentered(title);
    printCentered("RESORTS MANAGEMENT SYSTEM");
    printBorder('=');
  }

  public static void displaySubHeader(String title) {
    System.out.println();
    printBorder('-');
    printLine(title);
    printBorder('-');
  }

  public static void displayTableHeader(String... columns) {
    System.out.println("+" + repeat('-', 90) + "+");
    for (String column : columns) {
      System.out.print(column);
    }
    System.out.println(repeat('-', 90));
  }

  public static void displayMenuOption(int number, String title, String description) {
    System.out.printf("  [%d] %-31s %s%n", number, title, description);
  }

  public static void displayMenuOption(int number, String title) {
    System.out.printf("  [%d] %s%n", number, title);
  }

  public static void displayDetailPanel(String title, String... lines) {
    System.out.println();
    printBorder('-');
    printLine(title);
    System.out.println("|" + repeat('-', WIDTH) + "|");
    for (String line : lines) {
      printLine(line);
    }
    printBorder('-');
  }

  /** Returns one line from the shared console input stream. */
  public static String readLine() {
    return INPUT.hasNextLine() ? INPUT.nextLine() : "";
  }

  /** Reads a whole-number menu choice and keeps prompting until it is valid. */
  public static int readMenuChoice(String prompt) {
    while (true) {
      System.out.print(prompt);
      String value = readLine().trim();
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException ex) {
        MessageUI.displayErrorMessage("Please enter a whole-number menu choice.");
      }
    }
  }

  private static void printBorder(char character) {
    System.out.println("+" + repeat(character, WIDTH) + "+");
  }

  private static void printCentered(String text) {
    System.out.println("|" + centerText(text, WIDTH) + "|");
  }

  private static void printLine(String text) {
    String content = "  " + text;
    if (content.length() > WIDTH) {
      content = content.substring(0, WIDTH - 3) + "...";
    }
    System.out.println("|" + String.format("%-" + WIDTH + "s", content) + "|");
  }

  private static String centerText(String text, int width) {
    if (text.length() >= width) {
      return text;
    }
    int padding = (width - text.length()) / 2;
    return repeat(' ', padding) + text + repeat(' ', width - padding - text.length());
  }

  private static String repeat(char character, int count) {
    StringBuilder builder = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      builder.append(character);
    }
    return builder.toString();
  }
}
