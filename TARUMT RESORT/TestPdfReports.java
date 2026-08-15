import entity.*;
import utility.PdfReportEngine;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Standalone test: generates both PDF reports with sample data.
 * Run from TARUMT RESORT folder:
 *   java -cp "lib\pdfbox-app-3.0.3.jar;build\classes" TestPdfReports
 */
public class TestPdfReports {

  public static void main(String[] args) throws IOException {
    System.out.println("Generating PDFs...");
    generateReport1();
    generateReport2();
    System.out.println("Done. Check output/pdf/ folder.");
  }

  static void generateReport1() throws IOException {
    // ── Sample filtered task data ──────────────────────────────────────────
    String[][] taskData = {
        {"HK1001","R101","HK000","CHECKOUT_CLEAN","Dirty",        "2026-08-08 09:12"},
        {"HK1002","R102","HK002","DEEP_CLEAN",    "Dirty",        "2026-08-13 15:50"},
        {"HK1003","R201","HK001","INSPECTION",    "Inspected",    "2026-08-09 11:00"},
        {"HK1004","R301","HK003","TURNDOWN",      "Ready for Check-In","2026-08-10 14:30"},
        {"HK1005","R302","HK002","CHECKOUT_CLEAN","Cleaning In Progress","2026-08-11 08:45"},
    };

    Map<String,Integer> statusCount = new LinkedHashMap<>();
    statusCount.put("Dirty",               2);
    statusCount.put("Cleaning In Progress",1);
    statusCount.put("Inspected",           1);
    statusCount.put("Ready for Check-In",  1);

    Map<String,Integer> roomTypeCount = new LinkedHashMap<>();
    roomTypeCount.put("Standard", 2);
    roomTypeCount.put("Deluxe",   1);
    roomTypeCount.put("Suite",    2);

    String outDir = "output" + File.separator + "pdf";
    new File(outDir).mkdirs();
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String outPath = outDir + File.separator + "housekeeping_summary_" + ts + ".pdf";

    PdfReportEngine pdf = new PdfReportEngine();

    // ── Cover ──────────────────────────────────────────────────────────────
    pdf.addCoverPage(
        "Housekeeping Operational Summary",
        "Tasks by Status | Room Type Distribution | Filtered Analysis",
        "2026-08-01  to  2026-08-15",
        "Housekeeping Supervisor");

    // ── Page 1: KPIs + bar chart ───────────────────────────────────────────
    pdf.beginContentPage();
    pdf.addSectionHeading("Report Overview");
    pdf.addKpiRow("Report Type",      "Housekeeping Operational Summary", null);
    pdf.addKpiRow("Date Range",       "2026-08-01  to  2026-08-15", null);
    pdf.addKpiRow("Status Filter",    "ALL", null);
    pdf.addKpiRow("Room Type Filter", "ALL", null);
    pdf.addKpiRow("Tasks Matched",    String.valueOf(taskData.length),
        PdfReportEngine.SUCCESS);
    pdf.addDivider();

    pdf.addSectionHeading("Key Performance Indicators");
    pdf.addKpiCards(
        new String[]{"Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"},
        new String[]{"2","1","1","1"},
        new Color[]{ PdfReportEngine.DANGER, PdfReportEngine.WARNING,
                     PdfReportEngine.ACCENT_BLUE, PdfReportEngine.SUCCESS });
    pdf.addSpace(12);

    // Bar chart — tasks per status
    String[] sLabels = {"Dirty","Cleaning In Progress","Inspected","Ready for Check-In"};
    double[] sValues = {2, 1, 1, 1};
    pdf.addBarChart("Tasks by Status", sLabels, sValues, "Number of Tasks");

    // Donut — room type
    pdf.addSectionHeading("Room Type Distribution");
    pdf.addDonutChart("Tasks by Room Type",
        new String[]{"Standard","Deluxe","Suite"},
        new double[]{2, 1, 2});

    // ── Page 2: Detailed table ─────────────────────────────────────────────
    pdf.beginContentPage();
    pdf.addSectionHeading("Detailed Task List (Sorted by Status Priority)");
    pdf.addBodyText(
        "Algorithm: Tasks sorted by Bubble Sort on date, then Binary Search for date range," +
        " then Bubble Sort by status urgency (Dirty first).", 9);
    pdf.addSpace(6);

    String[] headers = {"Task ID","Room","Staff","Task Type","Status","Logged At"};
    float[] colW = {60, 50, 60, 90, 110, 120};
    List<String[]> rows = new ArrayList<>();
    for (String[] t : taskData) rows.add(t);
    pdf.addTable(headers, rows, colW);

    pdf.save(outPath);
    System.out.println("Report 1 saved: " + outPath);
  }

