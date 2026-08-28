package utility;

/** Small CSV helper for the resort's comma-separated text data files. */
public final class CsvUtils {

  private CsvUtils() {
  }

  /** Creates one RFC-style CSV row, escaping commas and quotation marks. */
  public static String row(String... values) {
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < values.length; index++) {
      if (index > 0) output.append(',');
      String value = values[index] == null ? "" : values[index];
      output.append('"');
      for (int character = 0; character < value.length(); character++) {
        char current = value.charAt(character);
        if (current == '"') output.append('"');
        output.append(current);
      }
      output.append('"');
    }
    return output.toString();
  }

  /** Reads a CSV row and also accepts old tab-separated rows during migration. */
  public static String[] parse(String line) {
    if (line.indexOf('\t') >= 0) return line.split("\\t", -1);

    String[] values = new String[8];
    int count = 0;
    StringBuilder value = new StringBuilder();
    boolean quoted = false;
    for (int index = 0; index < line.length(); index++) {
      char current = line.charAt(index);
      if (current == '"') {
        if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
          value.append('"');
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (current == ',' && !quoted) {
        values = add(values, count++, value.toString());
        value.setLength(0);
      } else {
        value.append(current);
      }
    }
    return add(values, count, value.toString(), count + 1);
  }

  private static String[] add(String[] values, int index, String value) {
    return add(values, index, value, index + 1);
  }

  private static String[] add(String[] values, int index, String value, int length) {
    if (index >= values.length) {
      String[] expanded = new String[values.length * 2];
      System.arraycopy(values, 0, expanded, 0, values.length);
      values = expanded;
    }
    values[index] = value;
    String[] result = new String[length];
    System.arraycopy(values, 0, result, 0, length);
    return result;
  }
}
