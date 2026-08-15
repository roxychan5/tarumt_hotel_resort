from collections import Counter
from datetime import date
from pathlib import Path
import csv

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak,
    KeepTogether,
)
from reportlab.graphics.shapes import Drawing, String, Rect
from reportlab.graphics.charts.barcharts import VerticalBarChart, HorizontalBarChart

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "data"
OUT = ROOT / "output" / "pdf" / "Housekeeping_Management_Reports.pdf"

STATUS_ORDER = ["DIRTY", "CLEANING_IN_PROGRESS", "INSPECTED", "READY_FOR_CHECK_IN"]
STATUS_LABELS = {
    "DIRTY": "Dirty",
    "CLEANING_IN_PROGRESS": "Cleaning",
    "INSPECTED": "Inspected",
    "READY_FOR_CHECK_IN": "Ready",
}
STATUS_COLORS = [colors.HexColor("#E26D5A"), colors.HexColor("#F4B942"),
                 colors.HexColor("#4AA3A2"), colors.HexColor("#2F855A")]


def read_tsv(filename):
    path = DATA / filename
    if not path.exists():
        return []
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


rooms = read_tsv("rooms.txt")
tasks = read_tsv("housekeeping_tasks.txt")
room_by_number = {row["roomNumber"]: row for row in rooms}
status_counts = Counter(row["status"] for row in rooms)
staff_counts = Counter(row["assignedStaff"] for row in tasks)
staff_pending = Counter(row["assignedStaff"] for row in tasks if row["status"] != "READY_FOR_CHECK_IN")


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="Kicker", parent=styles["Normal"], fontName="Helvetica-Bold",
                          fontSize=8.5, leading=10, textColor=colors.HexColor("#147D91"),
                          spaceAfter=7, tracking=1.2))
styles.add(ParagraphStyle(name="ReportTitle", parent=styles["Title"], fontName="Helvetica-Bold",
                          fontSize=26, leading=31, textColor=colors.HexColor("#12324A"),
                          spaceAfter=8))
styles.add(ParagraphStyle(name="Subtitle", parent=styles["Normal"], fontName="Helvetica",
                          fontSize=10.5, leading=15, textColor=colors.HexColor("#496579"),
                          spaceAfter=12))
styles.add(ParagraphStyle(name="Section", parent=styles["Heading2"], fontName="Helvetica-Bold",
                          fontSize=15, leading=19, textColor=colors.HexColor("#12324A"),
                          spaceBefore=10, spaceAfter=7))
styles.add(ParagraphStyle(name="BodyClean", parent=styles["BodyText"], fontName="Helvetica",
                          fontSize=9.2, leading=13, textColor=colors.HexColor("#283C4B"), spaceAfter=6))
styles.add(ParagraphStyle(name="Small", parent=styles["BodyText"], fontName="Helvetica",
                          fontSize=7.6, leading=10, textColor=colors.HexColor("#516B7B")))
styles.add(ParagraphStyle(name="CardNumber", parent=styles["Normal"], fontName="Helvetica-Bold",
                          fontSize=20, leading=23, textColor=colors.HexColor("#12324A"), alignment=TA_CENTER))
styles.add(ParagraphStyle(name="CardLabel", parent=styles["Normal"], fontName="Helvetica-Bold",
                          fontSize=7.2, leading=10, textColor=colors.HexColor("#516B7B"), alignment=TA_CENTER))


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#D6E2E8"))
    canvas.line(18 * mm, 15 * mm, A4[0] - 18 * mm, 15 * mm)
    canvas.setFillColor(colors.HexColor("#58707F"))
    canvas.setFont("Helvetica", 7.5)
    canvas.drawString(18 * mm, 10 * mm, "TARUMT Resorts | Housekeeping Management Review")
    canvas.drawRightString(A4[0] - 18 * mm, 10 * mm, f"Page {doc.page}")
    canvas.restoreState()


def metric_card(value, label, accent):
    table = Table([[Paragraph(str(value), styles["CardNumber"])], [Paragraph(label, styles["CardLabel"])]],
                  colWidths=[40 * mm], rowHeights=[12 * mm, 9 * mm])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F6FAFC")),
        ("BOX", (0, 0), (-1, -1), 0.7, colors.HexColor("#D6E2E8")),
        ("LINEBEFORE", (0, 0), (0, -1), 3, accent),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 3),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
    ]))
    return table


