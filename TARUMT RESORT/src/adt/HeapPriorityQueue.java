package adt;

public class HeapPriorityQueue<T extends Comparable<T>>
    implements PriorityQueueInterface<T> {

  private static final int DEFAULT_CAPACITY = 10;
  private T[] entries;
  private int numberOfEntries;

  @SuppressWarnings("unchecked")
  public HeapPriorityQueue() {
    entries = (T[]) new Comparable[DEFAULT_CAPACITY];
  }

  @Override
  public boolean add(T newEntry) {
    if (newEntry == null) return false;
    ensureCapacity();
    entries[numberOfEntries] = newEntry;
    siftUp(numberOfEntries++);
    return true;
  }

  @Override
  public T remove() {
    if (isEmpty()) return null;
    T removed = entries[0];
    entries[0] = entries[--numberOfEntries];
    entries[numberOfEntries] = null;
    if (!isEmpty()) siftDown(0);
    return removed;
  }

  // Removes a matching entry and restores the heap property. //
  public boolean removeEntry(T entry) {
    for (int index = 0; index < numberOfEntries; index++) {
      if (entries[index].compareTo(entry) == 0) {
        entries[index] = entries[--numberOfEntries];
        entries[numberOfEntries] = null;
        if (index < numberOfEntries) {
          siftUp(index);
          siftDown(index);
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public T getFront() {
    return isEmpty() ? null : entries[0];
  }

  // Returns the heap entry at a one-based position for reporting and filtering. //
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

  private void siftUp(int index) {
    while (index > 0) {
      int parent = (index - 1) / 2;
      if (entries[index].compareTo(entries[parent]) <= 0) break;
      swap(index, parent);
      index = parent;
    }
  }

  private void siftDown(int index) {
    while (true) {
      int left = index * 2 + 1;
      int right = left + 1;
      int largest = index;
      if (left < numberOfEntries && entries[left].compareTo(entries[largest]) > 0) largest = left;
      if (right < numberOfEntries && entries[right].compareTo(entries[largest]) > 0) largest = right;
      if (largest == index) return;
      swap(index, largest);
      index = largest;
    }
  }

  private void swap(int first, int second) {
    T temporary = entries[first];
    entries[first] = entries[second];
    entries[second] = temporary;
  }

  @SuppressWarnings("unchecked")
  private void ensureCapacity() {
    if (numberOfEntries == entries.length) {
      T[] expanded = (T[]) new Comparable[entries.length * 2];
      for (int index = 0; index < entries.length; index++) expanded[index] = entries[index];
      entries = expanded;
    }
  }
}
