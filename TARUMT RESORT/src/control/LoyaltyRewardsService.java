package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.LoyaltyRewardsUI;
import entity.LoyaltyTier;
import entity.RewardsMember;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import utility.MessageUI;

/** Manages loyalty profiles, reward points, redemptions and tier progression. */
public class LoyaltyRewardsService {
  private static final Path DATA_FILE = Paths.get("data", "loyalty_members.txt");
  private final ListInterface<RewardsMember> members = new ArrayList<>();
  private final LoyaltyRewardsUI ui = new LoyaltyRewardsUI();

  public LoyaltyRewardsService() { load(); }

  public void runLoyaltyRewardsModule() {
    int choice;
    do {
      choice = ui.getMenuChoice();
      switch (choice) {
        case 1: registerMember(); break;
        case 2: viewProfile(); break;
        case 3: addPoints(); break;
        case 4: redeemPoints(); break;
        case 5: showExpiryAlerts(); break;
        case 6: listMembers(); break;
        case 0: MessageUI.displayInfoMessage("Returning to main menu..."); break;
        default: MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }

  private void registerMember() {
    String id = ui.memberId();
    if (find(id) != null) { MessageUI.displayErrorMessage("A member with this ID already exists."); pause(); return; }
    RewardsMember member = new RewardsMember(id, ui.name(), ui.email(), LoyaltyTier.SILVER, 0, LocalDate.now().plusYears(1));
    members.add(member);
    save();
    ui.display("MEMBER REGISTERED", profile(member));
    MessageUI.displaySuccessMessage("Welcome promotion: 5% dining discount for Silver members.");
    pause();
  }

  private void viewProfile() {
    RewardsMember member = find(ui.memberId());
    if (member == null) MessageUI.displayErrorMessage("Member profile was not found.");
    else ui.display("LOYALTY MEMBER PROFILE", profile(member));
    pause();
  }

  private void addPoints() {
    RewardsMember member = find(ui.memberId());
    if (member == null) { MessageUI.displayErrorMessage("Member profile was not found."); pause(); return; }
    LoyaltyTier before = member.getTier();
    member.addPoints(ui.positivePoints("Points to add > "));
    member.setTier(tierFor(member.getPoints()));
    member.setPointsExpiryDate(LocalDate.now().plusYears(1));
    save();
    MessageUI.displaySuccessMessage("Points added. New balance: " + member.getPoints() + ".");
    if (before != member.getTier()) MessageUI.displaySuccessMessage("Tier upgraded: " + before + " -> " + member.getTier() + ". Notification recorded.");
    pause();
  }

  private void redeemPoints() {
    RewardsMember member = find(ui.memberId());
    if (member == null) { MessageUI.displayErrorMessage("Member profile was not found."); pause(); return; }
    int points = ui.positivePoints("Points to redeem > ");
    if (!member.redeemPoints(points)) MessageUI.displayErrorMessage("Insufficient points. Available balance: " + member.getPoints() + ".");
    else { save(); MessageUI.displaySuccessMessage("Redemption request approved for " + points + " points. Remaining balance: " + member.getPoints() + "."); }
    pause();
  }

  private void showExpiryAlerts() {
    LocalDate deadline = LocalDate.now().plusDays(30);
    StringBuilder output = new StringBuilder("Members with points expiring by " + deadline + ":\n\n");
    int count = 0;
    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      RewardsMember member = members.getEntry(i);
      if (member.getPoints() > 0 && !member.getPointsExpiryDate().isAfter(deadline)) {
        output.append(member.getMemberId()).append(" | ").append(member.getName()).append(" | ")
            .append(member.getPoints()).append(" points | expires ").append(member.getPointsExpiryDate()).append('\n');
        count++;
      }
    }
    if (count == 0) output.append("No expiring-points notifications at this time.");
    ui.display("EXPIRING POINTS ALERTS", output.toString());
    pause();
  }

  private void listMembers() {
    StringBuilder output = new StringBuilder(String.format("%-10s %-22s %-12s %-8s %s%n", "Member ID", "Name", "Tier", "Points", "Expires"));
    output.append("--------------------------------------------------------------------------\n");
    for (int i = 1; i <= members.getNumberOfEntries(); i++) {
      RewardsMember member = members.getEntry(i);
      output.append(String.format("%-10s %-22s %-12s %-8d %s%n", member.getMemberId(), member.getName(), member.getTier(), member.getPoints(), member.getPointsExpiryDate()));
    }
    if (members.isEmpty()) output.append("No loyalty members registered.");
    ui.display("LOYALTY MEMBERS", output.toString());
    pause();
  }

  private RewardsMember find(String id) { for (int i = 1; i <= members.getNumberOfEntries(); i++) if (members.getEntry(i).getMemberId().equalsIgnoreCase(id)) return members.getEntry(i); return null; }
  private LoyaltyTier tierFor(int points) { if (points >= 10000) return LoyaltyTier.ELITE; if (points >= 6000) return LoyaltyTier.DIAMOND; if (points >= 3000) return LoyaltyTier.PLATINUM; if (points >= 1000) return LoyaltyTier.GOLD; return LoyaltyTier.SILVER; }
  private String profile(RewardsMember member) { return "Member ID      : " + member.getMemberId() + "\nName           : " + member.getName() + "\nEmail          : " + member.getEmail() + "\nTier           : " + member.getTier() + "\nReward Points  : " + member.getPoints() + "\nPoints Expire  : " + member.getPointsExpiryDate() + "\nPromotion      : " + promotion(member.getTier()); }
  private String promotion(LoyaltyTier tier) { switch (tier) { case ELITE: return "20% suite upgrade offer"; case DIAMOND: return "15% spa and dining offer"; case PLATINUM: return "10% room upgrade offer"; case GOLD: return "8% dining discount"; default: return "5% dining discount"; } }
  private void pause() { MessageUI.pressEnterToContinue(); }

  private void save() {
    try { Files.createDirectories(DATA_FILE.getParent()); try (BufferedWriter writer = Files.newBufferedWriter(DATA_FILE, StandardCharsets.UTF_8)) { writer.write("memberId\tname\temail\ttier\tpoints\tpointsExpiryDate"); writer.newLine(); for (int i = 1; i <= members.getNumberOfEntries(); i++) { RewardsMember m = members.getEntry(i); writer.write(m.getMemberId() + "\t" + clean(m.getName()) + "\t" + clean(m.getEmail()) + "\t" + m.getTier().name() + "\t" + m.getPoints() + "\t" + m.getPointsExpiryDate()); writer.newLine(); } } } catch (IOException ex) { MessageUI.displayErrorMessage("Could not save loyalty data: " + ex.getMessage()); }
  }
  private void load() {
    if (!Files.exists(DATA_FILE)) return;
    try (BufferedReader reader = Files.newBufferedReader(DATA_FILE, StandardCharsets.UTF_8)) { reader.readLine(); String line; while ((line = reader.readLine()) != null) { String[] f = line.split("\\t", -1); if (f.length == 6) members.add(new RewardsMember(f[0], f[1], f[2], LoyaltyTier.valueOf(f[3]), Integer.parseInt(f[4]), LocalDate.parse(f[5]))); } } catch (IOException | IllegalArgumentException ex) { MessageUI.displayErrorMessage("Could not load loyalty data: " + ex.getMessage()); members.clear(); }
  }
  private String clean(String value) { return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '); }
}
