package models;

import java.util.*;
import java.util.stream.Collectors;

public class EventLog extends TransitionMatrix<Trace, ProcessStep> {
  private ObjectClass rootClass;

  public EventLog(List<Event> events, ObjectClass rootClass) {
    super(events.stream().map(event -> new ProcessStep(event)).collect(Collectors.toList()), rootClass.getName() + " based Case Notion");
    this.rootClass = rootClass;
  }

  public void sortEvents() {
    this.getSources().forEach(trace -> trace.getEvents().sort(Comparator.comparing(Event::getTimestamp)));
  }

  @Override
  protected Trace getSuitableSource(List<Trace> activeSources, ProcessStep element) {

    List<ObjectInstance> instances = element.getInstances();

    Trace suitableTrace = null;

    // A trace is suitable if it already contains a reference to a data object instance that refers to the considered event
    for (Trace trace : activeSources) {
      if (instances.stream().anyMatch(trace::contains)) {
        suitableTrace = trace;
        break;
      }
    }

    if (suitableTrace == null && instances.stream().anyMatch(instance -> instance.getObjectClass().getId() == rootClass.getId())) {
      return null;
    }

    // If the event is unrelated to all other traces by means of data object instance references, the reference must be created otherwise
    if (suitableTrace == null) {
      double highestProbability = 0;
      List<Trace> relevantTraces = new ArrayList<>();

      for (Trace trace : activeSources) {
        double transitionProbability = this.getTransitionProbability(trace.getLastElement(), element);
        if (transitionProbability > highestProbability) {
          relevantTraces = new ArrayList<>();
          highestProbability = transitionProbability;
        }
        if (transitionProbability == highestProbability) {
          relevantTraces.add(trace);
        }
      }

      List<Integer> referencedObjectClassIds = element.getInstances().stream().map(instance -> instance.getObjectClass().getId()).collect(Collectors.toList());
      int minInstanceOccurrenceCount = 0;
      int traceInstanceOccurrences = 0;
      for (ObjectInstance instance : relevantTraces.get(0).getInstances()) {
        if (referencedObjectClassIds.contains(instance.getObjectClass().getId())) {
          traceInstanceOccurrences++;
        }
      }
      minInstanceOccurrenceCount = traceInstanceOccurrences;
      suitableTrace = relevantTraces.get(0);
      for (int i = 1; i < relevantTraces.size(); i++) {
        traceInstanceOccurrences = 0;
        for (ObjectInstance instance : relevantTraces.get(i).getInstances()) {
          if (referencedObjectClassIds.contains(instance.getObjectClass().getId())) {
            traceInstanceOccurrences++;
          }
        }
        if (traceInstanceOccurrences < minInstanceOccurrenceCount) {
          minInstanceOccurrenceCount = traceInstanceOccurrences;
          suitableTrace = relevantTraces.get(i);
        }
      }
    }

    return suitableTrace;
  }

  @Override
  protected Trace createSource() {
    return new Trace();
  }
}
