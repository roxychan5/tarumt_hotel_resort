package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A housekeeping task kept in the recycle bin after deletion.
 * It can be restored until 30 days after {@code deletedAt}.
 */
public class DeletedHousekeepingTask implements Serializable {

  private final HousekeepingTask task;
  private final LocalDateTime deletedAt;

  public DeletedHousekeepingTask(HousekeepingTask task, LocalDateTime deletedAt) {
    this.task = task;
    this.deletedAt = deletedAt;
  }

  public HousekeepingTask getTask() {
    return task;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  /** @return the final moment at which this deleted task may be restored. */
  public LocalDateTime getRestoreUntil() {
    return deletedAt.plusDays(30);
  }
}
