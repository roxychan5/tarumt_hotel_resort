package boundary;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import utility.ConsoleUI;
import utility.MalaysiaTime;
import utility.MessageUI;

/**
 * The "screen & keyboard" part of the Loyalty & Rewards module.
 *
 * This class ONLY handles what the user sees and types:
 *   - prints the menu and pretty boxes
 *   - asks questions (member ID, points, tier, etc.)
 *   - shows member / report tables
 *
 * Returning null from any input method signals that the user pressed 0
 * to cancel the current operation.  The controller checks for null and
 * returns early without performing any changes.
 *
 * @author Kwan Hui Xuan
 */
public class LoyaltyRewardsUI {

    // BOX_W must be wide enough for the widest table row.
    private static final int  BOX_W   = 88;
    private static final int  LABEL_W = 30;   // label column in menu entries
    private static final char HL      = '-';
    private static final char VL      = '|';

    // ── Color shortcuts ──────────────────────────────────────────────────
    private static final String R  = ConsoleUI.RESET;
    private static final String B  = ConsoleUI.BOLD;
    private static final String C  = ConsoleUI.CYAN;
    private static final String IB = ConsoleUI.ICE_BLUE;
    private static final String SB = ConsoleUI.SKY_BLUE;
    private static final String DM = ConsoleUI.DIM;
    private static final String WH = ConsoleUI.WHITE;
    private static final String RD = ConsoleUI.RED;

    /** Set -Dtarumt.animations=false when instant output is preferred. */
    private static final boolean ANIMATIONS_ENABLED =
            !"false".equalsIgnoreCase(
                    System.getProperty("tarumt.animations", "true"));
    private static final int DEFAULT_PROGRESS_WIDTH = 34;

    // ======================================================================
    // Main Menu
    // ======================================================================

    public int getMenuChoice() {
        ConsoleUI.clearScreen();
        printMenu();
        return ConsoleUI.readMenuChoice(
                "  " + SB + B + "  Select option (0-12) > " + R + " ");
    }

    private void printMenu() {
        System.out.println();
        printBorder();
        printTitle("LOYALTY  &  REWARDS  SERVICE", "Module : Loyalty Member & Rewards Management");
        printBorder();

        printSectionLabel("MEMBER MANAGEMENT");
        printEntry(" 1", "Register Member Profile",  "Create a member and personalised promotion");
        printEntry(" 2", "View Member Profile",       "Points, tier and promotion details");
        printEntry(" 3", "Add Reward Points",         "Accumulate points and assess tier upgrade");
        printEntry(" 4", "Redeem Points",             "Submit a redemption request");
        printEntry(" 5", "Edit Member",               "Search ArrayList -> modify name or email");
        printEntry(" 6", "Delete Member",             "Search ArrayList -> remove from ADT");
        printEntry(" 7", "Delete History",            "Restore deleted members within 30 days");
        printBorder();

        printSectionLabel("NOTIFICATIONS  &  SEARCH");
        printEntry(" 8", "Expiring Points Alerts",   "Notifications due within 30 days");
        printEntry(" 9", "View All Members",          "Display all registered loyalty members");
        printEntry("10", "Search Member",             "Search member by ID or name");
        printBorder();

        printSectionLabel("REPORTS");
        printEntryHighlight("11", "Tier and Points Analysis",
                "Bubble sort + filter by tier & points  | PDF");
        printEntryHighlight("12", "Points Expiry and Risk Report",
                "Expiry alerts + high-balance members   | PDF");
        printBorder();

        printTierLegend();
        printBorder();

        printBack(" 0", "Back to Main Menu");
        printBorder();
        System.out.println();
    }

    // ======================================================================
    // Box drawing helpers
    // ======================================================================

    private void printBorder() {
        System.out.println("  " + SB + B + "+" + rep(HL, BOX_W) + "+" + R);
    }

