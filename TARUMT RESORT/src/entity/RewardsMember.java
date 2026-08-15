package entity;

import java.time.LocalDate;

/** A loyalty profile used by the Loyalty and Rewards service. */
public class RewardsMember {
  private final String memberId;
  private String name;
  private String email;
  private LoyaltyTier tier;
  private int points;
  private LocalDate pointsExpiryDate;

  public RewardsMember(String memberId, String name, String email, LoyaltyTier tier,
      int points, LocalDate pointsExpiryDate) {
    this.memberId = memberId;
    this.name = name;
    this.email = email;
    this.tier = tier;
    this.points = points;
    this.pointsExpiryDate = pointsExpiryDate;
  }

  public String getMemberId() { return memberId; }
  public String getName() { return name; }
  public String getEmail() { return email; }
  public LoyaltyTier getTier() { return tier; }
  public int getPoints() { return points; }
  public LocalDate getPointsExpiryDate() { return pointsExpiryDate; }
  public void setName(String name) { this.name = name; }
  public void setEmail(String email) { this.email = email; }
  public void setTier(LoyaltyTier tier) { this.tier = tier; }
  public void addPoints(int amount) { points += amount; }
  public boolean redeemPoints(int amount) {
    if (amount <= 0 || amount > points) return false;
    points -= amount;
    return true;
  }
  public void setPointsExpiryDate(LocalDate date) { pointsExpiryDate = date; }
}
