package adt;

/**
 * Generic Linear Stack ADT used by Housekeeping to store status-change
 * records. Its LIFO order makes the newest room-status change the first one
 * available for rollback.
 * Adapted from Frank M. Carrano, Data Structures and Algorithms in Java.
 *
 * @author Your Name
 */
public interface StackInterface<T> {

  public void push(T newEntry);

  public T pop();

  public T peek();

  public boolean isEmpty();

  public boolean isFull();

  public void clear();

  /** Returns the number of entries currently stored in the stack. */
  public int getSize();

  /** Removes up to {@code count} entries from the top of the stack. */
  public void popMultiple(int count);
}
