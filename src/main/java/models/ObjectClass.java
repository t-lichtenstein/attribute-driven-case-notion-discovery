package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ObjectClass {
  private final int id;
  private final String name;
  private List<String> attributeNames;
  private List<ObjectInstance> instances;
  private List<Event> relevantEvents;
  private Map<String, Column> attributes;
  private double highestSimilarityScore;
  private String lifecycle;

  public ObjectClass(int id, String name, List<String> attributeNames) {
    this.id = id;
    this.name = name;
    this.attributeNames = attributeNames;
    this.relevantEvents = new ArrayList<>();
    this.instances = new ArrayList<>();
    this.attributes = new HashMap();
    for (String columnName : attributeNames) {
      this.attributes.put(columnName, new Column(columnName));
    }
    this.highestSimilarityScore = 0;
  }

  public void setHighestSimilarityScore(double value) {
    this.highestSimilarityScore = value;
  }

  public double getHighestSimilarityScore() {
    return highestSimilarityScore;
  }

  public void setRelevantEvents(List<Event> events) {
    this.relevantEvents = events;
  }

  public List<Event> getRelevantEvents() {
    return relevantEvents;
  }

  public String getName() {
    return name;
  }

  public List<String> getAttributeNames() {
    return attributeNames;
  }

  public void setAttributeNames(List<String> attributeNames) {
    this.attributeNames = attributeNames;
  }

  public List<ObjectInstance> getInstances() {
    return instances;
  }

  public void setInstances(List<ObjectInstance> instances) {
    this.instances = instances;
  }

  public Map<String, Column> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Column> attributes) {
    this.attributes = attributes;
  }

  public String getLifecycle() {
    return lifecycle;
  }

  public void setLifecycle(String lifecycle) {
    this.lifecycle = lifecycle;
  }

  public int getId() {
    return id;
  }
}
