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

  public static final String RESET = "\033[0m";
  public static final String BOLD = "\033[1m";
  public static final String CYAN = "\033[96m";
  public static final String LIGHT_BLUE = "\033[94m";
  public static final String SKY_BLUE = "\033[38;5;117m";
  public static final String ICE_BLUE = "\033[38;5;195m";
  public static final String STEEL_BLUE = "\033[38;5;39m";
  public static final String DEEP_BLUE = "\033[38;5;33m";
  public static final String MAGENTA = "\033[95m";
  public static final String PURPLE = "\033[35m";
  public static final String WHITE = "\033[97m";
  public static final String RED = "\033[91m";
  public static final String DIM = "\033[2m";
  public static final String NAVY_BG = "\033[48;5;17m";

  private static final String[] BANNER_GRADIENT = {
      WHITE, ICE_BLUE, CYAN, SKY_BLUE, LIGHT_BLUE, STEEL_BLUE
  };
  private static final String BANNER_GAP = "    ";
  private static final String[] BANNER_TARUMT = {
      "████████╗  █████╗  ██████╗  ██╗   ██╗ ███╗   ███╗ ████████╗",
      "╚══██╔══╝ ██╔══██╗ ██╔══██╗ ██║   ██║ ████╗ ████║ ╚══██╔══╝",
      "   ██║    ███████║ ██████╔╝ ██║   ██║ ██╔████╔██║    ██║",
      "   ██║    ██╔══██║ ██╔══██╗ ██║   ██║ ██║╚██╔╝██║    ██║",
      "   ██║    ██║  ██║ ██║  ██║ ╚██████╔╝ ██║ ╚═╝ ██║    ██║",
      "   ╚═╝    ╚═╝  ╚═╝ ╚═╝  ╚═╝  ╚═════╝  ╚═╝     ╚═╝    ╚═╝"
  };
  private static final String[] BANNER_RESORTS = {
      "██████╗  ███████╗ ██████╗  ██████╗  ██████╗  ████████╗ ███████╗",
      "██╔══██╗ ██╔════╝ ██╔════╝ ██╔══██╗ ██╔══██╗ ╚══██╔══╝ ██╔════╝",
      "██████╔╝ █████╗   ███████╗ ██║  ██║ ██████╔╝    ██║    ███████╗",
      "██╔══██╗ ██╔══╝   ╚════██║ ██║  ██║ ██╔══██╗    ██║    ╚════██║",
      "██║  ██║ ███████╗ ███████║ ╚█████╔╝ ██║  ██║    ██║    ███████║",
      "╚═╝  ╚═╝ ╚══════╝ ╚══════╝  ╚════╝  ╚═╝  ╚═╝    ╚═╝    ╚══════╝"
  };
  private static final int TARUMT_BLOCK_WIDTH = maxLineWidth(BANNER_TARUMT);
  private static final int RESORTS_BLOCK_WIDTH = maxLineWidth(BANNER_RESORTS);
  private static final String[] GLITCH_BANNER = buildGlitchBanner();

  /* Refreshed before every menu so the logo and menu stay centred after a resize. */
  private static int terminalWidth = resolveScreenWidth();
  private static final int BANNER_WIDTH = computeBannerWidth();
  private static final int MENU_BOX_WIDTH = 54;
  private static final int LAYOUT_WIDTH = Math.max(BANNER_WIDTH, MENU_BOX_WIDTH);

  private ConsoleUI() {
  }

  /** Clears the terminal and moves the cursor to the top-left corner. */
  public static void clearScreen() {
    terminalWidth = resolveScreenWidth();
    System.out.print("\033[H\033[2J\033[3J");
    System.out.flush();
  }

  /** Enables ANSI colors and UTF-8 output on Windows terminals when supported. */
  public static void enableAnsiColors() {
    if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
      return;
    }
    try {
      ProcessBuilder builder = new ProcessBuilder(
          "powershell",
          "-NoProfile",
          "-Command",
          "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
              + "[Console]::InputEncoding = [System.Text.Encoding]::UTF8");
      builder.redirectErrorStream(true);
      builder.start().waitFor();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } catch (Exception ignored) {
      // ANSI and UTF-8 may still work in Windows Terminal / VS Code integrated terminal.
    }
  }

  /** Prints the stable neon banner used on the main menu. */
  public static void displayGlitchBanner() {
    System.out.println();
    for (int i = 0; i < BANNER_TARUMT.length; i++) {
      printGradientBannerLine(i);
    }
    System.out.print(RESET);
    System.out.println();
  }

  /**
   * Displays a neon-styled menu box with a title and numbered options.
   *
   * @param title     header text inside the box
   * @param options   menu labels indexed from 1
   * @param exitLabel label for option 0
   */
  public static void displayNeonMenuBox(String title, String[] options, String exitLabel) {
    int innerWidth = 52;
    String borderColor = SKY_BLUE + BOLD;
    String top = borderColor + "╔" + repeat('═', innerWidth) + "╗" + RESET;
    String divider = borderColor + "╠" + repeat('═', innerWidth) + "╣" + RESET;
    String bottom = borderColor + "╚" + repeat('═', innerWidth) + "╝" + RESET;

    System.out.println(padToCenter(top));
    System.out.println(padToCenter(formatBoxLineCentered(title, innerWidth, ICE_BLUE + BOLD)));
    System.out.println(padToCenter(divider));

    for (int i = 0; i < options.length; i++) {
      String entry = String.format("%d. %s", i + 1, options[i]);
      System.out.println(padToCenter(formatBoxLine(entry, innerWidth, WHITE)));
    }

    System.out.println(padToCenter(formatBoxLine("0. " + exitLabel, innerWidth, RED + BOLD)));
    System.out.println(padToCenter(bottom));
    System.out.println();
  }

  /** Returns a prompt string padded so it appears centered on screen. */
  public static String centeredPrompt(String prompt) {
    return padToCenter(prompt);
  }

  private static String[] buildGlitchBanner() {
    String[] banner = new String[BANNER_TARUMT.length];
    for (int i = 0; i < BANNER_TARUMT.length; i++) {
      banner[i] = padRight(BANNER_TARUMT[i], TARUMT_BLOCK_WIDTH)
          + BANNER_GAP
          + padRight(BANNER_RESORTS[i], RESORTS_BLOCK_WIDTH);
    }
    return banner;
  }

  private static int maxLineWidth(String[] lines) {
    int maxWidth = 0;
    for (String line : lines) {
      maxWidth = Math.max(maxWidth, line.length());
    }
    return maxWidth;
  }

  private static String padRight(String text, int width) {
    if (text.length() >= width) {
      return text;
    }
    return text + repeat(' ', width - text.length());
  }

  private static int computeBannerWidth() {
    int maxWidth = 0;
    for (String line : GLITCH_BANNER) {
      maxWidth = Math.max(maxWidth, line.length());
    }
    return maxWidth;
  }

  private static int centerWidth() {
    return Math.max(terminalWidth, LAYOUT_WIDTH);
  }

  /** Uses the same centre point for the logo, menu box, and prompt. */
  private static int leftPaddingFor(int contentWidth) {
    return Math.max(0, (centerWidth() - contentWidth) / 2);
  }

  private static int resolveScreenWidth() {
    if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
      try {
        ProcessBuilder builder = new ProcessBuilder(
            "powershell",
            "-NoProfile",
            "-Command",
            "(Get-Host).UI.RawUI.WindowSize.Width");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (Scanner scanner = new Scanner(process.getInputStream()).useDelimiter("\\A")) {
          if (process.waitFor() == 0 && scanner.hasNext()) {
            int width = Integer.parseInt(scanner.next().trim());
            if (width > 0) {
              return width;
            }
          }
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      } catch (Exception ignored) {
        // Fall back to default width below.
      }
    }

    String columns = System.getenv("COLUMNS");
    if (columns != null) {
      try {
        int width = Integer.parseInt(columns.trim());
        if (width > 0) {
          return width;
        }
      } catch (NumberFormatException ignored) {
        // Fall back to default width below.
      }
    }
    return 120;
  }

  private static void printGradientBannerLine(int index) {
    String tarumt = padRight(BANNER_TARUMT[index], TARUMT_BLOCK_WIDTH);
    String resorts = padRight(BANNER_RESORTS[index], RESORTS_BLOCK_WIDTH);
    String content = tarumt + BANNER_GAP + resorts;

    if (content.trim().isEmpty()) {
      System.out.println();
      return;
    }

    String rowColor = BANNER_GRADIENT[index];

    // Print one complete row. Cursor-up rendering caused some terminals to
    // show every banner row twice and shifted the menu out of alignment.
    System.out.println(padToCenter(NAVY_BG + rowColor + BOLD + content + RESET));
  }

  private static String formatBoxLineCentered(String text, int innerWidth, String color) {
    String plain = stripAnsi(text);
    if (plain.length() > innerWidth) {
      plain = plain.substring(0, innerWidth - 3) + "...";
    }
    int padding = innerWidth - plain.length();
    int leftPad = padding / 2;
    int rightPad = padding - leftPad;
    return borderColorLine(
        repeat(' ', leftPad) + color + plain + RESET + repeat(' ', rightPad));
  }

  private static String formatBoxLine(String text, int innerWidth, String color) {
    String plain = stripAnsi(text);
    if (plain.length() > innerWidth) {
      plain = plain.substring(0, innerWidth - 3) + "...";
      text = plain;
    }
    int padding = innerWidth - plain.length();
    return borderColorLine(color + text + RESET + repeat(' ', padding));
  }

  private static String borderColorLine(String content) {
    return SKY_BLUE + BOLD + "║" + RESET + content + SKY_BLUE + BOLD + "║" + RESET;
  }

  private static String padToCenter(String line) {
    String plain = stripAnsi(line);
    int width = centerWidth();
    if (plain.length() >= width) {
      return line;
    }
    int leftPad = leftPaddingFor(plain.length());
    return repeat(' ', leftPad) + line;
  }

  private static String stripAnsi(String text) {
    return text.replaceAll("\033\\[[0-9;]*m", "");
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

  /** Prints a compact label that groups related actions in a console menu. */
  public static void displayMenuSection(String title) {
    System.out.println();
    System.out.println("  " + repeat('-', 74));
    System.out.println("  " + title.toUpperCase());
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
