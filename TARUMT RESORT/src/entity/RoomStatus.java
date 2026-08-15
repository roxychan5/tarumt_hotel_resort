package entity;

/**
 * Represents the ordered cleaning lifecycle applied to rooms in the
 * Housekeeping Linear ADT module.
 *
 * @author Your Name
 */
public enum RoomStatus {
  DIRTY("Dirty"),
  CLEANING_IN_PROGRESS("Cleaning In Progress"),
  INSPECTED("Inspected"),
  READY_FOR_CHECK_IN("Ready for Check-In");

  private final String label;

  RoomStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public RoomStatus nextStatus() {
    switch (this) {
      case DIRTY:
        return CLEANING_IN_PROGRESS;
      case CLEANING_IN_PROGRESS:
        return INSPECTED;
      case INSPECTED:
        return READY_FOR_CHECK_IN;
      default:
        return this;
    }
  }

  public boolean canAdvance() {
    return this != READY_FOR_CHECK_IN;
  }
}
