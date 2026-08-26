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
import java.time.LocalDate;
import utility.DataFiles;
import utility.MessageUI;

/**
 * Manages loyalty member profiles, reward points, redemption,
 * tier progression, notifications and loyalty reports.
 *
 * @author
 */
public class LoyaltyRewardsService {

    private static final Path DATA_FILE =
            DataFiles.resolve("loyalty_members.txt");

    private final ListInterface<RewardsMember> members =
            new ArrayList<>();

    private final LoyaltyRewardsUI ui =
            new LoyaltyRewardsUI();

    public LoyaltyRewardsService() {
        load();
    }

     // Returns the registered loyalty profile for use by other resort modules. */
    public RewardsMember getMemberById(String memberId) {
      return find(memberId);
    }

    // MAIN LOYALTY MODULE
    public void runLoyaltyRewardsModule() {

        int choice;

        do {

            choice = ui.getMenuChoice();

            switch (choice) {

                case 1:
                    registerMember();
                    break;

                case 2:
                    viewProfile();
                    break;

                case 3:
                    addPoints();
                    break;

                case 4:
                    redeemPoints();
                    break;

                case 5:
                    showExpiryAlerts();
                    break;

                case 6:
                    listMembers();
                    break;

                case 7:
                    searchMember();
                    break;

                case 8:
                    generateReports();
                    break;

                case 0:
                    MessageUI.displayInfoMessage(
                            "Returning to main menu..."
                    );
                    break;

                default:
                    MessageUI.displayInvalidChoiceMessage();
            }

        } while (choice != 0);
    }

    // 1. REGISTER MEMBER
    private void registerMember() {

        String id = ui.memberId();

        if (find(id) != null) {

            MessageUI.displayErrorMessage(
                    "A member with this ID already exists."
            );

            pause();
            return;
        }

        String name = ui.name();
        String email = ui.email();

        RewardsMember member = new RewardsMember(
                id,
                name,
                email,
                LoyaltyTier.SILVER,
                0,
                LocalDate.now().plusYears(1)
        );

        members.add(member);

        save();

        ui.display(
                "MEMBER REGISTERED",
                profile(member)
        );

        MessageUI.displaySuccessMessage(
                "Welcome promotion: 5% dining discount for Silver members."
        );

        pause();
    }

    // 2. VIEW MEMBER PROFILE
    private void viewProfile() {

        RewardsMember member =
                find(ui.memberId());

        if (member == null) {

            MessageUI.displayErrorMessage(
                    "Member profile was not found."
            );

        } else {

            ui.display(
                    "LOYALTY MEMBER PROFILE",
                    profile(member)
            );
        }

        pause();
    }

    // 3. ADD REWARD POINTS
    private void addPoints() {

        RewardsMember member =
                find(ui.memberId());

        if (member == null) {

            MessageUI.displayErrorMessage(
                    "Member profile was not found."
            );

            pause();
            return;
        }

        LoyaltyTier before =
                member.getTier();

        int points =
                ui.positivePoints("Points to add > ");

        member.addPoints(points);

        LoyaltyTier newTier =
                tierFor(member.getPoints());

        member.setTier(newTier);

        /*
         * Extend the points expiry date when
         * new points are accumulated.
         */
        member.setPointsExpiryDate(
                LocalDate.now().plusYears(1)
        );

        save();

        MessageUI.displaySuccessMessage(
                points + " reward points added successfully."
        );

        MessageUI.displaySuccessMessage(
                "New balance: "
                + member.getPoints()
                + " points."
        );

        if (before != member.getTier()) {

            MessageUI.displaySuccessMessage(
                    "Tier upgraded: "
                    + before
                    + " -> "
                    + member.getTier()
                    + "."
            );

            MessageUI.displayInfoMessage(
                    "Tier upgrade notification recorded."
            );
        }

        pause();
    }

