package utility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves the shared data directory for all resort modules. */
public final class DataFiles {

  private static final String PROJECT_DIRECTORY_NAME = "TARUMT RESORT";
  private static final String DATA_DIRECTORY_NAME = "data";

  private DataFiles() {
  }

  public static Path directory() {
    return findResortDirectory().resolve(DATA_DIRECTORY_NAME);
  }

  public static Path resolve(String fileName) {
    return directory().resolve(fileName);
  }

  private static Path findResortDirectory() {
    Path current = Paths.get("").toAbsolutePath().normalize();

    for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
      if (PROJECT_DIRECTORY_NAME.equals(cursor.getFileName() == null ? "" : cursor.getFileName().toString())
          && Files.isDirectory(cursor.resolve(DATA_DIRECTORY_NAME))) {
        return cursor;
      }

      Path nestedResortDirectory = cursor.resolve(PROJECT_DIRECTORY_NAME);
      if (Files.isDirectory(nestedResortDirectory.resolve(DATA_DIRECTORY_NAME))) {
        return nestedResortDirectory;
      }
    }

    if (PROJECT_DIRECTORY_NAME.equals(current.getFileName() == null ? "" : current.getFileName().toString())) {
      return current;
    }
    return current.resolve(PROJECT_DIRECTORY_NAME);
  }
}
