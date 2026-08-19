package entity;

import java.time.LocalDate;

/**
 * Represents a loyalty programme member.
 *
 * @author
 */
public class RewardsMember {

    private final String memberId;
    private String name;
    private String email;
    private LoyaltyTier tier;
    private int points;
    private LocalDate pointsExpiryDate;

    public RewardsMember(
            String memberId,
            String name,
            String email,
            LoyaltyTier tier,
            int points,
            LocalDate pointsExpiryDate) {

        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.tier = tier;
        this.points = points;
        this.pointsExpiryDate = pointsExpiryDate;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public int getPoints() {
        return points;
    }

    public LocalDate getPointsExpiryDate() {
        return pointsExpiryDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTier(LoyaltyTier tier) {
        this.tier = tier;
    }

    public void addPoints(int amount) {
        if (amount > 0) {
            points += amount;
        }
    }

    public boolean redeemPoints(int amount) {

        if (amount <= 0 || amount > points) {
            return false;
        }

        points -= amount;
        return true;
    }

    public void setPointsExpiryDate(LocalDate date) {
        this.pointsExpiryDate = date;
    }

    /**
     * Returns the personalised promotion based on the member tier.
     */
    public String getPromotion() {

        switch (tier) {

            case ELITE:
                return "20% suite upgrade offer";

            case DIAMOND:
                return "15% spa and dining offer";

            case PLATINUM:
                return "10% room upgrade offer";

            case GOLD:
                return "8% dining discount";

            case SILVER:
            default:
                return "5% dining discount";
        }
    }

    @Override
    public String toString() {

        return memberId
                + " | "
                + name
                + " | "
                + tier
                + " | "
                + points
                + " points";
    }
}