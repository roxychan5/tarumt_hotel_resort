package adt;

/**
 * Interface for the ADT list.
 * Adapted from Frank M. Carrano, Data Structures and Algorithms in Java.
 *
 * @author Your Name
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
