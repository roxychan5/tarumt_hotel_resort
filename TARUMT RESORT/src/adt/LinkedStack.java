package adt;

import java.io.Serializable;

/**
 * Linked implementation of the Linear Stack ADT. Housekeeping pushes each room-status change and pops the latest record for single or bulk rollback.
 * Adapted from Data Structures and Algorithms in Java.
 *
 * @author Chan Rou Xuan
 */
public class LinkedStack<T> implements StackInterface<T>, Serializable {

  private Node topNode;
  private int size;

  public LinkedStack() {
    topNode = null;
    size = 0;
  }

  @Override
  public void push(T newEntry) {
    // Insert at the top so the newest status change is processed first (LIFO).
    Node newNode = new Node(newEntry, topNode);
    topNode = newNode;
    size++;
  }

  @Override
  public T pop() {
    // Remove only the top entry; this preserves LIFO rollback behaviour.
    T result = null;
    if (!isEmpty()) {
      result = topNode.data;
      topNode = topNode.next;
      size--;
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
    size = 0;
  }

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public void popMultiple(int count) {
    for (int i = 0; i < count && !isEmpty(); i++) {
      pop();
    }
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
