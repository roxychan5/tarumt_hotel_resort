package utility;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Shared PDF generation utility for TARUMT Resorts management reports.
 * Renders cover pages, section headings, data tables, bar charts,
 * and donut charts using Apache PDFBox + Java2D.
 *
 * @author Chan Rou Xuan
 */
public class PdfReportEngine {

  // ── Brand Palette ────────────────────────────────────────────────────────
  public static final Color BRAND_NAVY   = new Color(0x0D, 0x2B, 0x55);
  public static final Color BRAND_TEAL   = new Color(0x00, 0x8B, 0x8B);
  public static final Color BRAND_GOLD   = new Color(0xC8, 0xA2, 0x00);
  public static final Color ACCENT_BLUE  = new Color(0x1A, 0x73, 0xE8);
  public static final Color LIGHT_GREY   = new Color(0xF5, 0xF7, 0xFA);
  public static final Color MID_GREY     = new Color(0xCC, 0xD6, 0xE0);
  public static final Color TEXT_DARK    = new Color(0x1A, 0x1A, 0x2E);
  public static final Color TEXT_MED     = new Color(0x4A, 0x5A, 0x6A);
  public static final Color SUCCESS      = new Color(0x27, 0xAE, 0x60);
  public static final Color WARNING      = new Color(0xE6, 0x7E, 0x22);
  public static final Color DANGER       = new Color(0xC0, 0x39, 0x2B);

  public static final Color[] CHART_PALETTE = {
      new Color(0x1A, 0x73, 0xE8),
      new Color(0x34, 0xA8, 0x53),
      new Color(0xFB, 0xBC, 0x04),
      new Color(0xEA, 0x43, 0x35),
      new Color(0x9C, 0x27, 0xB0),
      new Color(0x00, 0x97, 0xA7)
  };

  // ── Page geometry ────────────────────────────────────────────────────────
  private static final float PAGE_W  = PDRectangle.A4.getWidth();
  private static final float PAGE_H  = PDRectangle.A4.getHeight();
  private static final float MARGIN  = 50f;
  private static final float CONTENT_W = PAGE_W - MARGIN * 2;

  // ── Fonts ────────────────────────────────────────────────────────────────
  private static final PDType1Font FONT_REGULAR =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final PDType1Font FONT_BOLD    =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
  private static final PDType1Font FONT_OBLIQUE =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

  private final PDDocument doc;
  private float cursorY;
  private PDPage currentPage;
  private PDPageContentStream stream;

