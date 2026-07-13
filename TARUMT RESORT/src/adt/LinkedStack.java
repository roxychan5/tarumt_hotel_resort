package adt;

import java.io.Serializable;

/**
 * A linked implementation of the ADT stack.
 * Adapted from Frank M. Carrano, Data Structures and Algorithms in Java.
 *
 * @author Your Name
 */
public class LinkedStack<T> implements StackInterface<T>, Serializable {

  private Node topNode;

  public LinkedStack() {
    topNode = null;
  }

  @Override
  public void push(T newEntry) {
    Node newNode = new Node(newEntry, topNode);
    topNode = newNode;
  }

  @Override
  public T pop() {
    T result = null;
    if (!isEmpty()) {
      result = topNode.data;
      topNode = topNode.next;
    }
    return result;
  }

  @Override
  public T peek() {
    T result = null;
    if (!isEmpty()) {
      result = topNode.data;
    }
    return result;
  }

  @Override
  public boolean isEmpty() {
    return topNode == null;
  }

  @Override
  public boolean isFull() {
    return false;
  }

  @Override
  public void clear() {
    topNode = null;
  }

  private class Node implements Serializable {

    private final T data;
    private Node next;

    private Node(T data, Node next) {
      this.data = data;
      this.next = next;
    }
  }
}
