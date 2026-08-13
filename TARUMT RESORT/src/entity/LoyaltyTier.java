package entity;

/** Loyalty membership tiers used to determine room-assignment priority. */
public enum LoyaltyTier {
  SILVER(1), GOLD(2), PLATINUM(3), DIAMOND(4), ELITE(5);

  private final int priority;

  LoyaltyTier(int priority) {
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }

  public static LoyaltyTier fromPriority(int priority) {
    for (LoyaltyTier tier : values()) {
      if (tier.priority == priority) {
        return tier;
      }
    }
    return null;
  }
}