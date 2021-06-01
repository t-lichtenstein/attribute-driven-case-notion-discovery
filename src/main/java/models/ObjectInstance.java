package models;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ObjectInstance extends SourceSequence<Event> {

  public static int idCounter = 0;
  public static Lock lock = new ReentrantLock();

  public static int assignId() {
    try {
      ObjectInstance.lock.lock();
      ObjectInstance.idCounter++;
      return ObjectInstance.idCounter;
    } finally {
      ObjectInstance.lock.unlock();
    }
  }

  private final int id;
  private final Map<String, String> state;
  private final List<Column> columns;
  private final ObjectClass objectClass;

  public ObjectInstance(List<Column> columns, ObjectClass objectClass) {
    this.state = new HashMap<>();
    this.id = ObjectInstance.assignId();
    this.objectClass = objectClass;
    this.columns = columns;
  }

  @Override
  public void add(Event event) {
    super.add(event);
    event.getAttributes().entrySet().forEach(entry -> {
      this.state.put(entry.getKey(), entry.getValue());
    });
  }

  public double evaluateSimilarity(Event event) {
    if (this.getObjectClass().getHighestSimilarityScore() == 0) {
      return 0;
    }

    double score = 0;

    for (Column column : this.getColumns()) {
      String eventValue = event.getAttributes().get(column.getName());
      String instanceValue = this.getState().get(column.getName());
      if (eventValue != null && eventValue.equals(instanceValue)) {
        score += column.getSimilarityWeight();
      }
    }

    return score / this.getObjectClass().getHighestSimilarityScore();
  }

  public List<Column> getColumns() {
    return columns;
  }

  public boolean containsActivity(Event event) {
    return this.getElements().stream().anyMatch(instanceEvent -> instanceEvent.getActivity().equals(event.getActivity()));
  }

  public int getId() {
    return id;
  }

  public Map<String, String> getState() {
    return state;
  }

  public ObjectClass getObjectClass() {
    return objectClass;
  }
}