  static void generateReport2() throws IOException {
    String[] staffIds = {"HK000","HK001","HK002","HK003"};
    int[] totals      = {3, 2, 2, 1};
    int[] pending     = {2, 1, 2, 1};

    String outDir = "output" + File.separator + "pdf";
    new File(outDir).mkdirs();
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String outPath = outDir + File.separator + "staff_workload_" + ts + ".pdf";

    PdfReportEngine pdf = new PdfReportEngine();

    // ── Cover ──────────────────────────────────────────────────────────────
    pdf.addCoverPage(
        "Staff Workload & Performance Analysis",
        "Insertion Sort Ranking | Pending vs Completed | Load Status Flags",
        "Current business cycle",
        "Housekeeping Supervisor");

    // ── Page 1: Summary + horizontal bar chart ─────────────────────────────
    pdf.beginContentPage();
    pdf.addSectionHeading("Report Overview");
    pdf.addKpiRow("Report Type",       "Staff Workload & Performance Analysis", null);
    pdf.addKpiRow("Staff Prefix Filter","All Staff", null);
    pdf.addKpiRow("Min Tasks Threshold","0", null);
    pdf.addKpiRow("Staff Evaluated",   "4", PdfReportEngine.ACCENT_BLUE);
    pdf.addKpiRow("Total Tasks",       "8", null);
    pdf.addKpiRow("Total Pending",     "6", PdfReportEngine.WARNING);
    pdf.addDivider();

    pdf.addSectionHeading("Key Performance Indicators");
    pdf.addKpiCards(
        new String[]{"Staff Evaluated","Total Tasks","Pending","Completion Rate"},
        new String[]{"4","8","6","25%"},
        new Color[]{ PdfReportEngine.BRAND_TEAL, PdfReportEngine.ACCENT_BLUE,
                     PdfReportEngine.WARNING, PdfReportEngine.SUCCESS });
    pdf.addSpace(12);

    // Horizontal bar chart — total vs pending
    double[] dTotals  = {3, 2, 2, 1};
    double[] dPending = {2, 1, 2, 1};
    pdf.addSectionHeading("Staff Workload Comparison (Insertion Sort — Highest First)");
    pdf.addBodyText(
        "Staff ranked by total tasks using Insertion Sort (descending). " +
        "Blue = Total Tasks, Orange = Pending Tasks.", 9);
    pdf.addSpace(4);
    pdf.addHorizontalBarChart("Total vs Pending Tasks per Staff",
        staffIds,
        new double[][]{dTotals, dPending},
        new String[]{"Total Tasks","Pending Tasks"});

    // ── Page 2: Ranked table + recommendations ─────────────────────────────
    pdf.beginContentPage();
    pdf.addSectionHeading("Staff Performance Ranking");
    pdf.addBodyText(
        "Sorted by total workload (descending). Flags: [OVERLOADED] >3 tasks," +
        " [OPTIMAL] 2-3 tasks, [LIGHT] 1 task.", 9);
    pdf.addSpace(6);

    String[] headers = {"Rank","Staff ID","Total Tasks","Pending","Completed","Load Status"};
    float[] colW = {35, 70, 70, 60, 70, 90};
    List<String[]> rows = new ArrayList<>();
    String[] flags = {"OVERLOADED","OPTIMAL","OPTIMAL","LIGHT"};
    for (int i = 0; i < staffIds.length; i++) {
      rows.add(new String[]{
          String.valueOf(i+1), staffIds[i],
          String.valueOf(totals[i]), String.valueOf(pending[i]),
          String.valueOf(totals[i]-pending[i]), flags[i]
      });
    }
    pdf.addTable(headers, rows, colW);

    pdf.addSpace(14);
    pdf.addSectionHeading("Management Recommendations");
    pdf.addBodyText("1 staff member (HK000) is OVERLOADED with 3 tasks. Consider redistributing tasks.", 10);
    pdf.addBodyText("1 staff member (HK003) has a LIGHT workload and may accept additional tasks.", 10);
    pdf.addBodyText("Action required: 6 task(s) remain pending. Review rooms R101 and R302 as priority.", 10);

    pdf.save(outPath);
    System.out.println("Report 2 saved: " + outPath);
  }
}