    private void printTitle(String title, String sub) {
        rowV(centerPad(B + C + title + R, title.length()), BOX_W);
        rowV(centerPad(DM + sub + R,       sub.length()),   BOX_W);
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
        rowV(" " + RD + B + "[" + num + "]  " + label + R, 7 + label.length());
    }

    private void printTierLegend() {
        int vis = 9 + 8 + 6 + 10 + 9 + 7;
        rowV("  " + IB + "Tier:  " + R
                + " \033[47m\033[30mSILVER\033[0m "
                + " \033[103m\033[30mGOLD\033[0m "
                + " \033[46m\033[97mPLATINUM\033[0m "
                + " \033[44m\033[97mDIAMOND\033[0m "
                + " \033[45m\033[97mELITE\033[0m ",
                vis);
    }

    /**
     * Core row printer.
     * visibleLen = printable characters in content (ANSI codes excluded).
     * Right-pads to BOX_W then wraps with | on each side.
     */
    private void rowV(String content, int visibleLen) {
        int pad = BOX_W - visibleLen;
        System.out.println("  " + SB + B + VL + R
                + content + rep(' ', Math.max(0, pad))
                + SB + B + VL + R);
    }

    /** Empty row — vertical spacer inside any box. */
    private void rowBlank() {
        rowV("", 0);
    }

    /**
     * Key–value detail row: "  Label        : value"
     * key is left-padded to KV_KEY_W chars so all values align.
     */
    private static final int KV_KEY_W = 16;

    private void rowKV(String key, String value, boolean highlight) {
        String prefix = "  " + String.format("%-" + KV_KEY_W + "s : ", key);
        if (highlight) {
            rowV(prefix + WH + B + value + R, prefix.length() + value.length());
        } else {
            rowV(prefix + value, prefix.length() + value.length());
        }
    }

    /** Section sub-label inside an open box: "  -- LABEL --" */
    private void rowSubLabel(String label) {
        String text = "  " + rep(HL, 2) + " " + label + " " + rep(HL, 2);
        rowV(IB + B + text + R, text.length());
    }

    private String centerPad(String colored, int plainLen) {
        int left  = (BOX_W - plainLen) / 2;
        int right = BOX_W - plainLen - left;
        return rep(' ', left) + colored + rep(' ', right);
    }

    // ======================================================================
    // PDF export helpers
    // ======================================================================

    /** Asks whether the user wants to save the report as a PDF. */
    public boolean confirmPdfExport() {
        System.out.println();
        return readYesNo(
                "  " + C + B + "Export as professional PDF? (y/n) > " + R);
    }

    /** Prints a success banner with the exported file path. */
    public void displayPdfExportSuccess(String filePath) {
        System.out.println();
        System.out.println("  " + C + B + "+" + rep(HL, 60) + "+" + R);
        System.out.println("  " + C + B + "|  [OK]  PDF report exported successfully!"
                + rep(' ', 20) + "|" + R);
        System.out.println("  " + C + B + "|  " + R + "Path: " + IB + filePath + R);
        System.out.println("  " + C + B + "+" + rep(HL, 60) + "+" + R);
        System.out.println();
    }

    // ======================================================================
    // Tier filter sub-menu
    // ======================================================================

    public String tierFilter() {
        System.out.println();
        System.out.println("  " + SB + B + "SELECT LOYALTY TIER" + R);
        System.out.println("  " + rep(HL, 40));
        System.out.println("  " + SB + "[1]" + R + " Silver      "
                + SB + "[2]" + R + " Gold");
        System.out.println("  " + SB + "[3]" + R + " Platinum    "
                + SB + "[4]" + R + " Diamond");
        System.out.println("  " + SB + "[5]" + R + " Elite       "
                + SB + "[6]" + R + " All Tiers");
        System.out.println();
        while (true) {
            switch (ConsoleUI.readMenuChoice("  Select tier (1-6) > ")) {
                case 1: return "SILVER";
                case 2: return "GOLD";
                case 3: return "PLATINUM";
                case 4: return "DIAMOND";
                case 5: return "ELITE";
                case 6: return "ALL";
                default: MessageUI.displayErrorMessage("Enter a number from 1 to 6.");
            }
        }
    }

