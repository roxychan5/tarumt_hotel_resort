package entity;

public class LoyaltyMember implements Comparable<LoyaltyMember> {

  /** The registered loyalty profile this priority-queue entry belongs to. */
  private final RewardsMember loyaltyMember;
  private String requestedRoomType;
  private final int numberOfNights;
  private final int arrivalSequence;

  public LoyaltyMember(RewardsMember loyaltyMember, String requestedRoomType,
      int numberOfNights, int arrivalSequence) {
    this.loyaltyMember = loyaltyMember;
    this.requestedRoomType = requestedRoomType;
    this.numberOfNights = numberOfNights;
    this.arrivalSequence = arrivalSequence;
  }

  public RewardsMember getLoyaltyMember() { 
    return loyaltyMember; 
  }

  public String getMemberId() { 
    return loyaltyMember.getMemberId(); 
  }

  public String getGuestName() { 
    return loyaltyMember.getName(); 
  }

  public LoyaltyTier getTier() { 
    return loyaltyMember.getTier(); 
  }

  public String getRequestedRoomType() { 
    return requestedRoomType; 
  }

  public void setRequestedRoomType(String requestedRoomType) {
    this.requestedRoomType = requestedRoomType;
  }

  public int getNumberOfNights() {
    return numberOfNights;
  }

  public int getArrivalSequence() { 
    return arrivalSequence; 
  }

  @Override
  public int compareTo(LoyaltyMember other) {
    int tierComparison = getTier().getPriority() - other.getTier().getPriority();
    // Earlier arrival wins only when membership tiers are equal.
    return tierComparison != 0 ? tierComparison
        : other.arrivalSequence - arrivalSequence;
  }
}