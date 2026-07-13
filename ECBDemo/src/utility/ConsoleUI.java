package utility;

/**
 * Reusable console display helpers for menus and reports.
 *
 * @author Your Name
 */
public class ConsoleUI {

  private ConsoleUI() {
  }

  public static void displayHeader(String title) {
    String line = repeat('=', 60);
    System.out.println("\n" + line);
    System.out.println(centerText(title, 60));
    System.out.println(line);
  }

  public static void displaySubHeader(String title) {
    System.out.println("\n" + repeat('-', 60));
    System.out.println("  " + title);
    System.out.println(repeat('-', 60));
  }

  public static void displayTableHeader(String... columns) {
    System.out.println();
    for (String column : columns) {
      System.out.print(column);
    }
    System.out.println("\n" + repeat('-', 90));
  }

  private static String centerText(String text, int width) {
    if (text.length() >= width) {
      return text;
    }
    int padding = (width - text.length()) / 2;
    return repeat(' ', padding) + text;
  }

  private static String repeat(char character, int count) {
    StringBuilder builder = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      builder.append(character);
    }
    return builder.toString();
  }
}
