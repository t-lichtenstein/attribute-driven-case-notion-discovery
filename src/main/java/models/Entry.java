package models;

public class Entry {
  private final Event event;
  private final String value;

  public Entry(Event event, String value) {
    this.event = event;
    this.value = value;
  }

  public Event getEvent() {
    return event;
  }

  public String getValue() {
    return value;
  }
}
