package adt;

/**
 * Interface for the ADT stack.
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
}