    // 4. REDEEM POINTS
    private void redeemPoints() {

        RewardsMember member =
                find(ui.memberId());

        if (member == null) {

            MessageUI.displayErrorMessage(
                    "Member profile was not found."
            );

            pause();
            return;
        }

        int points =
                ui.positivePoints("Points to redeem > ");

        if (!member.redeemPoints(points)) {

            MessageUI.displayErrorMessage(
                    "Insufficient points. Available balance: "
                    + member.getPoints()
                    + "."
            );

          }else {

          LoyaltyTier newTier = tierFor(member.getPoints());
          member.setTier(newTier);

          save();

          MessageUI.displaySuccessMessage(
                  "Redemption request approved."
          );

          MessageUI.displayInfoMessage(
                  "Points redeemed: " + points
          );

          MessageUI.displayInfoMessage(
                  "Remaining balance: " + member.getPoints()
          );

          MessageUI.displayInfoMessage(
                  "Current tier: " + member.getTier()
          );
      }

        pause();
    }

    // 5. EXPIRING POINTS ALERT
    private void showExpiryAlerts() {

        LocalDate today =
                LocalDate.now();

        LocalDate deadline =
                today.plusDays(30);

        StringBuilder output =
                new StringBuilder();

        output.append(
                "Current Date : "
        ).append(today).append("\n");

        output.append(
                "Alert Period : Within 30 days\n\n"
        );

        output.append(
                String.format(
                        "%-10s %-22s %-12s %-15s%n",
                        "Member ID",
                        "Name",
                        "Points",
                        "Expiry Date"
                )
        );

        output.append(
                "--------------------------------------------------------------\n"
        );

        int count = 0;

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

            RewardsMember member =
                    members.getEntry(i);

            LocalDate expiry =
                    member.getPointsExpiryDate();

            if (member.getPoints() > 0
                    && expiry != null
                    && !expiry.isBefore(today)
                    && !expiry.isAfter(deadline)) {

                output.append(
                        String.format(
                                "%-10s %-22s %-12d %-15s%n",
                                member.getMemberId(),
                                member.getName(),
                                member.getPoints(),
                                expiry
                        )
                );

                count++;
            }
        }

        if (count == 0) {

            output.append(
                    "No expiring-points notifications at this time.\n"
            );
        }

        output.append(
                "\nTotal members requiring notification: "
        ).append(count);

        ui.display(
                "EXPIRING POINTS ALERTS",
                output.toString()
        );

