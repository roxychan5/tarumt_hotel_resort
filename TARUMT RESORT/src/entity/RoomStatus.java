package entity;

/**
 * The different stages (lifecycle) a room goes through during cleaning.
 *
 * Imagine a room's "life story":
 *   1. DIRTY     -> the guest checked out, room needs cleaning
 *   2. CLEANING  -> the housekeeper is currently cleaning it
 *   3. INSPECTED -> a supervisor has checked the cleaning is OK
 *   4. READY     -> the room is ready for the next guest
 *
 * Stored in the Room's "status" field, so the system always knows
 * what stage each room is at. This is an enum (a fixed list of choices),
 * so we can only ever use one of these 4 values - no typos allowed!
 *
 * @author Chan Rou Xuan
 */
public enum RoomStatus {
  // The 4 allowed stages, each with a pretty label for display:
  DIRTY("Dirty"),
  CLEANING_IN_PROGRESS("Cleaning In Progress"),
  INSPECTED("Inspected"),
  READY_FOR_CHECK_IN("Ready for Check-In");

  /** A user-friendly name for this stage (e.g. "Dirty" instead of "DIRTY"). */
  private final String label;

  /**
   * Enum constructor - links up each stage with its nice label.
   * E.g. DIRTY("Dirty") means DIRTY has label "Dirty".
   */
  RoomStatus(String label) {
    this.label = label;
  }

  /** Returns the readable label, e.g. "Cleaning In Progress". */
  public String getLabel() {
    return label;
  }

  /**
   * Returns the NEXT stage in the cleaning flow, or itself if already last.
   *
   * This is like a "one-way path":
   *   DIRTY -> CLEANING -> INSPECTED -> READY (stops here)
   * Used by the "Advance Room Workflow" menu option.
   */
  public RoomStatus nextStatus() {
    switch (this) {
      case DIRTY:
        return CLEANING_IN_PROGRESS;
      case CLEANING_IN_PROGRESS:
        return INSPECTED;
      case INSPECTED:
        return READY_FOR_CHECK_IN;
      default:
        // READY_FOR_CHECK_IN has no next stage, so it stays the same:
        return this;
    }
  }

  /**
   * Checks if the room can move to the next stage.
   * A room that is already READY_FOR_CHECK_IN cannot advance further,
   * so this returns false for it, and true for all other stages.
   */
  public boolean canAdvance() {
    return this != READY_FOR_CHECK_IN;
  }
}