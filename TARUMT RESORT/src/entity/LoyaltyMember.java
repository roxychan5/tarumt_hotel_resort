package entity;

public class LoyaltyMember implements Comparable<LoyaltyMember> {

  private final String memberId;
  private final String guestName;
  private final LoyaltyTier tier;
  private final String requestedRoomType;
  private final int arrivalSequence;

  public LoyaltyMember(String memberId, String guestName, LoyaltyTier tier,
      String requestedRoomType, int arrivalSequence) {
    this.memberId = memberId;
    this.guestName = guestName;
    this.tier = tier;
    this.requestedRoomType = requestedRoomType;
    this.arrivalSequence = arrivalSequence;
  }

  public String getMemberId() { return memberId; }
  public String getGuestName() { return guestName; }
  public LoyaltyTier getTier() { return tier; }
  public String getRequestedRoomType() { return requestedRoomType; }
  public int getArrivalSequence() { return arrivalSequence; }

  @Override
  public int compareTo(LoyaltyMember other) {
    int tierComparison = tier.getPriority() - other.tier.getPriority();
    // Earlier arrival wins only when membership tiers are equal.
    return tierComparison != 0 ? tierComparison
        : other.arrivalSequence - arrivalSequence;
  }
}

