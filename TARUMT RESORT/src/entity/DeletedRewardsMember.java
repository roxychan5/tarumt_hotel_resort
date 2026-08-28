package entity;

import java.time.LocalDate;

/** A deleted loyalty profile retained during its 30-day recovery period. */
public class DeletedRewardsMember {

    public static final int RETENTION_DAYS = 30;

    private final RewardsMember member;
    private final LocalDate deletedDate;

    public DeletedRewardsMember(RewardsMember member, LocalDate deletedDate) {
        this.member = member;
        this.deletedDate = deletedDate;
    }

    public RewardsMember getMember() { return member; }

    public LocalDate getDeletedDate() { return deletedDate; }

    public LocalDate getRestoreDeadline() {
        return deletedDate.plusDays(RETENTION_DAYS);
    }

    public boolean isExpired(LocalDate today) {
        return today.isAfter(getRestoreDeadline());
    }
}
