package models;

import java.util.List;
import java.util.stream.Collectors;

public class ProcessStep extends EstimationElement {

  private Event event;
  private String name;

  public ProcessStep(Event event) {
    this.event = event;
    String objectClassIds = this.getInstances()
        .stream()
        .map(instance -> String.valueOf(instance.getObjectClass().getId()))
        .sorted()
        .collect(Collectors.joining("|"));
    this.name = event.getActivity() + "-" + objectClassIds;
  }

  public Event getEvent() {
    return event;
  }

  public List<ObjectInstance> getInstances() {
    return this.event.getInstanceReferences();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public int getId() {
    return this.event.getId();
  }
}
