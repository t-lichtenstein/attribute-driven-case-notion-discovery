package models;

import java.util.Set;

public class TableCandidate {
  private final int id;
  private final Set<String> keys;
  private final Set<String> attributes;

  public TableCandidate(int id, Set<String> keys, Set<String> attributes) {
    this.id = id;
    this.keys = keys;
    this.attributes = attributes;
  }

  public void deleteKey(String key) {
    this.keys.remove(key);
  }

  public Set<String> getKeys() {
    return keys;
  }

  public Set<String> getAttributes() {
    return attributes;
  }

  public int getId() {
    return id;
  }
}
