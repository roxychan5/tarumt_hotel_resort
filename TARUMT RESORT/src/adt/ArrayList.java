package adt;

/**
 * Array-based implementation of the Linear List ADT. Housekeeping uses this
 * class to preserve the sequential order of rooms and cleaning tasks.
 *
 * @author Chan Rou Xuan
 */

import java.io.Serializable;

public class ArrayList<T> implements ListInterface<T>, Serializable {

  private static final long serialVersionUID = 1L;

  private Object[] array;
  private int numberOfEntries;
  private static final int DEFAULT_CAPACITY = 5;

  public ArrayList() {
    this(DEFAULT_CAPACITY);
  }

  public ArrayList(int initialCapacity) {
    numberOfEntries = 0;
    array = new Object[Math.max(1, initialCapacity)];
  }

  @Override
  public boolean add(T newEntry) {
    // Appending keeps newly logged housekeeping tasks at the end of the list.
    if (isArrayFull()) {
      doubleArray();
    }

    array[numberOfEntries] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public boolean add(int newPosition, T newEntry) {
    boolean isSuccessful = true;

    if ((newPosition >= 1) && (newPosition <= numberOfEntries + 1)) {
      if (isArrayFull()) {
        doubleArray();
      }
      makeRoom(newPosition);
      array[newPosition - 1] = newEntry;
      numberOfEntries++;
    } else {
      isSuccessful = false;
    }

    return isSuccessful;
  }

  @Override
  public T remove(int givenPosition) {
    T result = null;

    if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
      result = getEntry(givenPosition);

      if (givenPosition < numberOfEntries) {
        removeGap(givenPosition);
      }

      numberOfEntries--;
      array[numberOfEntries] = null; // allow the removed object to be garbage-collected
    }

    return result;
  }

  @Override
  public void clear() {
    for (int index = 0; index < numberOfEntries; index++) {
      array[index] = null; // release references held by the backing array
    }
    numberOfEntries = 0;
  }

  @Override
  public boolean replace(int givenPosition, T newEntry) {
    boolean isSuccessful = true;

    if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
      array[givenPosition - 1] = newEntry;
    } else {
      isSuccessful = false;
    }

    return isSuccessful;
  }

  @Override
  public T getEntry(int givenPosition) {
    T result = null;

    if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
      @SuppressWarnings("unchecked")
      T entry = (T) array[givenPosition - 1];
      result = entry;
    }

    return result;
  }

  @Override
  public boolean contains(T anEntry) {
    boolean found = false;
    for (int index = 0; !found && (index < numberOfEntries); index++) {
      if (anEntry == null ? array[index] == null : anEntry.equals(array[index])) {
        found = true;
      }
    }
    return found;
  }

  @Override
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public boolean isFull() {
    return false;
  }

  private void doubleArray() {
    Object[] oldArray = array;
    array = new Object[oldArray.length * 2];
    System.arraycopy(oldArray, 0, array, 0, oldArray.length);
  }

  private boolean isArrayFull() {
    return numberOfEntries == array.length;
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < numberOfEntries; ++index) {
      output.append(array[index]).append('\n');
    }
    return output.toString();
  }

  /**
   * Task: Makes room for a new entry at newPosition. Precondition: 1 <=
   * newPosition <= numberOfEntries + 1; numberOfEntries is array's
   * numberOfEntries before addition.
   */
  private void makeRoom(int newPosition) {
    int newIndex = newPosition - 1;
    int lastIndex = numberOfEntries - 1;

    System.arraycopy(array, newIndex, array, newIndex + 1, lastIndex - newIndex + 1);
  }

  /**
   * Task: Shifts entries that are beyond the entry to be removed to the next
   * lower position. Precondition: array is not empty; 1 <= givenPosition <
   * numberOfEntries; numberOfEntries is array's numberOfEntries before removal.
   */
  private void removeGap(int givenPosition) {
    int removedIndex = givenPosition - 1;
    int lastIndex = numberOfEntries - 1;
    System.arraycopy(array, removedIndex + 1, array, removedIndex,
        lastIndex - removedIndex);
  }
}