    // ======================================================================
    // Sort option sub-menu
    // ======================================================================

    public int getSortChoice() {
        System.out.println();
        System.out.println("  " + SB + B + "SORT REPORT RESULTS" + R);
        System.out.println("  " + rep(HL, 40));
        System.out.println("  " + SB + "[1]" + R + " Points - Ascending      "
                + SB + "[2]" + R + " Points - Descending");
        System.out.println("  " + SB + "[3]" + R + " Member Name - Ascending "
                + SB + "[4]" + R + " Tier Priority - Descending");
        System.out.println();
        while (true) {
            int choice = ConsoleUI.readMenuChoice("  Select sorting method (1-4) > ");
            if (choice >= 1 && choice <= 4) return choice;
            MessageUI.displayErrorMessage("Enter a number from 1 to 4.");
        }
    }

    // ======================================================================
    // Edit / Delete helpers
    // ======================================================================

    /**
     * Sub-menu: which field does the user want to edit?
     * Returns 1 = Name, 2 = Email, 3 = Points, 0 = Cancel.
     */
    public int inputEditChoice() {
        System.out.println();
        System.out.println("  " + SB + B + "SELECT FIELD TO EDIT" + R);
        System.out.println("  " + rep(HL, 40));
        System.out.println("  " + SB + "[1]" + R + " Name");
        System.out.println("  " + SB + "[2]" + R + " Email");
        System.out.println("  " + SB + "[3]" + R + " Points");
        System.out.println("  " + RD + B + "[0]" + R + " Cancel");
        System.out.println();
        while (true) {
            int choice = ConsoleUI.readMenuChoice("  Select field (0-3) > ");
            if (choice >= 0 && choice <= 3) return choice;
            MessageUI.displayErrorMessage("Enter 0, 1, 2, or 3.");
        }
    }

    /** Returns 1 to restore a deleted member, or 0 to return. */
    public int inputDeleteHistoryChoice() {
        System.out.println();
        System.out.println("  " + SB + B + "DELETE HISTORY ACTION" + R);
        System.out.println("  " + rep(HL, 40));
        System.out.println("  " + SB + "[1]" + R + " Restore a deleted member");
        System.out.println("  " + RD + B + "[0]" + R + " Return to loyalty menu");
        System.out.println();
        while (true) {
            int choice = ConsoleUI.readMenuChoice("  Select action (0-1) > ");
            if (choice == 0 || choice == 1) return choice;
            MessageUI.displayErrorMessage(
                    "Invalid delete-history action: enter 1 to restore or 0 to return.");
        }
    }

    /**
     * Displays the tier threshold table, then asks for a new points value.
     * Returns -1 if the user enters "0" to cancel.
     */
    public int inputNewPoints(int currentPoints) {
        System.out.println();
        printBorder();
        rowV(centerPad(B + C + "LOYALTY TIER THRESHOLDS" + R,
                "LOYALTY TIER THRESHOLDS".length()), BOX_W);
        printBorder();
        rowBlank();

        // Column header
        String hdr = String.format("  %-12s %-20s %s", "Tier", "Points Required", "Benefit");
        rowV(IB + B + hdr + R, hdr.length());
        rowV("  " + rep(HL, 60), 2 + 60);

        // Tier rows
        printTierRow("\033[47m\033[30m SILVER \033[0m",   "SILVER",   "0 - 999",      "5% dining discount");
        printTierRow("\033[103m\033[30m GOLD \033[0m",    "GOLD",     "1,000 - 2,999","8% dining discount");
        printTierRow("\033[46m\033[97m PLATINUM \033[0m", "PLATINUM", "3,000 - 5,999","10% room upgrade offer");
        printTierRow("\033[44m\033[97m DIAMOND \033[0m",  "DIAMOND",  "6,000 - 9,999","15% spa and dining offer");
        printTierRow("\033[45m\033[97m ELITE \033[0m",    "ELITE",    "10,000+",      "20% suite upgrade offer");

        rowBlank();
        rowKV("Current Points", String.valueOf(currentPoints), true);
        rowBlank();
        printBorder();
        System.out.println();

        System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
        while (true) {
            System.out.print("  " + SB + "New points value (0 or above)" + R + " > ");
            String raw = ConsoleUI.readLine().trim();
            if (raw.equals("0")) return -1;
            try {
                int value = Integer.parseInt(raw);
                if (value >= 0) return value;
            } catch (NumberFormatException ignored) { }
            MessageUI.displayErrorMessage(
                    "Enter a whole number >= 0.  Enter 0 to cancel.");
        }
    }

