package models;

import java.util.ArrayList;
import java.util.List;

public class Column {

  private final String name;
  private final List<Entry> entries;
  private double similarityWeight;

  public Column(String name) {
    this.name = name;
    this.entries = new ArrayList<>();
    this.similarityWeight = 0;
  }

  public int size() {
    return this.getEntries().size();
  }

  public void add(Event event, String value) {
    this.entries.add(new Entry(event, value));
  }

  public String getName() {
    return name;
  }

  public List<Entry> getEntries() {
    return entries;
  }

  public double getSimilarityWeight() {
    return similarityWeight;
  }

  public void setSimilarityWeight(double similarityWeight) {
    this.similarityWeight = similarityWeight;
  }
}
