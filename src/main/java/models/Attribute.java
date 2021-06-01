package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Attribute {
  final String name;
  List<ActivityAttributeRelation> relations;
  Set<Integer> eventIds;

  public Attribute(String name) {
    this.name = name;
    this.relations = new ArrayList<>();
    this.eventIds = new HashSet<>();
  }

  public void addEvent(Event event) {
    this.eventIds.add(event.getId());
  }

  public Set<Integer> getEventIds() {
    return this.eventIds;
  }

  public String getName() {
    return name;
  }

  public List<ActivityAttributeRelation> getActivityRelations() {
    return relations;
  }

  public void addActivityRelation(ActivityAttributeRelation relation) {
    this.relations.add(relation);
  }
}