    /**
     * Shows a before -> after panel for an edit operation.
     * When field is "Points", newTier should be passed as the extra line;
     * use the overloaded version below for that case.
     */
    public void displayEditResult(String memberId, String field,
            String oldValue, String newValue) {
        System.out.println();
        printBorder();
        rowV(centerPad(B + C + "MEMBER UPDATED" + R,
                "MEMBER UPDATED".length()), BOX_W);
        printBorder();
        rowBlank();
        rowKV("Member ID", memberId,  true);
        rowKV("Field",     field,     false);
        rowKV("Before",    oldValue,  false);
        rowKV("After",     newValue,  false);
        rowBlank();
        printBorder();
        System.out.println();
    }

    /**
     * Overload for points edits — also shows the resulting tier.
     */
    public void displayEditResult(String memberId, String field,
            String oldValue, String newValue, String newTier) {
        try {
            animateNumberTransition("Updating reward balance",
                    Integer.parseInt(oldValue), Integer.parseInt(newValue));
        } catch (NumberFormatException ignored) {
            // The final values are still displayed normally below.
        }
        System.out.println();
        printBorder();
        rowV(centerPad(B + C + "MEMBER UPDATED" + R,
                "MEMBER UPDATED".length()), BOX_W);
        printBorder();
        rowBlank();
        rowKV("Member ID", memberId,         true);
        rowKV("Field",     field,            false);
        rowKV("Before",    oldValue,         false);
        rowKV("After",     newValue,         false);
        rowKV("New Tier",  colorTier(newTier), false);
        rowBlank();
        try {
            displayTierProgress(Integer.parseInt(newValue), newTier);
        } catch (NumberFormatException ignored) {
            // This overload is normally used for numeric point updates.
        }
        rowBlank();
        printBorder();
        System.out.println();
    }

    /**
     * Shows the member's details and asks "are you sure?" before deletion.
     * Only Y/y confirms and only N/n declines; all other input is rejected.
     */
    public boolean confirmDelete(String memberId, String name,
            String tier, int points) {
        System.out.println();
        printBorder();
        rowV(centerPad(B + RD + "CONFIRM DELETE MEMBER" + R,
                "CONFIRM DELETE MEMBER".length()), BOX_W);
        printBorder();
        rowBlank();
        rowKV("Member ID", memberId,              true);
        rowKV("Name",      name,                  false);
        rowKV("Tier",      colorTier(tier),        false);
        rowKV("Points",    String.valueOf(points), false);
        rowBlank();
        rowV("  " + RD + B + "[!!] This member will move to Delete History for 30 days." + R,
                2 + "[!!] This member will move to Delete History for 30 days.".length());
        rowBlank();
        printBorder();
        System.out.println();
        return readYesNo("  " + RD + B + "Confirm DELETE " + memberId
                + "? (y/n) > " + R);
    }

    // ======================================================================
    // Registration helper
    // ======================================================================

