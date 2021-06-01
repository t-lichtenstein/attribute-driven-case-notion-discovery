package helpers;

import models.Event;
import models.ObjectInstance;
import models.ObjectClass;
import models.TransitionMatrix;

import java.util.*;
import java.util.stream.Collectors;

// Concept inspired by: https://doi.org/10.1007/978-3-642-03848-8_11
// Implementation inspired by: https://github.com/diogoff/unlabelled-event-logs
public class ObjectLifecycleDetector extends TransitionMatrix<ObjectInstance, Event> {

  private ObjectClass objectClass;
  private final double similarityThreshold;

  public ObjectLifecycleDetector(ObjectClass objectClass, double similarityThreshold) {
    super(objectClass.getRelevantEvents(), objectClass.getName());
    this.objectClass = objectClass;
    this.similarityThreshold = similarityThreshold;
  }

  @Override
  protected ObjectInstance getSuitableSource(List<ObjectInstance> activeSources, Event element) {
    // Identify suitable object instance for event
    ObjectInstance suitableEventInstance = null;

    // Determine instance candidates with the highest similarity and probability
    double similarityScore = 0;
    double similarityTransitionProbability = 0;
    List<ObjectInstance> similarityInstanceCandidates = new ArrayList<>();

    for (ObjectInstance instance : this.getSources()) {
      // Do not add event to instance with first event having the same activity
      if (instance.getFirstElement().getActivity().equals(element.getActivity())) {
        continue;
      }

      double score = instance.evaluateSimilarity(element);
      if (score > similarityThreshold && score > similarityScore) {
        similarityScore = score;
        similarityTransitionProbability = this.getTransitionProbability(instance.getLastElement(), element);
        similarityInstanceCandidates = new ArrayList<>();
      }

      // Score equals similarity score
      if (score == similarityScore) {

        // Transition probability is higher than previous set
        if (this.getTransitionProbability(instance.getFirstElement(), element) > similarityTransitionProbability) {
          similarityTransitionProbability = this.getTransitionProbability(instance.getLastElement(), element);
          similarityInstanceCandidates = new ArrayList<>();
        }

        // Add instance to similarity event source candidates
        similarityInstanceCandidates.add(instance);
      }
    }

    // Identify object instance candidate having the minimal number of events with the same activity as the considered event
    OptionalLong minActivityOccurrences = similarityInstanceCandidates.stream()
        .mapToLong(instance -> instance.getElements().stream()
            .filter(instanceEvent -> instanceEvent.getActivity().equals(element.getActivity()))
            .count())
        .min();

    // Select object instance with minimal activity occurrences
    if (minActivityOccurrences.isPresent()) {
      Optional<ObjectInstance> optionalEventSource = similarityInstanceCandidates.stream()
          .filter(instance -> instance.getElements().stream()
              .filter(instanceEvent -> instanceEvent.getActivity().equals(element.getActivity()))
              .count() == minActivityOccurrences.getAsLong())
          .findFirst();
      if (optionalEventSource.isPresent()) {
        // Assign event to instance based on similarity
        return optionalEventSource.get();
      }
    }

    // No instance candidates found based on similarity

    // Assign Event based on Activity Transition Probability

    double probabilityScore = 0;

    for (ObjectInstance objectInstance : this.getSources()) {
      if (objectInstance.containsActivity(element)) {
        continue;
      }
      Event predecessor = objectInstance.getLastElement();
      double transitionProbability = this.getTransitionProbability(predecessor, element);
      if (transitionProbability > probabilityScore) {
        probabilityScore = transitionProbability;
        suitableEventInstance = objectInstance;
      }
    }

    // Create new source if no suitable source was found or start transition probability is higher
    if (this.getTransitionProbability(this.startActivity, this.getTransitionEntryMap().get(element.getName())) > probabilityScore) {
      return null;
    }

    return suitableEventInstance;
  }

  @Override
  protected ObjectInstance createSource() {
    return new ObjectInstance(this.objectClass.getAttributes().values().stream().collect(Collectors.toList()), this.objectClass);
  }

  @Override
  protected void postProcessing() {
    this.objectClass.setInstances(this.getSources());
    this.objectClass.setLifecycle(this.getProcess());
  }
}
