package models;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Trace extends SourceSequence<ProcessStep> {

  private Set<ObjectInstance> instances;

  public Trace() {
    this.instances = new HashSet<>();
  }

  public boolean contains(ObjectInstance instance) {
    return this.instances.stream().anyMatch(i -> i.getId() == instance.getId());
  }

  @Override
  public void add(ProcessStep processStep) {
    super.add(processStep);
    this.instances.addAll(processStep.getInstances()
        .stream()
        .filter(instance -> instance.getFirstElement().getId() == processStep.getEvent().getId())
        .collect(Collectors.toList()));
  }

  public List<Event> getEvents() {
    return this.getElements().stream().map(processStep -> processStep.getEvent()).collect(Collectors.toList());
  }

  public Set<ObjectInstance> getInstances() {
    return instances;
  }
}