    /**
     * Shows the system-generated member ID inside the bordered box before
     * the user is asked to fill in the remaining registration details.
     */
    public void displayAutoGeneratedId(String memberId) {
        System.out.println();
        printBorder();
        rowV(centerPad(B + C + "REGISTER NEW MEMBER" + R,
                "REGISTER NEW MEMBER".length()), BOX_W);
        printBorder();
        rowBlank();
        rowV("  " + DM + "Member ID is assigned automatically by the system." + R,
                2 + "Member ID is assigned automatically by the system.".length());
        rowBlank();
        rowKV("Member ID", memberId, true);
        rowBlank();
        rowV("  " + IB + "Please fill in the following details to complete registration." + R,
                2 + "Please fill in the following details to complete registration.".length());
        rowBlank();
        printBorder();
        System.out.println();
    }

    // ======================================================================
    // Input helpers  — every method prints "[0] Cancel" hint
    //                  and returns null when the user types 0
    // ======================================================================

    /**
     * Asks for a Member ID.
     * Returns null if the user enters "0" to cancel.
     */
    public String memberId() {
        System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
        while (true) {
            System.out.print("  " + SB + "Member ID" + R + " (e.g. LM001) > ");
            String raw = ConsoleUI.readLine().trim();
            if (raw.equals("0")) return null;
            String value = raw.toUpperCase();
            if (value.matches("LM[0-9]{3,6}")) return value;
            if (value.isEmpty()) {
                MessageUI.displayErrorMessage(
                        "Member ID cannot be empty.  Format: LM followed by 3-6 digits (e.g. LM001).");
            } else if (!value.startsWith("LM")) {
                MessageUI.displayErrorMessage(
                        "\"" + raw + "\" is invalid — Member ID must start with LM (e.g. LM001).");
            } else {
                MessageUI.displayErrorMessage(
                        "\"" + raw + "\" is invalid — digits after LM must be 3 to 6 characters (e.g. LM001).");
            }
        }
    }

    /**
     * Asks for a search keyword.
     * Returns null if the user enters "0" to cancel.
     */
    public String searchKeyword() {
        System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
        while (true) {
            System.out.print("  " + SB
                    + "Member ID, name, email or tier to search" + R + " > ");
            String value = ConsoleUI.readLine().trim();
            if (value.equals("0")) return null;
            if (value.matches("[A-Za-z0-9@._+ '\\-]{2,100}")) return value;
            MessageUI.displayErrorMessage(
                    "Search keyword must contain 2 to 100 valid characters. "
                    + "You may enter a member ID, name, email address, or tier. "
                    + "Enter 0 to cancel.");
        }
    }

    /**
     * Asks for the member's full name.
     * Returns null if the user enters "0" to cancel.
     */
    public String name() {
        System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
        while (true) {
            System.out.print("  " + SB + "Member name" + R + " > ");
            String value = ConsoleUI.readLine().trim();
            if (value.equals("0")) return null;
            if (value.matches("[A-Za-z][A-Za-z .'-]{1,59}")) return value;
            if (value.isEmpty()) {
                MessageUI.displayErrorMessage("Name cannot be empty.  Enter a valid full name.");
            } else if (!value.matches("[A-Za-z].*")) {
                MessageUI.displayErrorMessage(
                        "\"" + value + "\" is invalid — name must start with a letter.");
            } else {
                MessageUI.displayErrorMessage(
                        "\"" + value + "\" is invalid — only letters, spaces, dots, hyphens and apostrophes are allowed (2-60 chars).");
            }
        }
    }

    /**
     * Asks for an email address.
     * Returns null if the user enters "0" to cancel.
     */
    public String email() {
        System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
        while (true) {
            System.out.print("  " + SB + "Email address" + R + " > ");
            String value = ConsoleUI.readLine().trim();
            if (value.equals("0")) return null;
            if (value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) return value;
            if (value.isEmpty()) {
                MessageUI.displayErrorMessage("Email cannot be empty.  Example: john@example.com");
            } else if (!value.contains("@")) {
                MessageUI.displayErrorMessage(
                        "\"" + value + "\" is missing '@'.  Example: john@example.com");
            } else if (!value.contains(".")) {
                MessageUI.displayErrorMessage(
                        "\"" + value + "\" is missing a domain (e.g. .com).  Example: john@example.com");
            } else {
                MessageUI.displayErrorMessage(
                        "\"" + value + "\" is not a valid email address.  Example: john@example.com");
            }
        }
    }

