package adt;

public class ArrayPriorityQueue<T extends Comparable<T>>
    implements PriorityQueueInterface<T> {

  private static final int DEFAULT_CAPACITY = 10;
  private T[] entries;
  private int numberOfEntries;

  @SuppressWarnings("unchecked")
  public ArrayPriorityQueue() {
    entries = (T[]) new Comparable[DEFAULT_CAPACITY];
  }

  @Override
  public boolean add(T newEntry) {
    if (newEntry == null) {
      return false;
    }
    ensureCapacity();
    int index = numberOfEntries;
    // Shift lower-priority entries right. Equal entries stay FIFO.
    while (index > 0 && newEntry.compareTo(entries[index - 1]) > 0) {
      entries[index] = entries[index - 1];
      index--;
    }
    entries[index] = newEntry;
    numberOfEntries++;
    return true;
  }

  @Override
  public T remove() {
    if (isEmpty()) {
      return null;
    }
    T front = entries[0];
    for (int index = 1; index < numberOfEntries; index++) {
      entries[index - 1] = entries[index];
    }
    entries[--numberOfEntries] = null;
    return front;
  }

  /** Removes an entry by one-based queue position while preserving queue order. */
  public T remove(int position) {
    if (position < 1 || position > numberOfEntries) return null;
    T removed = entries[position - 1];
    for (int index = position; index < numberOfEntries; index++) {
      entries[index - 1] = entries[index];
    }
    entries[--numberOfEntries] = null;
    return removed;
  }

  @Override
  public T getFront() {
    return isEmpty() ? null : entries[0];
  }

  /** Returns an entry using a one-based position for report generation. */
  public T getEntry(int position) {
    return position < 1 || position > numberOfEntries ? null : entries[position - 1];
  }

  @Override
  public int getNumberOfEntries() {
    return numberOfEntries;
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @SuppressWarnings("unchecked")
  private void ensureCapacity() {
    if (numberOfEntries == entries.length) {
      T[] expanded = (T[]) new Comparable[entries.length * 2];
      for (int index = 0; index < entries.length; index++) {
        expanded[index] = entries[index];
      }
      entries = expanded;
    }
  }
}