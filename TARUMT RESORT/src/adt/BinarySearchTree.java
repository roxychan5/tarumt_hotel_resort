package adt;

import java.io.Serializable;

public class BinarySearchTree<KeyType extends Comparable<KeyType>, ValueType>
    implements SearchTreeInterface<KeyType, ValueType>, Serializable {

  private Node<KeyType, ValueType> root;
  private int numberOfEntries;

  @Override
  public boolean insert(KeyType key, ValueType value) {
    if (key == null) {
      return false;
    }

    InsertResult result = new InsertResult();
    root = insert(root, key, value, result);
    if (result.inserted) {
      numberOfEntries++;
    }
    return result.inserted;
  }

  @Override
  public ValueType search(KeyType key) {
    Node<KeyType, ValueType> currentNode = root;
    while (currentNode != null) {
      int comparison = key.compareTo(currentNode.key);
      if (comparison == 0) {
        return currentNode.value;
      } else if (comparison < 0) {
        currentNode = currentNode.left;
      } else {
        currentNode = currentNode.right;
      }
    }
    return null;
  }

  @Override
  public boolean contains(KeyType key) {
    return search(key) != null;
  }

  @Override
  public ListInterface<ValueType> inOrderTraversal() {
    ListInterface<ValueType> entries = new ArrayList<>();
    addInOrder(root, entries);
    return entries;
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
  public void clear() {
    root = null;
    numberOfEntries = 0;
  }

  private Node<KeyType, ValueType> insert(Node<KeyType, ValueType> currentNode,
      KeyType key, ValueType value, InsertResult result) {
    if (currentNode == null) {
      result.inserted = true;
      return new Node<>(key, value);
    }

    int comparison = key.compareTo(currentNode.key);
    if (comparison < 0) {
      currentNode.left = insert(currentNode.left, key, value, result);
    } else if (comparison > 0) {
      currentNode.right = insert(currentNode.right, key, value, result);
    } else {
      currentNode.value = value;
    }
    updateHeight(currentNode);
    return rebalance(currentNode);
  }

  private void addInOrder(Node<KeyType, ValueType> currentNode,
      ListInterface<ValueType> entries) {
    if (currentNode != null) {
      addInOrder(currentNode.left, entries);
      entries.add(currentNode.value);
      addInOrder(currentNode.right, entries);
    }
  }

  private static class Node<KeyType, ValueType> implements Serializable {

    private final KeyType key;
    private ValueType value;
    private Node<KeyType, ValueType> left;
    private Node<KeyType, ValueType> right;
    private int height = 1;

    private Node(KeyType key, ValueType value) {
      this.key = key;
      this.value = value;
    }
  }

  private int getHeight(Node<KeyType, ValueType> currentNode) {
    return currentNode == null ? 0 : currentNode.height;
  }

  private void updateHeight(Node<KeyType, ValueType> currentNode) {
    currentNode.height = Math.max(getHeight(currentNode.left), getHeight(currentNode.right)) + 1;
  }

  private int getBalance(Node<KeyType, ValueType> currentNode) {
    return currentNode == null ? 0 : getHeight(currentNode.left) - getHeight(currentNode.right);
  }

  private Node<KeyType, ValueType> rebalance(Node<KeyType, ValueType> currentNode) {
    int balance = getBalance(currentNode);

    if (balance > 1) {
      if (getBalance(currentNode.left) < 0) {
        currentNode.left = rotateLeft(currentNode.left);
      }
      return rotateRight(currentNode);
    }

    if (balance < -1) {
      if (getBalance(currentNode.right) > 0) {
        currentNode.right = rotateRight(currentNode.right);
      }
      return rotateLeft(currentNode);
    }

    return currentNode;
  }

  private Node<KeyType, ValueType> rotateRight(Node<KeyType, ValueType> currentNode) {
    Node<KeyType, ValueType> newRoot = currentNode.left;
    Node<KeyType, ValueType> shiftedSubtree = newRoot.right;

    newRoot.right = currentNode;
    currentNode.left = shiftedSubtree;

    updateHeight(currentNode);
    updateHeight(newRoot);
    return newRoot;
  }

  private Node<KeyType, ValueType> rotateLeft(Node<KeyType, ValueType> currentNode) {
    Node<KeyType, ValueType> newRoot = currentNode.right;
    Node<KeyType, ValueType> shiftedSubtree = newRoot.left;

    newRoot.left = currentNode;
    currentNode.right = shiftedSubtree;

    updateHeight(currentNode);
    updateHeight(newRoot);
    return newRoot;
  }

  private static class InsertResult {

    private boolean inserted;
  }
}
