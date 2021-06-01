package helpers;

import models.Event;
import models.ObjectClass;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class EventCollector extends Thread {

  private ObjectClass objectClass;
  private List<Event> events;

  public EventCollector(ObjectClass objectClass, List<Event> events) {
    this.objectClass = objectClass;
    this.events = events;
  }

  public static void createEventChunks(ThreadPoolExecutor executor, List<ObjectClass> objectClasses, List<Event> events) {
    System.out.print("Assigning events to object classes... ");
    List<EventCollector> eventCollectors = objectClasses.stream().map(table -> new EventCollector(table, events)).collect(Collectors.toList());
    List<Future<Void>> futures = ConcurrencyHelper.startAll(executor, eventCollectors);
    ConcurrencyHelper.syncAll(futures);
    System.out.println("done.");
    for (ObjectClass objectClass : objectClasses) {
      System.out.println("Detected " + objectClass.getRelevantEvents().size() + " relevant events for " + objectClass.getName());
    }
  }

  @Override
  public void run() {
    // Select all events that access at least one attribute of the object class
    List<String> columns = this.objectClass.getAttributeNames();
    this.objectClass.setRelevantEvents(events.stream().filter(event -> {
      Set<String> attributeNames = event.getAttributes().keySet();
      return attributeNames.stream().anyMatch(attributeName -> {
        for (String column : columns) {
          if (column.equals(attributeName)) {
            return true;
          }
        }
        return false;
      });
    }).collect(Collectors.toList()));
  }
}