  public PdfReportEngine() throws IOException {
    doc = new PDDocument();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Public API
  // ══════════════════════════════════════════════════════════════════════════

  /** Renders a full-bleed cover page. */
  public void addCoverPage(String title, String subtitle, String reportDate,
      String generatedBy) throws IOException {
    PDPage page = newPage();
    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

      // Navy gradient background
      BufferedImage bg = new BufferedImage(595, 842, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = bg.createGraphics();
      g.setPaint(new GradientPaint(0, 0, BRAND_NAVY, 0, 842, new Color(0x05, 0x18, 0x38)));
      g.fillRect(0, 0, 595, 842);

      // Gold accent bar
      g.setColor(new Color(0xC8, 0xA2, 0x00));
      g.fillRect(0, 340, 595, 6);

      // Decorative circles
      g.setColor(new Color(255, 255, 255, 18));
      g.fillOval(-60, 600, 300, 300);
      g.fillOval(380, -60, 280, 280);
      g.setColor(new Color(255, 255, 255, 10));
      g.fillOval(440, 580, 220, 220);
      g.dispose();

      addImageToPage(cs, bg, 0, 0, PAGE_W, PAGE_H);

      // Hotel name
      drawCenteredText(cs, "TARUMT RESORTS", PAGE_H - 180, FONT_BOLD, 28, Color.WHITE);
      drawCenteredText(cs, "MANAGEMENT SYSTEM", PAGE_H - 208, FONT_BOLD, 16,
          new Color(0xC8, 0xA2, 0x00));

      // Title block
      drawCenteredText(cs, title, PAGE_H - 290, FONT_BOLD, 22, Color.WHITE);
      drawCenteredText(cs, subtitle, PAGE_H - 320, FONT_OBLIQUE, 13,
          new Color(0xAA, 0xCC, 0xEE));

      // Meta info
      drawCenteredText(cs, "Report Period: " + reportDate, PAGE_H - 480,
          FONT_REGULAR, 11, new Color(0xCC, 0xDD, 0xEE));
      drawCenteredText(cs, "Generated by: " + generatedBy, PAGE_H - 500,
          FONT_REGULAR, 11, new Color(0xCC, 0xDD, 0xEE));
      drawCenteredText(cs, "Generated on: "
          + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
          PAGE_H - 520, FONT_REGULAR, 11, new Color(0xCC, 0xDD, 0xEE));

      // Confidential footer
      drawCenteredText(cs, "CONFIDENTIAL — FOR MANAGEMENT USE ONLY", 60,
          FONT_OBLIQUE, 9, new Color(0x88, 0xAA, 0xCC));
    }
  }

  /** Opens a new content page and resets the writing cursor. */
  public void beginContentPage() throws IOException {
    closeStream();
    currentPage = newPage();
    stream = new PDPageContentStream(doc, currentPage);
    cursorY = PAGE_H - MARGIN;
    drawPageHeader();
    cursorY -= 30;
  }

  /** Writes a section heading line. */
  public void addSectionHeading(String text) throws IOException {
    ensureSpace(40);
    cursorY -= 14;

    // Teal background strip
    stream.setNonStrokingColor(BRAND_TEAL);
    stream.addRect(MARGIN, cursorY - 4, CONTENT_W, 22);
    stream.fill();

    stream.beginText();
    stream.setFont(FONT_BOLD, 11);
    stream.setNonStrokingColor(Color.WHITE);
    stream.newLineAtOffset(MARGIN + 8, cursorY + 4);
    stream.showText(text.toUpperCase());
    stream.endText();

    cursorY -= 22;
    stream.setNonStrokingColor(TEXT_DARK);
  }

  /** Renders a key-value pair summary line. */
  public void addKpiRow(String label, String value, Color valueColor) throws IOException {
    ensureSpace(20);
    cursorY -= 16;

    stream.beginText();
    stream.setFont(FONT_REGULAR, 10);
    stream.setNonStrokingColor(TEXT_MED);
    stream.newLineAtOffset(MARGIN + 8, cursorY);
    stream.showText(label);
    stream.endText();

    stream.beginText();
    stream.setFont(FONT_BOLD, 10);
    stream.setNonStrokingColor(valueColor != null ? valueColor : TEXT_DARK);
    stream.newLineAtOffset(MARGIN + 200, cursorY);
    stream.showText(value);
    stream.endText();
  }

  /**
   * Renders a data table with a header row and zebra striping.
   *
   * @param headers   column labels
   * @param rows      data rows (String[])
   * @param colWidths column widths in points (must sum ≈ CONTENT_W)
   */
  public void addTable(String[] headers, List<String[]> rows, float[] colWidths)
      throws IOException {
    float rowH = 18f;
    ensureSpace(rowH * 2 + 10);
    cursorY -= 10;

    // Header row
    float x = MARGIN;
    stream.setNonStrokingColor(BRAND_NAVY);
    stream.addRect(MARGIN, cursorY - rowH + 4, CONTENT_W, rowH);
    stream.fill();

    for (int i = 0; i < headers.length; i++) {
      stream.beginText();
      stream.setFont(FONT_BOLD, 9);
      stream.setNonStrokingColor(Color.WHITE);
      stream.newLineAtOffset(x + 4, cursorY - rowH + 8);
      stream.showText(clip(headers[i], colWidths[i] - 6, FONT_BOLD, 9));
      stream.endText();
      x += colWidths[i];
    }
    cursorY -= rowH;

    // Data rows
    boolean zebra = false;
    for (String[] row : rows) {
      ensureSpace(rowH + 6);

      if (zebra) {
        stream.setNonStrokingColor(LIGHT_GREY);
        stream.addRect(MARGIN, cursorY - rowH + 4, CONTENT_W, rowH);
        stream.fill();
      }

      x = MARGIN;
      for (int i = 0; i < row.length && i < colWidths.length; i++) {
        stream.beginText();
        stream.setFont(FONT_REGULAR, 9);
        stream.setNonStrokingColor(TEXT_DARK);
        stream.newLineAtOffset(x + 4, cursorY - rowH + 8);
        stream.showText(clip(row[i] == null ? "" : row[i], colWidths[i] - 6, FONT_REGULAR, 9));
        stream.endText();
        x += colWidths[i];
      }

      // Bottom border
      stream.setStrokingColor(MID_GREY);
      stream.setLineWidth(0.4f);
      stream.moveTo(MARGIN, cursorY - rowH + 4);
      stream.lineTo(MARGIN + CONTENT_W, cursorY - rowH + 4);
      stream.stroke();

      cursorY -= rowH;
      zebra = !zebra;
    }
    cursorY -= 8;
  }

  /**
   * Renders a vertical bar chart as an embedded image.
   *
   * @param labels   x-axis labels
   * @param values   bar heights (raw values)
   * @param chartTitle chart title
   * @param yLabel   y-axis label
   */
  public void addBarChart(String chartTitle, String[] labels, double[] values,
      String yLabel) throws IOException {
    int imgW = 900, imgH = 480;
    BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = setupG2D(img);

    // Background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, imgW, imgH);
    g.setColor(new Color(0xF0, 0xF4, 0xF8));
    g.fillRoundRect(0, 0, imgW - 1, imgH - 1, 12, 12);

    int plotL = 70, plotR = imgW - 30, plotT = 60, plotB = imgH - 80;
    int plotW = plotR - plotL, plotH = plotB - plotT;

    // Title
    g.setFont(new Font("SansSerif", Font.BOLD, 18));
    g.setColor(BRAND_NAVY);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(chartTitle, (imgW - fm.stringWidth(chartTitle)) / 2, 38);

    // Axes
    g.setColor(new Color(0x90, 0xA0, 0xB0));
    g.setStroke(new BasicStroke(1.5f));
    g.drawLine(plotL, plotT, plotL, plotB);
    g.drawLine(plotL, plotB, plotR, plotB);

    if (values.length == 0) {
      g.setFont(new Font("SansSerif", Font.PLAIN, 14));
      g.setColor(TEXT_MED);
      g.drawString("No data available", plotL + plotW / 2 - 60, plotT + plotH / 2);
      g.dispose();
      embedImage(img, 380f);
      return;
    }

    double maxVal = 0;
    for (double v : values) maxVal = Math.max(maxVal, v);
    if (maxVal == 0) maxVal = 1;
    double scale = (double) plotH / (maxVal * 1.15);

    // Horizontal grid lines
    int gridLines = 5;
    g.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
        1, new float[]{4, 4}, 0));
    g.setColor(new Color(0xCC, 0xD6, 0xE0));
    g.setFont(new Font("SansSerif", Font.PLAIN, 11));
    for (int i = 0; i <= gridLines; i++) {
      int y = plotB - (int)(plotH * i / gridLines);
      g.drawLine(plotL, y, plotR, y);
      int gridVal = (int) Math.round(maxVal * i / gridLines);
      g.setColor(TEXT_MED);
      g.drawString(String.valueOf(gridVal), plotL - 36, y + 4);
      g.setColor(new Color(0xCC, 0xD6, 0xE0));
    }

