package adt;

/**
 * Generic Linear ADT contract used by the Housekeeping module for its sequential task log and room-status collection. Entries are addressed in
 * their stored order using one-based positions.
 * Adapted from Data Structures and Algorithms in Java.
 *
 * @author Chan Rou Xuan
 */
public interface ListInterface<T> {

  public boolean add(T newEntry);

  public boolean add(int newPosition, T newEntry);

  public T remove(int givenPosition);

  public void clear();

  public boolean replace(int givenPosition, T newEntry);

  public T getEntry(int givenPosition);

  public boolean contains(T anEntry);

  public int getNumberOfEntries();

  public boolean isEmpty();

  public boolean isFull();
}
