package adt;

public interface PriorityQueueInterface<T> {

  boolean add(T newEntry);

  T remove();

  T getFront();

  int getNumberOfEntries();

  boolean isEmpty();
}