    /**
     * Asks for a positive integer points value.
     * Returns -1 if the user enters "0" to cancel.
     */
    public int positivePoints(String prompt) {
        System.out.println("  " + DM + "(Enter 0 to cancel)" + R);
        while (true) {
            System.out.print("  " + SB + prompt + R + " > ");
            String raw = ConsoleUI.readLine().trim();
            if (raw.equals("0")) return -1;
            try {
                int value = Integer.parseInt(raw);
                if (value > 0) return value;
            } catch (NumberFormatException ignored) { }
            MessageUI.displayErrorMessage(
                    "Points must be greater than zero.  Enter 0 to cancel.");
        }
    }

    /** Asks for a minimum points threshold (0 or above). */
    public int minimumPoints() {
        while (true) {
            System.out.print("  " + SB + "Minimum points (0 or above)" + R + " > ");
            try {
                int value = Integer.parseInt(ConsoleUI.readLine().trim());
                if (value >= 0) return value;
            } catch (NumberFormatException ignored) { }
            MessageUI.displayErrorMessage("Minimum points cannot be negative.");
        }
    }

    /** Asks for a maximum points threshold (must be >= minimum). */
    public int maximumPoints(int minimum) {
        while (true) {
            System.out.print(
                    "  " + SB + "Maximum points (" + minimum + " or above)" + R + " > ");
            try {
                int value = Integer.parseInt(ConsoleUI.readLine().trim());
                if (value >= minimum) return value;
            } catch (NumberFormatException ignored) { }
            MessageUI.displayErrorMessage(
                    "Maximum points must be greater than or equal to minimum points.");
        }
    }

    /** Asks "are you sure?" before a generic action. */
    public boolean confirmAction(String prompt) {
        System.out.println();
        return readYesNo("  " + WH + B + prompt + "  "
                + SB + B + "[Y] Yes" + R + "    "
                + RD + B + "[N] No" + R + " > ");
    }

    // ======================================================================
    // Display helpers — all output rendered inside the bordered box
    // ======================================================================

    /**
     * General-purpose display.  The controller passes a multi-line
     * pre-formatted String; every line is rendered inside the bordered
     * box via rowV().
     *
     * Lines that are pure dashes      → dimmed separator
     * Lines that are ALL-CAPS headings → IB+B sub-label
     * Everything else                 → plain data row
     */
    public void display(String title, String content) {
        System.out.println();
        printBorder();
        rowV(centerPad(B + C + title + R, title.length()), BOX_W);
        printBorder();
        rowBlank();

        if (content != null && !content.isEmpty()) {
            for (String raw : content.split("\r?\n", -1)) {
                String line = raw.replace("\r", "");

                if (line.isEmpty()) {
                    rowBlank();
                } else if (line.matches("[\\-]+")) {
                    rowV("  " + DM + line + R, 2 + line.length());
                } else if (line.matches("[A-Z][A-Z &/]+") && !line.contains(":")) {
                    rowSubLabel(line);
                } else if (line.trim().matches("-+")) {
                    rowV("  " + DM + line.trim() + R, 2 + line.trim().length());
                } else {
                    rowV("  " + line, 2 + line.length());
                }
            }
        }

        rowBlank();
        printBorder();
        System.out.println();
    }

    /** Shows the full profile of one loyalty member as a KV panel. */
    public void displayMemberProfile(String memberId, String name,
            String email, String tier, int points, String expiry,
            String promotion) {
        animateFadeIn("Loading loyalty member profile");
        System.out.println();
        printBorder();
        rowV(centerPad(B + C + "LOYALTY MEMBER PROFILE" + R,
                "LOYALTY MEMBER PROFILE".length()), BOX_W);
        printBorder();
        rowBlank();
        rowKV("Member ID",     memberId,              true);
        rowKV("Name",          name,                  false);
        rowKV("Email",         email,                 false);
        rowKV("Tier",          colorTier(tier),        false);
        rowKV("Reward Points", String.valueOf(points), true);
        rowKV("Points Expire", expiry,                false);
        rowKV("Promotion",     promotion,             false);
        rowBlank();
        displayTierProgress(points, tier);
        displayValidityProgress(expiry);
        rowBlank();
        printBorder();
        System.out.println();
    }

