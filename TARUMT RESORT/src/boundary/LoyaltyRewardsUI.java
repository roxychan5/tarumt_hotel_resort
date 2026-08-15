package boundary;

import utility.ConsoleUI;
import utility.MessageUI;

/** Console boundary for member profiles, points, rewards and notifications. */
public class LoyaltyRewardsUI {
  public int getMenuChoice() {
    ConsoleUI.displaySubHeader("LOYALTY & REWARDS SERVICE");
    ConsoleUI.displayMenuOption(1, "Register Member Profile", "Create a member and personalised promotion");
    ConsoleUI.displayMenuOption(2, "View Member Profile", "Points, tier and promotion");
    ConsoleUI.displayMenuOption(3, "Add Reward Points", "Accumulate points and assess tier upgrade");
    ConsoleUI.displayMenuOption(4, "Redeem Points", "Submit a redemption request");
    ConsoleUI.displayMenuOption(5, "Expiring Points Alerts", "Notifications due within 30 days");
    ConsoleUI.displayMenuOption(6, "View All Members");
    ConsoleUI.displayMenuOption(0, "Back to Main Menu");
    return ConsoleUI.readMenuChoice("\nSelect an option > ");
  }

  public String required(String prompt, String pattern, String error) {
    while (true) {
      System.out.print(prompt);
      String value = ConsoleUI.readLine().trim();
      if (value.matches(pattern)) return value;
      MessageUI.displayErrorMessage(error);
    }
  }

  public String memberId() { return required("Member ID (e.g. LM001): ", "(?i)LM[0-9]{3,6}", "Member ID must be LM followed by 3 to 6 digits.").toUpperCase(); }
  public String name() { return required("Member name: ", "[A-Za-z][A-Za-z .'-]{1,59}", "Enter a valid member name."); }
  public String email() { return required("Email address: ", "[^\\s@]+@[^\\s@]+\\.[^\\s@]+", "Enter a valid email address."); }

  public int positivePoints(String prompt) {
    while (true) {
      int value = ConsoleUI.readMenuChoice(prompt);
      if (value > 0) return value;
      MessageUI.displayErrorMessage("Points must be greater than zero.");
    }
  }

  public void display(String title, String content) {
    ConsoleUI.displaySubHeader(title);
    System.out.println(content);
  }
}