    // Bars
    g.setStroke(new BasicStroke(1f));
    int barCount = values.length;
    int gap = Math.max(8, plotW / (barCount * 5));
    int barW = Math.min(80, (plotW - gap * (barCount + 1)) / barCount);
    int startX = plotL + (plotW - (barW + gap) * barCount + gap) / 2;

    for (int i = 0; i < barCount; i++) {
      int barH2 = (int) (values[i] * scale);
      int bx = startX + i * (barW + gap);
      int by = plotB - barH2;

      Color c = CHART_PALETTE[i % CHART_PALETTE.length];
      g.setPaint(new GradientPaint(bx, by, c.brighter(), bx, plotB, c.darker()));
      g.fillRoundRect(bx, by, barW, barH2, 6, 6);

      // Value label on top
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.setColor(TEXT_DARK);
      String valStr = String.valueOf((int) values[i]);
      int vw = g.getFontMetrics().stringWidth(valStr);
      g.drawString(valStr, bx + (barW - vw) / 2, by - 4);

      // X label
      g.setFont(new Font("SansSerif", Font.PLAIN, 10));
      g.setColor(TEXT_MED);
      String lbl = labels[i];
      int lw = g.getFontMetrics().stringWidth(lbl);
      g.drawString(lbl, bx + (barW - lw) / 2, plotB + 18);
    }

    // Y-axis label
    g.setFont(new Font("SansSerif", Font.PLAIN, 11));
    g.setColor(TEXT_MED);
    java.awt.geom.AffineTransform orig = g.getTransform();
    g.rotate(-Math.PI / 2, 18, plotT + plotH / 2);
    g.drawString(yLabel, 18 - g.getFontMetrics().stringWidth(yLabel) / 2,
        plotT + plotH / 2 + 4);
    g.setTransform(orig);