    /** Shows the result of an Add Points or Redeem Points operation. */
    public void displayPointsResult(String memberId, String operation,
            int delta, int newTotal, String newTier) {
        int oldTotal = Math.max(0, newTotal - delta);
        animateNumberTransition("Updating reward balance", oldTotal, newTotal);
        animateFadeIn(operation);
        System.out.println();
        printBorder();
        rowV(centerPad(B + C + operation.toUpperCase() + R,
                operation.length()), BOX_W);
        printBorder();
        rowBlank();
        rowKV("Member ID",     memberId,                          true);
        rowKV("Points Change", (delta >= 0 ? "+" : "") + delta,  false);
        rowKV("New Balance",   String.valueOf(newTotal),          true);
        rowKV("New Tier",      colorTier(newTier),                false);
        rowBlank();
        displayTierProgress(newTotal, newTier);
        rowBlank();
        printBorder();
        System.out.println();
    }

    // ======================================================================
    // Private utilities
    // ======================================================================

    /** Reads a strict one-character Y/N response and retries invalid input. */
    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = ConsoleUI.readLine().trim();
            if (answer.equalsIgnoreCase("y")) return true;
            if (answer.equalsIgnoreCase("n")) return false;
            MessageUI.displayErrorMessage(
                    "Invalid response: enter Y or N only.");
        }
    }

    /** Displays progress within the member's current tier using real thresholds. */
    private void displayTierProgress(int points, String tier) {
        int floor = tierFloor(tier);
        int target = nextTierThreshold(tier);
        String nextTier = nextTierName(tier);
        int percentage;
        String milestone;

        if (target < 0) {
            percentage = 100;
            milestone = "Maximum tier reached";
        } else {
            int range = Math.max(1, target - floor);
            percentage = clampPercentage((int) Math.min(100L,
                    ((long) points - floor) * 100L / range));
            milestone = Math.max(0, target - points) + " points to " + nextTier;
        }

        rowSubLabel("TIER PROGRESS");
        rowProgress(percentage);
        rowKV("Next Milestone", milestone, false);
    }

    /** Displays the remaining portion of the standard one-year points validity. */
    private void displayValidityProgress(String expiry) {
        try {
            LocalDate expiryDate = LocalDate.parse(expiry);
            long days = ChronoUnit.DAYS.between(
                    MalaysiaTime.now().toLocalDate(), expiryDate);
            int percentage = clampPercentage((int) Math.floor(days * 100.0 / 365.0));
            String label = days < 0
                    ? "Expired " + Math.abs(days) + " day(s) ago"
                    : days + " day(s) remaining";

            rowBlank();
            rowSubLabel("POINTS VALIDITY");
            rowProgress(percentage);
            rowKV("Validity", label, false);
        } catch (RuntimeException ignored) {
            // A legacy/unparseable date remains visible in the profile above.
        }
    }

    private void rowProgress(int percentage) {
        int width = responsiveProgressWidth();
        int filled = (int) Math.round(width * clampPercentage(percentage) / 100.0);
        String bar = "[" + C + rep('=', filled) + R
                + DM + rep('-', width - filled) + R + "] "
                + String.format("%3d%%", clampPercentage(percentage));
        rowV("  " + bar, 2 + width + 7);
    }

    /** Keeps added visual elements usable in narrower terminals. */
    private int responsiveProgressWidth() {
        String columns = System.getenv("COLUMNS");
        if (columns == null) return DEFAULT_PROGRESS_WIDTH;
        try {
            int terminalColumns = Integer.parseInt(columns.trim());
            return Math.max(18, Math.min(DEFAULT_PROGRESS_WIDTH, terminalColumns - 46));
        } catch (NumberFormatException ignored) {
            return DEFAULT_PROGRESS_WIDTH;
        }
    }

    private int clampPercentage(int percentage) {
        return Math.max(0, Math.min(100, percentage));
    }

    private int tierFloor(String tier) {
        if (tier == null) return 0;
        switch (tier.toUpperCase()) {
            case "GOLD": return 1000;
            case "PLATINUM": return 3000;
            case "DIAMOND": return 6000;
            case "ELITE": return 10000;
            default: return 0;
        }
    }

    private int nextTierThreshold(String tier) {
        if (tier == null) return 1000;
        switch (tier.toUpperCase()) {
            case "SILVER": return 1000;
            case "GOLD": return 3000;
            case "PLATINUM": return 6000;
            case "DIAMOND": return 10000;
            default: return -1;
        }
    }

    private String nextTierName(String tier) {
        if (tier == null) return "GOLD";
        switch (tier.toUpperCase()) {
            case "SILVER": return "GOLD";
            case "GOLD": return "PLATINUM";
            case "PLATINUM": return "DIAMOND";
            case "DIAMOND": return "ELITE";
            default: return "ELITE";
        }
    }

    /** A short three-stage ANSI fade; it never affects the final layout. */
    private void animateFadeIn(String text) {
        if (!ANIMATIONS_ENABLED) return;
        String[] shades = {DM, IB, C + B};
        for (String shade : shades) {
            System.out.print("\r  " + shade + text + R);
            System.out.flush();
            animationDelay(35);
        }
        clearAnimationLine();
    }

    /** Smoothly interpolates a point balance in a maximum of ten short frames. */
    private void animateNumberTransition(String label, int from, int to) {
        if (!ANIMATIONS_ENABLED || from == to) return;
        final int frames = 10;
        for (int frame = 0; frame <= frames; frame++) {
            long value = from + Math.round((to - from) * (frame / (double) frames));
            System.out.print("\r  " + IB + label + R + "  "
                    + B + String.format("%,d", value) + R + " points");
            System.out.flush();
            animationDelay(25);
        }
        clearAnimationLine();
    }

    private void clearAnimationLine() {
        System.out.print("\r\033[2K");
        System.out.flush();
    }

    private void animationDelay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Prints one tier row inside the threshold table.
     * badge   = ANSI-colored badge string
     * tierName = plain tier name used to measure visible badge width
     * range   = points range string
     * benefit = promotion text
     */
    private void printTierRow(String badge, String tierName,
            String range, String benefit) {
        // visible: "  " + badge(plain=tierName+2) + padding + range + "  " + benefit
        int badgePlain = tierName.length() + 2;  // " TIER " has 2 extra spaces
        String row = String.format("  %-12s %-20s %s", "", range, benefit);
        // build manually so badge aligns in the 12-char tier column
        int vis = 2 + badgePlain + (12 - badgePlain) + 1 + 20 + 1 + benefit.length();
        String content = "  " + badge
                + rep(' ', Math.max(0, 12 - badgePlain))
                + " " + padR(range, 20)
                + " " + benefit;
        rowV(content, vis);
    }

    /** Returns an ANSI-colored badge string for a loyalty tier. */
    private String colorTier(String tier) {
        if (tier == null) return "Unknown";
        switch (tier.toUpperCase()) {
            case "SILVER":   return "\033[47m\033[30m SILVER \033[0m";
            case "GOLD":     return "\033[103m\033[30m GOLD \033[0m";
            case "PLATINUM": return "\033[46m\033[97m PLATINUM \033[0m";
            case "DIAMOND":  return "\033[44m\033[97m DIAMOND \033[0m";
            case "ELITE":    return "\033[45m\033[97m ELITE \033[0m";
            default:         return tier;
        }
    }

    private String padR(String s, int w) {
        return s.length() >= w ? s : s + rep(' ', w - s.length());
    }

    private String rep(char c, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
