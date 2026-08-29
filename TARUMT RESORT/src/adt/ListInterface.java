package adt;

/**
 * Generic Linear ADT contract used by the Housekeeping module for its sequential task log and room-status collection. Entries are addressed in
 * their stored order using one-based positions.
 * Adapted from Data Structures and Algorithms in Java.
 *
 * @author Chan Rou Xuan
 */
public interface ListInterface<T> {

  /**
   * Task: Adds a new entry to the end of the list.
   *
   * @param newEntry the object to add
   * @return true if the entry was added successfully, or false otherwise
   */
  public boolean add(T newEntry);

  /**
   * Task: Adds a new entry at a specified one-based position in the list.
   * Entries at and after that position move one place to the right.
   *
   * @param newPosition the one-based position for the new entry
   * @param newEntry the object to add
   * @return true if the position is valid and the entry was added, or false otherwise
   */
  public boolean add(int newPosition, T newEntry);

  /**
   * Task: Removes and returns the entry at a specified one-based position.
   * Remaining entries keep their relative order.
   *
   * @param givenPosition the one-based position of the entry to remove
   * @return the removed entry, or null if the position is invalid
   */
  public T remove(int givenPosition);

  /**
   * Task: Removes all entries from the list.
   */
  public void clear();

  /**
   * Task: Replaces the entry at a specified one-based position.
   *
   * @param givenPosition the one-based position to update
   * @param newEntry the replacement object
   * @return true if the position is valid and the entry was replaced, or false otherwise
   */
  public boolean replace(int givenPosition, T newEntry);

  /**
   * Task: Retrieves the entry at a specified one-based position without removing it.
   *
   * @param givenPosition the one-based position of the entry to retrieve
   * @return the entry at the position, or null if the position is invalid
   */
  public T getEntry(int givenPosition);

  /**
   * Task: Detects whether the list contains a given entry.
   *
   * @param anEntry the entry to search for
   * @return true if the entry is in the list, or false otherwise
   */
  public boolean contains(T anEntry);

  /**
   * Task: Gets the current number of entries in the list.
   *
   * @return the number of entries currently stored
   */
  public int getNumberOfEntries();

  /**
   * Task: Detects whether the list is empty.
   *
   * @return true if the list contains no entries, or false otherwise
   */
  public boolean isEmpty();

  /**
   * Task: Detects whether the list has reached its storage capacity.
   *
   * @return true if no further entries can be added without expanding storage,
   *         or false otherwise
   */
  public boolean isFull();
}