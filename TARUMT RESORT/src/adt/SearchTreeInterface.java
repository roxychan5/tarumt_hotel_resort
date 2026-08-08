package adt;

public interface SearchTreeInterface<KeyType extends Comparable<KeyType>, ValueType> {

  public boolean insert(KeyType key, ValueType value);

  public ValueType search(KeyType key);

  public boolean contains(KeyType key);

  public ListInterface<ValueType> inOrderTraversal();

  public int getNumberOfEntries();

  public boolean isEmpty();

  public void clear();
}
