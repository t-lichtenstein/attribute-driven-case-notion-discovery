package models;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Map;

public class A2AMapping {

  private Map<String, Activity> activities;
  private Map<String, Attribute> attributes;
  private final Table<Activity, Attribute, Integer> activityAttributeRelations;

   public A2AMapping() {
     this.activityAttributeRelations = HashBasedTable.create();
   }

   public void incrementActivityToAttributeRelation(Activity activity, Attribute attribute) {
     if (!this.activityAttributeRelations.contains(activity, attribute)) {
       this.activityAttributeRelations.put(activity, attribute, 1);
       return;
     }
     int counter = this.activityAttributeRelations.get(activity, attribute);
     this.activityAttributeRelations.put(activity, attribute, counter + 1);
   }

   public int getRelationCounter(Activity activity, Attribute attribute) {
     if (!this.activityAttributeRelations.contains(activity, attribute)) {
       return 0;
     }
     return this.activityAttributeRelations.get(activity, attribute);
   }

  public Map<String, Activity> getActivities() {
    return activities;
  }

  public void setActivities(Map<String, Activity> activities) {
    this.activities = activities;
  }

  public Map<String, Attribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Attribute> attributes) {
    this.attributes = attributes;
  }
}