def status_chart():
    values = [status_counts.get(key, 0) for key in STATUS_ORDER]
    drawing = Drawing(480, 190)
    chart = VerticalBarChart()
    chart.x, chart.y, chart.width, chart.height = 45, 30, 400, 130
    chart.data = [values]
    chart.categoryAxis.categoryNames = [STATUS_LABELS[key] for key in STATUS_ORDER]
    chart.categoryAxis.labels.fontName = "Helvetica"
    chart.categoryAxis.labels.fontSize = 8
    chart.valueAxis.valueMin = 0
    chart.valueAxis.valueMax = max(4, max(values, default=0) + 1)
    chart.valueAxis.valueStep = 1
    chart.valueAxis.labels.fontSize = 7
    chart.valueAxis.visibleGrid = True
    chart.valueAxis.gridStrokeColor = colors.HexColor("#DCE7ED")
    chart.bars[0].fillColor = colors.HexColor("#147D91")
    chart.bars[0].strokeColor = colors.HexColor("#147D91")
    drawing.add(chart)
    drawing.add(String(45, 170, "Rooms by current housekeeping status", fontName="Helvetica-Bold", fontSize=10,
                       fillColor=colors.HexColor("#12324A")))
    return drawing


def staff_chart():
    rows = sorted(staff_counts.items(), key=lambda item: (-item[1], item[0])) or [("No tasks", 0)]
    names, counts = zip(*rows)
    drawing = Drawing(480, 185)
    chart = HorizontalBarChart()
    chart.x, chart.y, chart.width, chart.height = 120, 25, 305, 125
    chart.data = [list(counts)]
    chart.categoryAxis.categoryNames = list(names)
    chart.categoryAxis.labels.fontName = "Helvetica"
    chart.categoryAxis.labels.fontSize = 8
    chart.valueAxis.valueMin = 0
    chart.valueAxis.valueMax = max(3, max(counts) + 1)
    chart.valueAxis.valueStep = 1
    chart.valueAxis.labels.fontSize = 7
    chart.valueAxis.visibleGrid = True
    chart.valueAxis.gridStrokeColor = colors.HexColor("#DCE7ED")
    chart.bars[0].fillColor = colors.HexColor("#3D8D6A")
    chart.bars[0].strokeColor = colors.HexColor("#3D8D6A")
    drawing.add(chart)
    drawing.add(String(45, 165, "Open task workload by assigned staff", fontName="Helvetica-Bold", fontSize=10,
                       fillColor=colors.HexColor("#12324A")))
    return drawing


def report_table(data, widths):
    table = Table(data, colWidths=widths, repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#12324A")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, 0), 8),
        ("FONTNAME", (0, 1), (-1, -1), "Helvetica"),
        ("FONTSIZE", (0, 1), (-1, -1), 7.8),
        ("TEXTCOLOR", (0, 1), (-1, -1), colors.HexColor("#283C4B")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F5F9FB")]),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#D6E2E8")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
    ]))
    return table


story = []
story += [Spacer(1, 25 * mm), Paragraph("HOUSEKEEPING", styles["Kicker"]),
          Paragraph("Management Reports", styles["ReportTitle"]),
          Paragraph("End-of-cycle operational summary for management review and decision-making.", styles["Subtitle"])]

summary_cards = Table([[metric_card(len(rooms), "REGISTERED ROOMS", colors.HexColor("#147D91")),
                        metric_card(len(tasks), "OPEN TASKS", colors.HexColor("#E09A2B")),
                        metric_card(status_counts.get("READY_FOR_CHECK_IN", 0), "ROOMS READY", colors.HexColor("#3D8D6A")),
                        metric_card(sum(staff_pending.values()), "PENDING TASKS", colors.HexColor("#A94B69"))]],
                      colWidths=[42 * mm] * 4)
