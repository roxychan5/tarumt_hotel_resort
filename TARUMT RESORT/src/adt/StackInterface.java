package adt;

/**
 * Generic Linear Stack ADT used by Housekeeping to store status-change
 * records. Its LIFO order makes the newest room-status change the first one
 * available for rollback.
 * Adapted from Frank M. Carrano, Data Structures and Algorithms in Java.
 *
 * @author Chan Rou Xuan
 */
public interface StackInterface<T> {

  /**
   * Task: Adds a new entry to the top of this stack.
   *
   * @param newEntry the object to be added
   */
  public void push(T newEntry);

  /**
   * Task: Removes and returns the entry at the top of this stack.
   *
   * @return the top entry, or null if the stack is empty before the operation
   */
  public T pop();

  /**
   * Task: Retrieves the entry at the top of this stack without removing it.
   *
   * @return the top entry, or null if the stack is empty
   */
  public T peek();

  /**
   * Task: Detects whether this stack contains no entries.
   *
   * @return true if the stack is empty, or false otherwise
   */
  public boolean isEmpty();

  /**
   * Task: Detects whether this stack has reached its storage capacity.
   * A linked implementation can grow while memory is available.
   *
   * @return true if no more entries can be added, or false otherwise
   */
  public boolean isFull();

  /**
   * Task: Removes all entries from this stack.
   */
  public void clear();

  /**
   * Task: Counts the entries currently stored in this stack.
   *
   * @return the number of entries in the stack
   */
  public int getSize();

}
