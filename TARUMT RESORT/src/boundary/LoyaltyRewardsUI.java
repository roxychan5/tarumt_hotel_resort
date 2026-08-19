package boundary;

import utility.ConsoleUI;
import utility.MessageUI;

/**
 * Console boundary for member profiles, points, rewards and notifications.
 *
 * @author
 */
public class LoyaltyRewardsUI {

    public int getMenuChoice() {

        ConsoleUI.displaySubHeader("LOYALTY & REWARDS SERVICE");

        ConsoleUI.displayMenuOption(
                1,
                "Register Member Profile",
                "Create a member and personalised promotion"
        );

        ConsoleUI.displayMenuOption(
                2,
                "View Member Profile",
                "Points, tier and promotion"
        );

        ConsoleUI.displayMenuOption(
                3,
                "Add Reward Points",
                "Accumulate points and assess tier upgrade"
        );

        ConsoleUI.displayMenuOption(
                4,
                "Redeem Points",
                "Submit a redemption request"
        );

        ConsoleUI.displayMenuOption(
                5,
                "Expiring Points Alerts",
                "Notifications due within 30 days"
        );

        ConsoleUI.displayMenuOption(
                6,
                "View All Members",
                "Display all registered loyalty members"
        );

        ConsoleUI.displayMenuOption(
                7,
                "Search Member",
                "Search member by ID or name"
        );

        ConsoleUI.displayMenuOption(
                8,
                "Generate Reports",
                "Analyse loyalty members and reward points"
        );

        ConsoleUI.displayMenuOption(
                0,
                "Back to Main Menu"
        );

        return ConsoleUI.readMenuChoice("\nSelect an option > ");
    }

    public String required(
            String prompt,
            String pattern,
            String error) {

        while (true) {

            System.out.print(prompt);

            String value = ConsoleUI.readLine().trim();

            if (value.matches(pattern)) {
                return value;
            }

            MessageUI.displayErrorMessage(error);
        }
    }

    public String memberId() {

        return required(
                "Member ID (e.g. LM001): ",
                "(?i)LM[0-9]{3,6}",
                "Member ID must be LM followed by 3 to 6 digits."
        ).toUpperCase();
    }

    public String searchKeyword() {

        return required(
                "Enter member ID or name to search: ",
                "[A-Za-z0-9 .'-]{3,60}",
                "Search keyword must contain 3 to 60 valid characters."
        );
    }

    public String name() {

        return required(
                "Member name: ",
                "[A-Za-z][A-Za-z .'-]{1,59}",
                "Enter a valid member name."
        );
    }

    public String email() {

        return required(
                "Email address: ",
                "[^\\s@]+@[^\\s@]+\\.[^\\s@]+",
                "Enter a valid email address."
        );
    }

    public int positivePoints(String prompt) {

        while (true) {

            int value = ConsoleUI.readMenuChoice(prompt);

            if (value > 0) {
                return value;
            }

            MessageUI.displayErrorMessage(
                    "Points must be greater than zero."
            );
        }
    }

    public int getReportChoice() {

        ConsoleUI.displaySubHeader("LOYALTY REPORTS");

        ConsoleUI.displayMenuOption(
                1,
                "Tier and Points Analysis",
                "Analyse members by loyalty tier and reward points"
        );

        ConsoleUI.displayMenuOption(
                2,
                "Points Expiry and Risk Report",
                "Identify members with expiring points and high balances"
        );

        ConsoleUI.displayMenuOption(
                0,
                "Back"
        );

        return ConsoleUI.readMenuChoice("\nSelect report > ");
    }

    public int minimumPoints() {

        while (true) {

            int value = ConsoleUI.readMenuChoice(
                    "Minimum points (0 or above) > "
            );

            if (value >= 0) {
                return value;
            }

            MessageUI.displayErrorMessage(
                    "Minimum points cannot be negative."
            );
        }
    }

    public int maximumPoints(int minimum) {

        while (true) {

            int value = ConsoleUI.readMenuChoice(
                    "Maximum points (" + minimum + " or above) > "
            );

            if (value >= minimum) {
                return value;
            }

            MessageUI.displayErrorMessage(
                    "Maximum points must be greater than or equal to minimum points."
            );
        }
    }

    public String tierFilter() {

        ConsoleUI.displaySubHeader("SELECT LOYALTY TIER");

        ConsoleUI.displayMenuOption(1, "SILVER");
        ConsoleUI.displayMenuOption(2, "GOLD");
        ConsoleUI.displayMenuOption(3, "PLATINUM");
        ConsoleUI.displayMenuOption(4, "DIAMOND");
        ConsoleUI.displayMenuOption(5, "ELITE");
        ConsoleUI.displayMenuOption(6, "ALL TIERS");

        int choice = ConsoleUI.readMenuChoice("\nSelect tier > ");

        switch (choice) {

            case 1:
                return "SILVER";

            case 2:
                return "GOLD";

            case 3:
                return "PLATINUM";

            case 4:
                return "DIAMOND";

            case 5:
                return "ELITE";

            case 6:
                return "ALL";

            default:
                MessageUI.displayErrorMessage(
                        "Invalid tier choice."
                );

                return tierFilter();
        }
    }

    public int getSortChoice() {

        ConsoleUI.displaySubHeader("SORT REPORT RESULTS");

        ConsoleUI.displayMenuOption(
                1,
                "Points - Ascending"
        );

        ConsoleUI.displayMenuOption(
                2,
                "Points - Descending"
        );

        ConsoleUI.displayMenuOption(
                3,
                "Member Name - Ascending"
        );

        ConsoleUI.displayMenuOption(
                4,
                "Tier Priority - Descending"
        );

        return ConsoleUI.readMenuChoice(
                "\nSelect sorting method > "
        );
    }

    public void display(
            String title,
            String content) {

        ConsoleUI.displaySubHeader(title);

        System.out.println(content);
    }
}