summary_cards.setStyle(TableStyle([("LEFTPADDING", (0, 0), (-1, -1), 2), ("RIGHTPADDING", (0, 0), (-1, -1), 2)]))
story += [summary_cards, Spacer(1, 12 * mm), status_chart(), Spacer(1, 4 * mm)]
story += [Paragraph("Management reading", styles["Section"]),
          Paragraph("This report package contains two management reports generated from the Housekeeping task log and room-status board. The first identifies operational pressure by status and location. The second ranks staff workload to support fair task allocation.", styles["BodyClean"]),
          Paragraph("Data source: readable text records from rooms.txt and housekeeping_tasks.txt. Snapshot generated on " + date.today().isoformat() + ".", styles["Small"]), PageBreak()]

story += [Paragraph("REPORT 1", styles["Kicker"]), Paragraph("Room Status and Task Priority", styles["ReportTitle"]),
          Paragraph("A filtered operational view of every active housekeeping task, enriched with room location and then ordered by urgency: Dirty, Cleaning, Inspected, Ready.", styles["Subtitle"])]
task_rows = [["Task ID", "Room", "Floor", "Staff", "Task Type", "Current Status"]]
urgency = {status: index for index, status in enumerate(STATUS_ORDER)}
for task in sorted(tasks, key=lambda row: (urgency.get(row.get("status"), 99), row.get("roomNumber", ""))):
    room = room_by_number.get(task["roomNumber"], {})
    task_rows.append([task["taskId"], task["roomNumber"], room.get("floor", "-"), task["assignedStaff"],
                      task["taskType"].replace("_", " "), STATUS_LABELS.get(task["status"], task["status"])])
if len(task_rows) == 1:
    task_rows.append(["No active tasks", "-", "-", "-", "-", "-"])
story += [report_table(task_rows, [24*mm, 18*mm, 14*mm, 22*mm, 42*mm, 38*mm]), Spacer(1, 9 * mm)]
story += [Paragraph("Report algorithm", styles["Section"]),
          Paragraph("Search each task by room number to enrich it with floor details; filter to active tasks; sort the result first by operational status and then room number. Management can use this ordered list to direct staff toward Dirty and Cleaning rooms first.", styles["BodyClean"]),
          Paragraph("Decision focus: rooms at Dirty or Cleaning stages require immediate operational attention before new check-in demand is accepted.", styles["Small"]), PageBreak()]

story += [Paragraph("REPORT 2", styles["Kicker"]), Paragraph("Staff Workload and Completion Risk", styles["ReportTitle"]),
          Paragraph("A workload summary that searches task assignments, filters out completed-ready tasks, and ranks staff by active task count.", styles["Subtitle"]), staff_chart(), Spacer(1, 5 * mm)]
staff_rows = [["Rank", "Staff ID", "Assigned Tasks", "Pending Tasks", "Operational Note"]]
for rank, (staff, total) in enumerate(sorted(staff_counts.items(), key=lambda item: (-item[1], item[0])), 1):
    pending = staff_pending[staff]
    note = "Monitor workload" if pending >= 3 else "Within normal range"
    staff_rows.append([str(rank), staff, str(total), str(pending), note])
if len(staff_rows) == 1:
    staff_rows.append(["-", "No task data", "0", "0", "No action required"])
story += [report_table(staff_rows, [16*mm, 30*mm, 32*mm, 30*mm, 66*mm]), Spacer(1, 9 * mm)]
story += [Paragraph("Report algorithm", styles["Section"]),
          Paragraph("Search the sequential task log by assigned staff ID; filter tasks whose room status is not Ready for Check-In; sort staff in descending workload order, retaining alphabetical order for ties. This makes staffing imbalances visible at a glance.", styles["BodyClean"]),
          Paragraph("Management action: redistribute assignments when a staff member accumulates several pending tasks while others have spare capacity.", styles["Small"])]

OUT.parent.mkdir(parents=True, exist_ok=True)
document = SimpleDocTemplate(str(OUT), pagesize=A4, leftMargin=18*mm, rightMargin=18*mm,
                             topMargin=17*mm, bottomMargin=21*mm, title="Housekeeping Management Reports")
document.build(story, onFirstPage=footer, onLaterPages=footer)
print(OUT)