    g.dispose();
    embedImage(img, 320f);
  }

  /**
   * Renders a horizontal bar chart (good for ranked staff lists).
   *
   * @param chartTitle title
   * @param labels     row labels
   * @param series     array of double[] — each series is a set of bars
   * @param seriesLabels legend labels for each series
   */
  public void addHorizontalBarChart(String chartTitle, String[] labels,
      double[][] series, String[] seriesLabels) throws IOException {
    int imgW = 900, imgH = Math.max(300, 80 + labels.length * 50);
    BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = setupG2D(img);

    g.setColor(new Color(0xF0, 0xF4, 0xF8));
    g.fillRoundRect(0, 0, imgW - 1, imgH - 1, 12, 12);

    int plotL = 130, plotR = imgW - 60, plotT = 60, plotB = imgH - 60;
    int plotW = plotR - plotL, plotH = plotB - plotT;

    // Title
    g.setFont(new Font("SansSerif", Font.BOLD, 18));
    g.setColor(BRAND_NAVY);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(chartTitle, (imgW - fm.stringWidth(chartTitle)) / 2, 40);

    // Axes
    g.setColor(new Color(0x90, 0xA0, 0xB0));
    g.setStroke(new BasicStroke(1.5f));
    g.drawLine(plotL, plotT, plotL, plotB);
    g.drawLine(plotL, plotB, plotR, plotB);

    if (labels.length == 0 || series.length == 0) {
      g.setFont(new Font("SansSerif", Font.PLAIN, 14));
      g.setColor(TEXT_MED);
      g.drawString("No data available", plotL + 20, plotT + 40);
      g.dispose();
      embedImage(img, (float)(imgH * 0.38));
      return;
    }

    double maxVal = 0;
    for (double[] s : series) for (double v : s) maxVal = Math.max(maxVal, v);
    if (maxVal == 0) maxVal = 1;

    int rowH = plotH / Math.max(1, labels.length);
    int subH = Math.min(18, rowH / series.length - 2);

    for (int i = 0; i < labels.length; i++) {
      int rowY = plotT + i * rowH;

      // Row label
      g.setFont(new Font("SansSerif", Font.BOLD, 11));
      g.setColor(TEXT_DARK);
      g.drawString(labels[i], plotL - g.getFontMetrics().stringWidth(labels[i]) - 6,
          rowY + rowH / 2 + 4);

      for (int s = 0; s < series.length; s++) {
        if (i >= series[s].length) continue;
        double val = series[s][i];
        int barW = (int)(val / maxVal * plotW);
        int by = rowY + s * (subH + 2) + (rowH - series.length * (subH + 2)) / 2;

        Color c = CHART_PALETTE[s % CHART_PALETTE.length];
        g.setPaint(new GradientPaint(plotL, by, c.brighter(), plotL + barW, by, c));
        g.fillRoundRect(plotL, by, Math.max(barW, 2), subH, 4, 4);

        // Value label
        if (barW > 20) {
          g.setFont(new Font("SansSerif", Font.BOLD, 10));
          g.setColor(Color.WHITE);
          g.drawString(String.valueOf((int) val), plotL + barW - 24, by + subH - 4);
        }
      }

      // Row separator
      g.setColor(new Color(0xCC, 0xD6, 0xE0));
      g.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
          1, new float[]{3, 3}, 0));
      g.drawLine(plotL, rowY + rowH, plotR, rowY + rowH);
    }

    // Legend
    int legendX = plotL;
    int legendY = plotB + 35;
    for (int s = 0; s < seriesLabels.length; s++) {
      Color c = CHART_PALETTE[s % CHART_PALETTE.length];
      g.setColor(c);
      g.fillRoundRect(legendX, legendY - 10, 16, 12, 3, 3);
      g.setFont(new Font("SansSerif", Font.PLAIN, 11));
      g.setColor(TEXT_DARK);
      g.drawString(seriesLabels[s], legendX + 20, legendY);
      legendX += g.getFontMetrics().stringWidth(seriesLabels[s]) + 40;
    }

    g.dispose();
    embedImage(img, (float)(imgH * 0.38));
  }

  /**
   * Renders a donut chart as an embedded image.
   *
   * @param chartTitle chart title
   * @param labels     segment labels
   * @param values     segment values
   */
  public void addDonutChart(String chartTitle, String[] labels, double[] values)
      throws IOException {
    int imgW = 680, imgH = 380;
    BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = setupG2D(img);

    g.setColor(new Color(0xF0, 0xF4, 0xF8));
    g.fillRoundRect(0, 0, imgW - 1, imgH - 1, 12, 12);

    // Title
    g.setFont(new Font("SansSerif", Font.BOLD, 16));
    g.setColor(BRAND_NAVY);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(chartTitle, (imgW - fm.stringWidth(chartTitle)) / 2, 36);

    double total = 0;
    for (double v : values) total += v;
    if (total == 0) total = 1;

    int cx = 220, cy = 210, r = 130, innerR = 70;
    double angle = -90;

    for (int i = 0; i < values.length; i++) {
      double sweep = values[i] / total * 360.0;
      Color c = CHART_PALETTE[i % CHART_PALETTE.length];
      g.setColor(c);
      g.fill(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, angle, sweep, Arc2D.PIE));
      angle += sweep;
    }

    // Inner white hole (donut)
    g.setColor(new Color(0xF0, 0xF4, 0xF8));
    g.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

    // Center text
    g.setFont(new Font("SansSerif", Font.BOLD, 13));
    g.setColor(BRAND_NAVY);
    String totalStr = String.valueOf((int) total);
    g.drawString(totalStr, cx - g.getFontMetrics().stringWidth(totalStr) / 2, cy + 4);
    g.setFont(new Font("SansSerif", Font.PLAIN, 9));
    g.setColor(TEXT_MED);
    g.drawString("TOTAL", cx - g.getFontMetrics().stringWidth("TOTAL") / 2, cy + 17);

    // Legend
    int lgX = 380, lgY = 80;
    for (int i = 0; i < labels.length; i++) {
      Color c = CHART_PALETTE[i % CHART_PALETTE.length];
      g.setColor(c);
      g.fillRoundRect(lgX, lgY - 10, 14, 14, 3, 3);
      g.setFont(new Font("SansSerif", Font.PLAIN, 11));
      g.setColor(TEXT_DARK);
      g.drawString(labels[i], lgX + 20, lgY);
      g.setFont(new Font("SansSerif", Font.BOLD, 11));
      g.setColor(TEXT_MED);
      int pct = (int)Math.round(values[i] / total * 100);
      g.drawString("(" + (int)values[i] + "  " + pct + "%)", lgX + 20, lgY + 14);
      lgY += 38;
    }

    g.dispose();
    embedImage(img, 280f);
  }

  /** Adds a spacer gap. */
  public void addSpace(float pts) throws IOException {
    ensureSpace(pts);
    cursorY -= pts;
  }

  /** Adds a thin horizontal divider line. */
  public void addDivider() throws IOException {
    ensureSpace(12);
    cursorY -= 6;
    stream.setStrokingColor(MID_GREY);
    stream.setLineWidth(0.5f);
    stream.moveTo(MARGIN, cursorY);
    stream.lineTo(MARGIN + CONTENT_W, cursorY);
    stream.stroke();
    cursorY -= 6;
  }

  /** Writes a body text line (wraps long text). */
  public void addBodyText(String text, float fontSize) throws IOException {
    ensureSpace(fontSize + 6);
    cursorY -= (fontSize + 4);
    stream.beginText();
    stream.setFont(FONT_REGULAR, fontSize);
    stream.setNonStrokingColor(TEXT_MED);
    stream.newLineAtOffset(MARGIN + 8, cursorY);
    stream.showText(clip(text, CONTENT_W - 16, FONT_REGULAR, fontSize));
    stream.endText();
  }

  /** Adds a KPI card row (4-per-row horizontal). */
  public void addKpiCards(String[] labels, String[] values, Color[] colors)
      throws IOException {
    ensureSpace(70);
    cursorY -= 10;
    float cardW = CONTENT_W / labels.length - 4;
    for (int i = 0; i < labels.length; i++) {
      float cx = MARGIN + i * (cardW + 4);
      Color bg = colors != null && i < colors.length ? colors[i] : ACCENT_BLUE;

      stream.setNonStrokingColor(bg);
      stream.addRect(cx, cursorY - 52, cardW, 52);
      stream.fill();

      stream.beginText();
      stream.setFont(FONT_BOLD, 18);
      stream.setNonStrokingColor(Color.WHITE);
      float tw = getTextWidth(values[i], FONT_BOLD, 18);
      stream.newLineAtOffset(cx + (cardW - tw) / 2, cursorY - 26);
      stream.showText(values[i]);
      stream.endText();

      stream.beginText();
      stream.setFont(FONT_REGULAR, 8);
      stream.setNonStrokingColor(new Color(220, 235, 255));
      float lw = getTextWidth(labels[i], FONT_REGULAR, 8);
      stream.newLineAtOffset(cx + (cardW - lw) / 2, cursorY - 42);
      stream.showText(labels[i]);
      stream.endText();
    }
    cursorY -= 60;
  }

  /** Saves the document to the given path. */
  public void save(String filePath) throws IOException {
    closeStream();
    doc.save(filePath);
  }

  /** Closes the document and releases all resources. Safe to call multiple times. */
  public void close() throws IOException {
    closeStream();
    doc.close();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // Private helpers
  // ══════════════════════════════════════════════════════════════════════════

  private PDPage newPage() {
    PDPage page = new PDPage(PDRectangle.A4);
    doc.addPage(page);
    currentPage = page;
    return page;
  }

  private void drawPageHeader() throws IOException {
    // Thin navy header bar
    stream.setNonStrokingColor(BRAND_NAVY);
    stream.addRect(0, PAGE_H - 32, PAGE_W, 32);
    stream.fill();

    stream.beginText();
    stream.setFont(FONT_BOLD, 9);
    stream.setNonStrokingColor(Color.WHITE);
    stream.newLineAtOffset(MARGIN, PAGE_H - 20);
    stream.showText("TARUMT RESORTS — MANAGEMENT REPORT");
    stream.endText();

    stream.beginText();
    stream.setFont(FONT_REGULAR, 8);
    stream.setNonStrokingColor(new Color(0xAA, 0xCC, 0xEE));
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    float tw = getTextWidth(ts, FONT_REGULAR, 8);
    stream.newLineAtOffset(PAGE_W - MARGIN - tw, PAGE_H - 20);
    stream.showText(ts);
    stream.endText();

    // Gold accent line
    stream.setStrokingColor(new Color(0xC8, 0xA2, 0x00));
    stream.setLineWidth(1.5f);
    stream.moveTo(0, PAGE_H - 32);
    stream.lineTo(PAGE_W, PAGE_H - 32);
    stream.stroke();
  }

  private void drawCenteredText(PDPageContentStream cs, String text, float y,
      PDType1Font font, float size, Color color) throws IOException {
    float tw = getTextWidth(text, font, size);
    cs.beginText();
    cs.setFont(font, size);
    cs.setNonStrokingColor(color);
    cs.newLineAtOffset((PAGE_W - tw) / 2, y);
    cs.showText(text);
    cs.endText();
  }

  private void ensureSpace(float needed) throws IOException {
    if (cursorY - needed < MARGIN + 20) {
      closeStream();
      currentPage = newPage();
      stream = new PDPageContentStream(doc, currentPage);
      cursorY = PAGE_H - MARGIN;
      drawPageHeader();
      cursorY -= 30;
    }
  }

  private void embedImage(BufferedImage img, float displayH) throws IOException {
    float aspect = (float) img.getWidth() / img.getHeight();
    float displayW = Math.min(CONTENT_W, displayH * aspect);
    float displayX = MARGIN + (CONTENT_W - displayW) / 2f;

    ensureSpace(displayH + 10);
    cursorY -= displayH;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", baos);
    PDImageXObject pdImg = PDImageXObject.createFromByteArray(doc, baos.toByteArray(), "chart");
    stream.drawImage(pdImg, displayX, cursorY, displayW, displayH);
    cursorY -= 10;
  }

  private void addImageToPage(PDPageContentStream cs, BufferedImage img,
      float x, float y, float w, float h) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", baos);
    PDImageXObject pdImg = PDImageXObject.createFromByteArray(doc, baos.toByteArray(), "bg");
    cs.drawImage(pdImg, x, y, w, h);
  }

  private Graphics2D setupG2D(BufferedImage img) {
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    return g;
  }

  private float getTextWidth(String text, PDType1Font font, float size) throws IOException {
    return font.getStringWidth(text) / 1000f * size;
  }

  private String clip(String text, float maxWidth, PDType1Font font, float size) {
    if (text == null) return "";
    try {
      while (text.length() > 1 && font.getStringWidth(text) / 1000f * size > maxWidth) {
        text = text.substring(0, text.length() - 1);
      }
      if (text.length() < (text.length() + 3)) return text;
    } catch (IOException ignored) {}
    return text;
  }

  private void closeStream() throws IOException {
    if (stream != null) {
      stream.close();
      stream = null;
    }
  }
}
