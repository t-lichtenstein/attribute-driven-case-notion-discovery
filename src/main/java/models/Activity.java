package models;

import java.util.ArrayList;
import java.util.List;

public class Activity {
  final String name;
  List<ActivityAttributeRelation> relations;

  public Activity(String name) {
    this.name = name;
    this.relations = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public List<ActivityAttributeRelation> getAttributeRelations() {
    return relations;
  }

  public void addAttributeRelation(ActivityAttributeRelation relation) {
    this.relations.add(relation);
  }
}