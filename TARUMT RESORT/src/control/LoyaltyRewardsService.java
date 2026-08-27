package control;

import adt.ArrayList;
import adt.ListInterface;
import boundary.LoyaltyRewardsUI;
import entity.LoyaltyTier;
import entity.RewardsMember;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import utility.DataFiles;
import utility.MessageUI;
import utility.MalaysiaTime;
import utility.PdfReportEngine;

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
                    editMember();
                    break;

                case 6:
                    deleteMember();
                    break;

                case 7:
                    showExpiryAlerts();
                    break;

                case 8:
                    listMembers();
                    break;

                case 9:
                    searchMember();
                    break;

                case 10:
                    generateTierAndPointsReport();
                    break;

                case 11:
                    generateExpiryRiskReport();
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

        String id = nextMemberId();

        String name = ui.name();
        if (name == null) {
            MessageUI.displayInfoMessage("Registration cancelled.");
            pause();
            return;
        }

        String email = ui.email();
        if (email == null) {
            MessageUI.displayInfoMessage("Registration cancelled.");
            pause();
            return;
        }

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

        RewardsMember member = null;
        while (member == null) {
            String id = ui.memberId();
            if (id == null) {
                MessageUI.displayInfoMessage("Cancelled.");
                pause();
                return;
            }
            member = find(id);
            if (member == null) {
                MessageUI.displayErrorMessage(
                        "Member \"" + id + "\" not found. Try again or enter 0 to cancel."
                );
            }
        }

        ui.display(
                "LOYALTY MEMBER PROFILE",
                profile(member)
        );

        pause();
    }

    // 3. ADD REWARD POINTS
    private void addPoints() {

        RewardsMember member = null;
        while (member == null) {
            String id = ui.memberId();
            if (id == null) {
                MessageUI.displayInfoMessage("Cancelled.");
                pause();
                return;
            }
            member = find(id);
            if (member == null) {
                MessageUI.displayErrorMessage(
                        "Member \"" + id + "\" not found. Try again or enter 0 to cancel."
                );
            }
        }

        LoyaltyTier before = member.getTier();

        int points = ui.positivePoints("Points to add");
        if (points == -1) {
            MessageUI.displayInfoMessage("Cancelled.");
            pause();
            return;
        }

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

        RewardsMember member = null;
        while (member == null) {
            String id = ui.memberId();
            if (id == null) {
                MessageUI.displayInfoMessage("Cancelled.");
                pause();
                return;
            }
            member = find(id);
            if (member == null) {
                MessageUI.displayErrorMessage(
                        "Member \"" + id + "\" not found. Try again or enter 0 to cancel."
                );
            }
        }

        int points = ui.positivePoints("Points to redeem");
        if (points == -1) {
            MessageUI.displayInfoMessage("Cancelled.");
            pause();
            return;
        }

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

        // Sort main list by member ID ascending before iterating
        sortByMemberId(members);

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

        // Sort by member ID ascending before displaying
        sortByMemberId(members);

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

        String rawKeyword = ui.searchKeyword();
        if (rawKeyword == null) {
            MessageUI.displayInfoMessage("Cancelled.");
            pause();
            return;
        }

        String keyword = rawKeyword.toLowerCase();

        /*
        * Temporary Linear List ADT used to store
        * all members matching the search keyword.
        *
        * Partial matching is supported for both
        * Member ID and Member Name.
        *
        * Example:
        * Searching "xuan" can find:
        * Huixuan
        * Rouxuan
        * Xuanxuan
        */
        ListInterface<RewardsMember> searchResults =
                new ArrayList<>();

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

                RewardsMember member =
                        members.getEntry(i);

                String memberId =
                        member.getMemberId().toLowerCase();

                String memberName =
                        member.getName().toLowerCase();

                /*
                * contains() allows partial keyword searching.
                */
                if (memberId.contains(keyword)
                        || memberName.contains(keyword)) {

                searchResults.add(member);
                }
        }

        // ---------------------------------------------------------
        // No matching member
        // ---------------------------------------------------------

        if (searchResults.isEmpty()) {

                MessageUI.displayErrorMessage(
                        "No members found matching: "
                        + keyword
                );

                pause();
                return;
        }

        // ---------------------------------------------------------
        // Display search results
        // ---------------------------------------------------------

        // Sort search results by member ID ascending
        sortByMemberId(searchResults);

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
                i <= searchResults.getNumberOfEntries();
                i++) {

                RewardsMember member =
                        searchResults.getEntry(i);

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

        output.append(
                "\nSearch keyword : "
        ).append(keyword);

        output.append(
                "\nMembers found  : "
        ).append(
                searchResults.getNumberOfEntries()
        );

        ui.display(
                "MEMBER SEARCH RESULTS",
                output.toString()
        );

        pause();
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

        // Sort master list by member ID before filtering so filtered list
        // inherits ID-ascending order as its base sequence.
        sortByMemberId(members);

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

        if (ui.confirmPdfExport()) {
            exportReport1ToPdf(
                    filteredMembers,
                    selectedTier,
                    minimumPoints,
                    maximumPoints,
                    silver, gold, platinum, diamond, elite,
                    totalPoints, averagePoints
            );
        }

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

        // Sort by member ID ascending before building report rows
        sortByMemberId(members);

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

        if (ui.confirmPdfExport()) {
            exportReport2ToPdf(
                    today,
                    deadline,
                    expiryCount,
                    expiryPoints,
                    highBalanceCount,
                    totalPoints,
                    expiringMembers.toString(),
                    highBalanceMembers.toString()
            );
        }

        pause();
    }

    // EXPORT REPORT 1 TO PDF  (Tier and Points Analysis)
    private void exportReport1ToPdf(
            ListInterface<RewardsMember> filtered,
            String tierFilter,
            int minPts,
            int maxPts,
            int silver, int gold, int platinum, int diamond, int elite,
            int totalPoints,
            double avgPoints) {

        PdfReportEngine pdf = null;

        try {

            new File("output/pdf").mkdirs();

            String timestamp = MalaysiaTime.now()
                    .format(java.time.format.DateTimeFormatter
                            .ofPattern("yyyyMMdd_HHmmss"));

            String outPath = "output/pdf/loyalty_tier_points_"
                    + timestamp + ".pdf";

            pdf = new PdfReportEngine();

            // ── Cover page ───────────────────────────────────────────────
            pdf.addCoverPage(
                    "TIER AND POINTS ANALYSIS",
                    "Loyalty Members — Filtered Report",
                    LocalDate.now().toString(),
                    "Loyalty & Rewards Service"
            );

            // ── Page 1: KPI overview ─────────────────────────────────────
            pdf.beginContentPage();

            pdf.addSectionHeading("Report Criteria");
            pdf.addKpiRow("Tier Filter",    tierFilter,           PdfReportEngine.TEXT_DARK);
            pdf.addKpiRow("Min Points",     String.valueOf(minPts), PdfReportEngine.TEXT_DARK);
            pdf.addKpiRow("Max Points",     String.valueOf(maxPts), PdfReportEngine.TEXT_DARK);
            pdf.addKpiRow("Members Found",
                    String.valueOf(filtered.getNumberOfEntries()),
                    PdfReportEngine.ACCENT_BLUE);
            pdf.addDivider();

            // KPI cards: Silver / Gold / Platinum / Diamond / Elite
            pdf.addSectionHeading("Tier Distribution");
            pdf.addKpiCards(
                    new String[]{"SILVER", "GOLD", "PLATINUM", "DIAMOND", "ELITE"},
                    new String[]{
                        String.valueOf(silver),
                        String.valueOf(gold),
                        String.valueOf(platinum),
                        String.valueOf(diamond),
                        String.valueOf(elite)
                    },
                    new Color[]{
                        PdfReportEngine.MID_GREY,
                        PdfReportEngine.BRAND_GOLD,
                        PdfReportEngine.BRAND_TEAL,
                        PdfReportEngine.ACCENT_BLUE,
                        PdfReportEngine.BRAND_NAVY
                    }
            );
            pdf.addDivider();

            // Points summary KPIs
            pdf.addSectionHeading("Points Summary");
            pdf.addKpiRow("Total Points",
                    String.valueOf(totalPoints),   PdfReportEngine.BRAND_TEAL);
            pdf.addKpiRow("Average Points",
                    String.format("%.2f", avgPoints), PdfReportEngine.BRAND_TEAL);
            pdf.addDivider();

            // Bar chart: members per tier
            pdf.addSectionHeading("Members per Tier — Bar Chart");
            pdf.addBarChart(
                    "Members by Loyalty Tier",
                    new String[]{"SILVER", "GOLD", "PLATINUM", "DIAMOND", "ELITE"},
                    new double[]{silver, gold, platinum, diamond, elite},
                    "Number of Members"
            );

            // ── Page 2: Member table ─────────────────────────────────────
            pdf.beginContentPage();
            pdf.addSectionHeading("Filtered Member List");

            String[] headers = {"Member ID", "Name", "Tier", "Points"};
            float[] colWidths = {80f, 160f, 90f, 80f};

            List<String[]> rows = new java.util.ArrayList<>();

            for (int i = 1; i <= filtered.getNumberOfEntries(); i++) {

                RewardsMember m = filtered.getEntry(i);

                rows.add(new String[]{
                    m.getMemberId(),
                    m.getName(),
                    m.getTier().name(),
                    String.valueOf(m.getPoints())
                });
            }

            pdf.addTable(headers, rows, colWidths);

            // Save
            pdf.save(outPath);
            ui.displayPdfExportSuccess(outPath);

        } catch (IOException ex) {

            MessageUI.displayErrorMessage(
                    "PDF export failed: " + ex.getMessage()
            );

        } finally {

            if (pdf != null) {
                try { pdf.close(); } catch (IOException ignored) {}
            }
        }
    }

    // EXPORT REPORT 2 TO PDF  (Points Expiry and Risk Report)
    private void exportReport2ToPdf(
            LocalDate today,
            LocalDate deadline,
            int expiryCount,
            int expiryPoints,
            int highBalanceCount,
            int totalPoints,
            String expiringRows,
            String highBalanceRows) {

        PdfReportEngine pdf = null;

        try {

            new File("output/pdf").mkdirs();

            String timestamp = MalaysiaTime.now()
                    .format(java.time.format.DateTimeFormatter
                            .ofPattern("yyyyMMdd_HHmmss"));

            String outPath = "output/pdf/loyalty_expiry_risk_"
                    + timestamp + ".pdf";

            pdf = new PdfReportEngine();

            // ── Cover page ───────────────────────────────────────────────
            pdf.addCoverPage(
                    "POINTS EXPIRY AND RISK REPORT",
                    "Expiry Alerts & High-Balance Members",
                    today + " to " + deadline,
                    "Loyalty & Rewards Service"
            );

            // ── Page 1: KPI overview ─────────────────────────────────────
            pdf.beginContentPage();

            pdf.addSectionHeading("Report Period");
            pdf.addKpiRow("Current Date",      today.toString(),    PdfReportEngine.TEXT_DARK);
            pdf.addKpiRow("Expiry Alert Date", deadline.toString(), PdfReportEngine.TEXT_DARK);
            pdf.addDivider();

            pdf.addSectionHeading("Risk Summary");
            pdf.addKpiCards(
                    new String[]{
                        "TOTAL MEMBERS", "TOTAL POINTS",
                        "EXPIRY RISK",   "HIGH BALANCE"
                    },
                    new String[]{
                        String.valueOf(members.getNumberOfEntries()),
                        String.valueOf(totalPoints),
                        String.valueOf(expiryCount),
                        String.valueOf(highBalanceCount)
                    },
                    new Color[]{
                        PdfReportEngine.ACCENT_BLUE,
                        PdfReportEngine.BRAND_TEAL,
                        PdfReportEngine.WARNING,
                        PdfReportEngine.DANGER
                    }
            );
            pdf.addDivider();

            // Donut chart: expiry risk vs safe
            int safeCount = members.getNumberOfEntries() - expiryCount;
            pdf.addSectionHeading("Expiry Risk Distribution");
            pdf.addDonutChart(
                    "Members at Expiry Risk vs Safe",
                    new String[]{"At Risk", "Safe"},
                    new double[]{expiryCount, Math.max(0, safeCount)}
            );

            // ── Page 2: Expiring members table ───────────────────────────
            pdf.beginContentPage();
            pdf.addSectionHeading("Members with Expiring Points (within 30 days)");

            String[] expiryHeaders = {"Member ID", "Name", "Points", "Expiry Date"};
            float[] expiryWidths   = {80f, 160f, 80f, 100f};
            List<String[]> expiryTableRows = new java.util.ArrayList<>();

            if (expiryCount == 0) {
                expiryTableRows.add(new String[]{
                    "—", "No members have points expiring within 30 days.", "", ""
                });
            } else {
                for (String line : expiringRows.split("\r?\n")) {
                    if (line.trim().isEmpty()) continue;
                    // format: %-10s %-22s %-10d %-15s
                    String[] parts = line.trim().split("\\s{2,}");
                    if (parts.length >= 4) {
                        expiryTableRows.add(new String[]{
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim()
                        });
                    } else {
                        expiryTableRows.add(new String[]{line.trim(), "", "", ""});
                    }
                }
            }

            pdf.addTable(expiryHeaders, expiryTableRows, expiryWidths);
            pdf.addDivider();

            pdf.addSectionHeading("High Point Balance Members (5,000+ points)");

            String[] highHeaders = {"Member ID", "Name", "Tier", "Points"};
            float[] highWidths   = {80f, 160f, 90f, 80f};
            List<String[]> highTableRows = new java.util.ArrayList<>();

            if (highBalanceCount == 0) {
                highTableRows.add(new String[]{
                    "—", "No members have 5,000 or more points.", "", ""
                });
            } else {
                for (String line : highBalanceRows.split("\r?\n")) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.trim().split("\\s{2,}");
                    if (parts.length >= 4) {
                        highTableRows.add(new String[]{
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim()
                        });
                    } else {
                        highTableRows.add(new String[]{line.trim(), "", "", ""});
                    }
                }
            }

            pdf.addTable(highHeaders, highTableRows, highWidths);
            pdf.addDivider();

            pdf.addSectionHeading("Management Recommendations");
            pdf.addBodyText(
                    "Contact members with expiring points and encourage redemption before expiry.",
                    10
            );
            pdf.addBodyText(
                    "Consider targeted promotions for high-balance members to drive engagement.",
                    10
            );
            pdf.addBodyText(
                    "Review tier thresholds periodically to retain Diamond and Elite members.",
                    10
            );

            // Save
            pdf.save(outPath);
            ui.displayPdfExportSuccess(outPath);

        } catch (IOException ex) {

            MessageUI.displayErrorMessage(
                    "PDF export failed: " + ex.getMessage()
            );

        } finally {

            if (pdf != null) {
                try { pdf.close(); } catch (IOException ignored) {}
            }
        }
    }

    // 5. EDIT MEMBER
    private void editMember() {

        // Step 1 — keep asking until member is found or user cancels
        RewardsMember member = null;
        int position = -1;
        while (member == null) {
            String id = ui.memberId();
            if (id == null) {
                MessageUI.displayInfoMessage("Cancelled.");
                pause();
                return;
            }
            for (int i = 1; i <= members.getNumberOfEntries(); i++) {
                if (members.getEntry(i).getMemberId().equalsIgnoreCase(id)) {
                    position = i;
                    member   = members.getEntry(i);
                    break;
                }
            }
            if (member == null) {
                MessageUI.displayErrorMessage(
                        "Member \"" + id + "\" not found. Try again or enter 0 to cancel."
                );
            }
        }

        // Show current profile before editing
        ui.display(
                "CURRENT MEMBER PROFILE",
                profile(member)
        );

        // Step 2 — pick which field to modify (0 = cancel)
        int fieldChoice = ui.inputEditChoice();

        if (fieldChoice == 0) {
            MessageUI.displayInfoMessage("Edit cancelled.");
            pause();
            return;
        }

        // Step 3 — modify the object in place
        if (fieldChoice == 1) {

            String oldName = member.getName();
            String newName = ui.name();
            if (newName == null) {
                MessageUI.displayInfoMessage("Edit cancelled.");
                pause();
                return;
            }

            member.setName(newName);
            save();

            ui.displayEditResult(
                    member.getMemberId(), "Name", oldName, newName
            );

            MessageUI.displaySuccessMessage(
                    "Member name updated successfully."
            );

        } else if (fieldChoice == 2) {

            String oldEmail = member.getEmail();
            String newEmail = ui.email();
            if (newEmail == null) {
                MessageUI.displayInfoMessage("Edit cancelled.");
                pause();
                return;
            }

            member.setEmail(newEmail);
            save();

            ui.displayEditResult(
                    member.getMemberId(), "Email", oldEmail, newEmail
            );

            MessageUI.displaySuccessMessage(
                    "Member email updated successfully."
            );

        } else {

            // fieldChoice == 3: edit points, recalculate tier
            int oldPoints = member.getPoints();
            int newPoints = ui.inputNewPoints(oldPoints);
            if (newPoints == -1) {
                MessageUI.displayInfoMessage("Edit cancelled.");
                pause();
                return;
            }

            member.setPoints(newPoints);

            LoyaltyTier newTier = tierFor(newPoints);
            member.setTier(newTier);

            save();

            ui.displayEditResult(
                    member.getMemberId(),
                    "Points",
                    String.valueOf(oldPoints),
                    String.valueOf(newPoints),
                    newTier.name()
            );

            MessageUI.displaySuccessMessage(
                    "Member points updated. New tier: " + newTier + "."
            );
        }

        pause();
    }

    // 6. DELETE MEMBER
    private void deleteMember() {

        // Step 1 — keep asking until member is found or user cancels
        RewardsMember target = null;
        int position = -1;
        while (target == null) {
            String id = ui.memberId();
            if (id == null) {
                MessageUI.displayInfoMessage("Cancelled.");
                pause();
                return;
            }
            for (int i = 1; i <= members.getNumberOfEntries(); i++) {
                if (members.getEntry(i).getMemberId().equalsIgnoreCase(id)) {
                    position = i;
                    target   = members.getEntry(i);
                    break;
                }
            }
            if (target == null) {
                MessageUI.displayErrorMessage(
                        "Member \"" + id + "\" not found. Try again or enter 0 to cancel."
                );
            }
        }

        // Step 2 — show member details and confirm deletion
        boolean confirmed = ui.confirmDelete(
                target.getMemberId(),
                target.getName(),
                target.getTier().name(),
                target.getPoints()
        );

        if (!confirmed) {
            MessageUI.displayInfoMessage("Delete cancelled.");
            pause();
            return;
        }

        // Step 3 — remove from ADT by position
        members.remove(position);

        save();

        MessageUI.displaySuccessMessage(
                "Member " + target.getMemberId()
                + " (" + target.getName() + ") deleted successfully."
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

    // GENERATE NEXT MEMBER ID  (LM001, LM002, ...)
    private String nextMemberId() {

        int max = 0;

        for (int i = 1;
                i <= members.getNumberOfEntries();
                i++) {

            String raw =
                    members.getEntry(i)
                           .getMemberId()
                           .toUpperCase();

            /*
             * Strip the "LM" prefix and parse the numeric part.
             * Handles both 3-digit (LM001) and longer IDs (LM0012).
             */
            if (raw.startsWith("LM")) {

                try {

                    int num = Integer.parseInt(raw.substring(2));

                    if (num > max) {
                        max = num;
                    }

                } catch (NumberFormatException ignored) {
                    // non-numeric suffix — skip
                }
            }
        }

        // Zero-pad to at least 3 digits: 1 -> "001", 12 -> "012", 100 -> "100"
        return String.format("LM%03d", max + 1);
    }

    // SORT LIST BY MEMBER ID ASCENDING  (insertion sort on String)
    private void sortByMemberId(ListInterface<RewardsMember> list) {

        int size = list.getNumberOfEntries();

        for (int i = 2; i <= size; i++) {

            RewardsMember key = list.getEntry(i);

            int j = i - 1;

            while (j >= 1
                    && list.getEntry(j)
                           .getMemberId()
                           .compareToIgnoreCase(key.getMemberId()) > 0) {

                list.replace(j + 1, list.getEntry(j));
                j--;
            }

            list.replace(j + 1, key);
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
