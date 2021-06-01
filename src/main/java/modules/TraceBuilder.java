package modules;

import models.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TraceBuilder {

  public static EventLog buildEventLog(ObjectClass rootObjectClass, List<Event> events) {
    EventLog eventLog = new EventLog(events, rootObjectClass);
    eventLog.estimate();
    eventLog.sortEvents();
    return eventLog;
  }
}