        pause();
    }

    // 6. LIST ALL MEMBERS
    private void listMembers() {

        StringBuilder output =
                new StringBuilder();

        output.append(
                String.format(
                        "%-10s %-22s %-12s %-10s %-15s%n",
                        "Member ID",
                        "Name",
                        "Tier",
                        "Points",
                        "Expires"
                )
        );

        output.append(
                "-----------------------------------------------------------------------\n"
        );

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

            RewardsMember member =
                    members.getEntry(i);

            output.append(
                    String.format(
                            "%-10s %-22s %-12s %-10d %-15s%n",
                            member.getMemberId(),
                            member.getName(),
                            member.getTier(),
                            member.getPoints(),
                            member.getPointsExpiryDate()
                    )
            );
        }

        if (members.isEmpty()) {

            output.append(
                    "No loyalty members registered.\n"
            );
        }

        output.append(
                "\nTotal Members: "
        ).append(
                members.getNumberOfEntries()
        );

        ui.display(
                "LOYALTY MEMBERS",
                output.toString()
        );

        pause();
    }

    // 7. SEARCH MEMBER
    private void searchMember() {

        String keyword = ui.searchKeyword().toLowerCase();

        StringBuilder output = new StringBuilder();

        output.append(
                String.format(
                        "%-10s %-22s %-12s %-10s %-15s%n",
                        "Member ID",
                        "Name",
                        "Tier",
                        "Points",
                        "Expires"
                )
        );

        output.append(
                "-----------------------------------------------------------------------\n"
        );

        int count = 0;

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

            RewardsMember member =
                    members.getEntry(i);

            String memberId =
                    member.getMemberId().toLowerCase();

            String name =
                    member.getName().toLowerCase();

            if (memberId.contains(keyword)
                    || name.contains(keyword)) {

                output.append(
                        String.format(
                                "%-10s %-22s %-12s %-10d %-15s%n",
                                member.getMemberId(),
                                member.getName(),
                                member.getTier(),
                                member.getPoints(),
                                member.getPointsExpiryDate()
                        )
                );

                count++;
            }
        }

        if (count == 0) {

            MessageUI.displayErrorMessage(
                    "No member was found matching: "
                    + keyword
            );

        } else {

            output.append(
                    "\nTotal Members Found: "
            ).append(count);

            ui.display(
                    "MEMBER SEARCH RESULTS",
                    output.toString()
            );
        }

        pause();
    }

    // 8. GENERATE REPORTS
    private void generateReports() {

        int choice;

        do {

            choice =
                    ui.getReportChoice();

            switch (choice) {

                case 1:
                    generateTierAndPointsReport();
                    break;

                case 2:
                    generateExpiryRiskReport();
                    break;

                case 0:
                    break;

                default:
                    MessageUI.displayInvalidChoiceMessage();
            }

        } while (choice != 0);
    }

    // REPORT 1: TIER AND POINTS ANALYSIS
    private void generateTierAndPointsReport() {

        if (members.isEmpty()) {

            MessageUI.displayErrorMessage(
                    "No loyalty members available for reporting."
            );

            pause();
            return;
        }

        String selectedTier =
                ui.tierFilter();

        int minimumPoints =
                ui.minimumPoints();

        int maximumPoints =
                ui.maximumPoints(minimumPoints);

        int sortChoice =
                ui.getSortChoice();

        /*
         * Temporary ArrayList stores members
         * matching the report criteria.
         */
        ListInterface<RewardsMember> filteredMembers =
                new ArrayList<>();

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

            RewardsMember member =
                    members.getEntry(i);

            boolean tierMatch =
                    selectedTier.equals("ALL")
                    || member.getTier()
                            .name()
                            .equals(selectedTier);

            boolean pointsMatch =
                    member.getPoints() >= minimumPoints
                    && member.getPoints() <= maximumPoints;

            if (tierMatch && pointsMatch) {

                filteredMembers.add(member);
            }
        }

        sortMembers(
                filteredMembers,
                sortChoice
        );

        StringBuilder output =
                new StringBuilder();

        output.append(
                "Report Criteria\n"
        );

        output.append(
                "----------------------------------------\n"
        );

        output.append(
                "Tier Filter       : "
        ).append(selectedTier).append("\n");

        output.append(
                "Minimum Points    : "
        ).append(minimumPoints).append("\n");

        output.append(
                "Maximum Points    : "
        ).append(maximumPoints).append("\n");

        output.append(
                "Members Found     : "
        ).append(
                filteredMembers.getNumberOfEntries()
        ).append("\n\n");

        // TIER DISTRIBUTION
        int silver = 0;
        int gold = 0;
        int platinum = 0;
        int diamond = 0;
        int elite = 0;

        int totalPoints = 0;

        for (int i = 1;
                i <= filteredMembers.getNumberOfEntries();
                i++) {

            RewardsMember member =
                    filteredMembers.getEntry(i);

            totalPoints +=
                    member.getPoints();

            switch (member.getTier()) {

                case SILVER:
                    silver++;
                    break;

                case GOLD:
                    gold++;
                    break;

                case PLATINUM:
                    platinum++;
                    break;

                case DIAMOND:
                    diamond++;
                    break;

                case ELITE:
                    elite++;
                    break;
            }
        }

        double averagePoints = 0;

        if (filteredMembers.getNumberOfEntries() > 0) {

            averagePoints =
                    (double) totalPoints
                    / filteredMembers.getNumberOfEntries();
        }

        output.append(
                "TIER DISTRIBUTION\n"
        );

        output.append(
                "----------------------------------------\n"
        );

        output.append(
                String.format(
                        "SILVER     : %d%n",
                        silver
                )
        );

        output.append(
                String.format(
                        "GOLD       : %d%n",
                        gold
                )
        );

        output.append(
                String.format(
                        "PLATINUM   : %d%n",
                        platinum
                )
        );

        output.append(
                String.format(
                        "DIAMOND    : %d%n",
                        diamond
                )
        );

        output.append(
                String.format(
                        "ELITE      : %d%n",
                        elite
                )
        );

        output.append("\n");

        // POINT SUMMARY
        output.append(
                "POINT SUMMARY\n"
        );

        output.append(
                "----------------------------------------\n"
        );

        output.append(
                "Total Points   : "
        ).append(totalPoints).append("\n");

        output.append(
                String.format(
                        "Average Points : %.2f%n",
                        averagePoints
                )
        );

        output.append("\n");

        // FILTERED MEMBER LIST
        output.append(
                "FILTERED MEMBERS\n"
        );

        output.append(
                "-------------------------------------------------------------------------------\n"
        );

        output.append(
                String.format(
                        "%-10s %-22s %-12s %-10s%n",
                        "ID",
                        "Name",
                        "Tier",
                        "Points"
                )
        );

        output.append(
                "-------------------------------------------------------------------------------\n"
        );

        if (filteredMembers.isEmpty()) {

            output.append(
                    "No members match the selected criteria.\n"
            );

        } else {

            for (int i = 1;
                    i <= filteredMembers.getNumberOfEntries();
                    i++) {

                RewardsMember member =
                        filteredMembers.getEntry(i);

                output.append(
                        String.format(
                                "%-10s %-22s %-12s %-10d%n",
                                member.getMemberId(),
                                member.getName(),
                                member.getTier(),
                                member.getPoints()
                        )
                );
            }
        }

        ui.display(
                "TIER AND POINTS ANALYSIS",
                output.toString()
        );

        pause();
    }

    // REPORT 2: EXPIRY AND RISK REPORT
    private void generateExpiryRiskReport() {

        if (members.isEmpty()) {

            MessageUI.displayErrorMessage(
                    "No loyalty members available for reporting."
            );

            pause();
            return;
        }

        LocalDate today =
                LocalDate.now();

        LocalDate deadline =
                today.plusDays(30);

        int expiryCount = 0;
        int highBalanceCount = 0;

        int expiryPoints = 0;
        int totalPoints = 0;

        StringBuilder expiringMembers =
                new StringBuilder();

        StringBuilder highBalanceMembers =
                new StringBuilder();

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

            RewardsMember member =
                    members.getEntry(i);

            totalPoints +=
                    member.getPoints();

            // EXPIRY RISK
            LocalDate expiry =
                    member.getPointsExpiryDate();

            if (member.getPoints() > 0
                    && expiry != null
                    && !expiry.isBefore(today)
                    && !expiry.isAfter(deadline)) {

                expiryCount++;

                expiryPoints +=
                        member.getPoints();

                expiringMembers.append(
                        String.format(
                                "%-10s %-22s %-10d %-15s%n",
                                member.getMemberId(),
                                member.getName(),
                                member.getPoints(),
                                expiry
                        )
                );
            }

            // HIGH BALANCE RISK
            if (member.getPoints() >= 5000) {

                highBalanceCount++;

                highBalanceMembers.append(
                        String.format(
                                "%-10s %-22s %-12s %-10d%n",
                                member.getMemberId(),
                                member.getName(),
                                member.getTier(),
                                member.getPoints()
                        )
                );
            }
        }

        StringBuilder output =
                new StringBuilder();

        // REPORT DATE
        output.append(
                "REPORT DATE\n"
        );

        output.append(
                "----------------------------------------\n"
        );

        output.append(
                "Current Date       : "
        ).append(today).append("\n");

        output.append(
                "Expiry Alert Date  : "
        ).append(deadline).append("\n\n");

        // EXPIRING POINTS
        output.append(
                "EXPIRING POINTS\n"
        );

        output.append(
                "-------------------------------------------------------------------------------\n"
        );

        output.append(
                String.format(
                        "%-10s %-22s %-10s %-15s%n",
                        "ID",
                        "Name",
                        "Points",
                        "Expiry Date"
                )
        );

        output.append(
                "-------------------------------------------------------------------------------\n"
        );

        if (expiryCount == 0) {

            output.append(
                    "No members have points expiring within 30 days.\n"
            );

        } else {

            output.append(
                    expiringMembers
            );
        }

        output.append("\n");

        output.append(
                "Expiry Risk Summary\n"
        );

        output.append(
                "----------------------------------------\n"
        );

        output.append(
                "Members at Risk : "
        ).append(expiryCount).append("\n");

        output.append(
                "Points at Risk  : "
        ).append(expiryPoints).append("\n\n");

        // HIGH POINT BALANCE
        output.append(
                "HIGH POINT BALANCE MEMBERS\n"
        );

        output.append(
                "-------------------------------------------------------------------------------\n"
        );

        output.append(
                String.format(
                        "%-10s %-22s %-12s %-10s%n",
                        "ID",
                        "Name",
                        "Tier",
                        "Points"
                )
        );

        output.append(
                "-------------------------------------------------------------------------------\n"
        );

        if (highBalanceCount == 0) {

            output.append(
                    "No members have 5,000 or more points.\n"
            );

        } else {

            output.append(
                    highBalanceMembers
            );
        }

        output.append("\n");

        // OVERALL SUMMARY
        output.append(
                "OVERALL SUMMARY\n"
        );

        output.append(
                "----------------------------------------\n"
        );

        output.append(
                "Total Members        : "
        ).append(
                members.getNumberOfEntries()
        ).append("\n");

        output.append(
                "Total Reward Points  : "
        ).append(totalPoints).append("\n");

        output.append(
                "Expiry Risk Members  : "
        ).append(expiryCount).append("\n");

        output.append(
                "High Balance Members : "
        ).append(highBalanceCount).append("\n");

        ui.display(
                "POINTS EXPIRY AND RISK REPORT",
                output.toString()
        );

        pause();
    }

    // SORT MEMBERS
    private void sortMembers(
            ListInterface<RewardsMember> list,
            int sortChoice) {

        int size =
                list.getNumberOfEntries();

        /*
         * Bubble sort is used for sorting the
         * temporary ADT-based list.
         */
        for (int i = 1;
                i <= size - 1;
                i++) {

            for (int j = 1;
                    j <= size - i;
                    j++) {

                RewardsMember current =
                        list.getEntry(j);

                RewardsMember next =
                        list.getEntry(j + 1);

                boolean shouldSwap = false;

                switch (sortChoice) {

                    case 1:

                        // Points ascending
                        if (current.getPoints()
                                > next.getPoints()) {

                            shouldSwap = true;
                        }

                        break;

                    case 2:

                        // Points descending
                        if (current.getPoints()
                                < next.getPoints()) {

                            shouldSwap = true;
                        }

                        break;

                    case 3:

                        // Name ascending
                        if (current.getName()
                                .compareToIgnoreCase(
                                        next.getName()
                                ) > 0) {

                            shouldSwap = true;
                        }

                        break;

                    case 4:

                        // Tier priority descending
                        if (current.getTier()
                                .getPriority()
                                < next.getTier()
                                        .getPriority()) {

                            shouldSwap = true;
                        }

                        break;

                    default:
                        break;
                }

                if (shouldSwap) {

                    list.replace(
                            j,
                            next
                    );

                    list.replace(
                            j + 1,
                            current
                    );
                }
            }
        }
    }

    // FIND MEMBER
    private RewardsMember find(String id) {

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

            RewardsMember member =
                    members.getEntry(i);

            if (member.getMemberId()
                    .equalsIgnoreCase(id)) {

                return member;
            }
        }

        return null;
    }

    // DETERMINE LOYALTY TIER
    private LoyaltyTier tierFor(int points) {

        if (points >= 10000) {
            return LoyaltyTier.ELITE;
        }

        if (points >= 6000) {
            return LoyaltyTier.DIAMOND;
        }

        if (points >= 3000) {
            return LoyaltyTier.PLATINUM;
        }

        if (points >= 1000) {
            return LoyaltyTier.GOLD;
        }

        return LoyaltyTier.SILVER;
    }

    // MEMBER PROFIL
    private String profile(
            RewardsMember member) {

        return
                "Member ID      : "
                + member.getMemberId()

                + "\nName           : "
                + member.getName()

                + "\nEmail          : "
                + member.getEmail()

                + "\nTier           : "
                + member.getTier()

                + "\nReward Points  : "
                + member.getPoints()

                + "\nPoints Expire  : "
                + member.getPointsExpiryDate()

                + "\nPromotion      : "
                + promotion(member.getTier());
    }

    // PERSONALIZED PROMOTION
    private String promotion(
            LoyaltyTier tier) {

        switch (tier) {

            case ELITE:
                return "20% suite upgrade offer";

            case DIAMOND:
                return "15% spa and dining offer";

            case PLATINUM:
                return "10% room upgrade offer";

            case GOLD:
                return "8% dining discount";

            default:
                return "5% dining discount";
        }
    }

    // PAUSE
    private void pause() {

        MessageUI.pressEnterToContinue();
    }

    // SAVE DATA
    private void save() {

        try {

            Files.createDirectories(
                    DATA_FILE.getParent()
            );

            try (BufferedWriter writer =
                    Files.newBufferedWriter(
                            DATA_FILE,
                            StandardCharsets.UTF_8
                    )) {

                writer.write(
                        "memberId\tname\temail\ttier\tpoints\tpointsExpiryDate"
                );

                writer.newLine();

                for (int i = 1;
                        i <= members.getNumberOfEntries();
                        i++) {

                    RewardsMember member =
                            members.getEntry(i);

                    writer.write(
                            member.getMemberId()
                            + "\t"
                            + clean(member.getName())
                            + "\t"
                            + clean(member.getEmail())
                            + "\t"
                            + member.getTier().name()
                            + "\t"
                            + member.getPoints()
                            + "\t"
                            + member.getPointsExpiryDate()
                    );

                    writer.newLine();
                }
            }

        } catch (IOException ex) {

            MessageUI.displayErrorMessage(
                    "Could not save loyalty data: "
                    + ex.getMessage()
            );
        }
    }

    // LOAD DATA
    private void load() {

        if (!Files.exists(DATA_FILE)) {
            return;
        }

        try (BufferedReader reader =
                Files.newBufferedReader(
                        DATA_FILE,
                        StandardCharsets.UTF_8
                )) {

            // Skip header
            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] fields =
                        line.split("\\t", -1);

                if (fields.length == 6) {

                    RewardsMember member =
                            new RewardsMember(
                                    fields[0],
                                    fields[1],
                                    fields[2],
                                    LoyaltyTier.valueOf(fields[3]),
                                    Integer.parseInt(fields[4]),
                                    LocalDate.parse(fields[5])
                            );

                    members.add(member);
                }
            }

        } catch (
                IOException
                | IllegalArgumentException ex) {

            MessageUI.displayErrorMessage(
                    "Could not load loyalty data: "
                    + ex.getMessage()
            );

            members.clear();
        }
    }

    // CLEAN TEXT BEFORE SAVING
    private String clean(String value) {

        return value
                .replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
    }
}
