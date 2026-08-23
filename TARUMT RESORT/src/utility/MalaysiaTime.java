package utility;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Centralised helper for Malaysia time (Asia/Kuala_Lumpur, UTC+8).
 *
 * Every timestamp captured in the system should come from
 * {@link #now()} so logs always show Malaysia local time.
 * All display formatters use the 24-hour clock (HH).
 *
 * @author Chan Rou Xuan
 */
public final class MalaysiaTime {

  /** Malaysia's time zone (UTC+8) - observes no daylight saving. */
  public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");

  /** 24-hour display format WITH seconds and AM/PM, e.g. "2026-08-23 17:35:12 PM". */
  public static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a");

  /** 24-hour compact format used for PDF filenames, e.g. "20260823_173512". */
  public static final DateTimeFormatter FILE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private MalaysiaTime() {
    // Utility class - not meant to be instantiated.
  }

  /** Returns the current date/time in Malaysia (24-hour clock). */
  public static LocalDateTime now() {
    return LocalDateTime.now(ZONE);
  }

  /** Formats a timestamp as "yyyy-MM-dd HH:mm:ss AM/PM" in Malaysia time. */
  public static String format(LocalDateTime dateTime) {
    return dateTime.format(FORMATTER);
  }
}