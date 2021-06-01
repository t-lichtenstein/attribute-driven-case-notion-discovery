package models;

public class ActivityAttributeRelation {
  private final Activity activity;
  private final Attribute attribute;
  private final int relationCounter;

  public ActivityAttributeRelation(Activity activity, Attribute attribute, int relationCounter) {
    this.activity = activity;
    this.attribute = attribute;
    this.relationCounter = relationCounter;
  }

  public Activity getActivity() {
    return activity;
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public int getRelationCounter() {
    return relationCounter;
  }
}
