package modules;

import models.A2AMapping;
import models.Event;
import models.ObjectClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SchemaDetectorTest {

  @Test
  public void emptyInput() {
    // Empty list
    List<Event> events = new ArrayList();
    List<ObjectClass> result = SchemaDetector.extractTables(events);
    assertEquals(0, result.size());
  }

  @Test
  public void isolatedActivityIsolatedAttributeRelations() {
    // isolated activity and isolated attribute
    List<Event> events = new ArrayList();
    events.add(new Event(0, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_1", "test_value"); }}));
    events.add(new Event(1, "0", 0L, "test_activity_2", new HashMap() {{ put("test_column_2", "test_value"); }}));

    A2AMapping mapping = SchemaDetector.generateA2AMapping(events);
    List<ObjectClass> objectClasses = new ArrayList<>();
    SchemaDetector.detectIsolatedActivityIsolatedAttributeRelations(mapping, objectClasses);
    assertEquals(events.size(), objectClasses.size());
    assertEquals("test_column_1", objectClasses.get(0).getAttributeNames().get(0));
    assertEquals(1, objectClasses.get(0).getAttributeNames().size());
    assertEquals("test_column_2", objectClasses.get(1).getAttributeNames().get(0));
    assertEquals(1, objectClasses.get(1).getAttributeNames().size());
  }

  @Test
  public void nonIsolatedActivityIsolatedAttributeRelations() {
    // Non-isolated activity and isolated attribute
    List<Event> events = new ArrayList();
    Map<String, String> attribute = new HashMap() {{
      put("test_column_1", "test_value");
    }};
    events.add(new Event(0, "0", 0L, "test_activity_1", attribute));
    events.add(new Event(1, "0", 0L, "test_activity_1", attribute));
    events.add(new Event(2, "0", 0L, "test_activity_1", attribute));
    events.add(new Event(3, "0", 0L, "test_activity_2", attribute));
    events.add(new Event(4, "0", 0L, "test_activity_2", attribute));
    events.add(new Event(5, "0", 0L, "test_activity_3", attribute));

    A2AMapping mapping = SchemaDetector.generateA2AMapping(events);
    List<ObjectClass> objectClasses = new ArrayList<>();
    SchemaDetector.detectNonIsolatedActivityIsolatedAttributeRelations(mapping, objectClasses);
    assertEquals(1, objectClasses.size());
    assertEquals("test_column_1", objectClasses.get(0).getAttributeNames().get(0));
    assertEquals(1, objectClasses.get(0).getAttributeNames().size());
  }

  @Test
  public void isolatedActivityNonIsolatedAttributeRelations() {
    // Isolated activity and non-isolated attribute
    List<Event> events = new ArrayList();
    events.add(new Event(0, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_1", "test_value"); put("test_column_2", "test_value"); }}));
    events.add(new Event(1, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_1", "test_value"); put("test_column_2", "test_value"); }}));
    events.add(new Event(1, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_1", "test_value"); }}));
    events.add(new Event(2, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_3", "test_value"); put("test_column_4", "test_value");  }}));
    events.add(new Event(3, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_5", "test_value"); }}));

    A2AMapping mapping = SchemaDetector.generateA2AMapping(events);
    List<ObjectClass> objectClasses = new ArrayList<>();
    SchemaDetector.detectIsolatedActivityNonIsolatedAttributeRelations(mapping, objectClasses);
    assertEquals(3, objectClasses.size());
  }

  @Test
  public void NonIsolatedActivityNonIsolatedAttributeRelations() {
    // Isolated activity and non-isolated attribute
    List<Event> events = new ArrayList();
    events.add(new Event(0, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_1", "test_value"); put("test_column_2", "test_value"); }}));
    events.add(new Event(1, "0", 0L, "test_activity_1", new HashMap() {{ put("test_column_1", "test_value"); put("test_column_2", "test_value"); }}));
    events.add(new Event(2, "0", 0L, "test_activity_2", new HashMap() {{ put("test_column_1", "test_value"); put("test_column_2", "test_value"); }}));
    events.add(new Event(3, "0", 0L, "test_activity_3", new HashMap() {{ put("test_column_2", "test_value"); }}));
    events.add(new Event(4, "0", 0L, "test_activity_3", new HashMap() {{ put("test_column_2", "test_value"); }}));
    events.add(new Event(5, "0", 0L, "test_activity_3", new HashMap() {{ put("test_column_2", "test_value"); }}));

    A2AMapping mapping = SchemaDetector.generateA2AMapping(events);
    List<ObjectClass> objectClasses = new ArrayList<>();
    SchemaDetector.detectNonIsolatedActivityNonIsolatedAttributeRelations(mapping, objectClasses);
    assertEquals(3, objectClasses.size());
  }

  @Test
  public void noKeyMergeTest() {
    List<ObjectClass> objectClasses = new ArrayList<>();
    objectClasses.add(new ObjectClass(1, "Table_1", new ArrayList() {{ add("subject_id"); add("birthday"); add("gender"); }}));
    objectClasses.add(new ObjectClass(2, "Table_2", new ArrayList() {{ add("duration"); add("medication"); add("status"); }}));
    List<ObjectClass> result = SchemaDetector.mergeTables(objectClasses);
    assertEquals(2, result.size());
  }

  @Test
  public void complexMergeTest() {
    List<ObjectClass> objectClasses = new ArrayList<>();
    objectClasses.add(new ObjectClass(1, "Table_1", new ArrayList() {{ add("subject_id"); add("hadm_id"); add("location"); }}));
    objectClasses.add(new ObjectClass(2, "Table_2", new ArrayList() {{ add("drug"); add("dose_val_rx"); add("pharmacy_id"); add("dose_unit_rx"); }}));
    objectClasses.add(new ObjectClass(3, "Table_3", new ArrayList() {{ add("subject_id"); add("gender"); add("dod"); }}));
    objectClasses.add(new ObjectClass(4, "Table_4", new ArrayList() {{ add("drug"); add("dose_val_rx"); add("pharmacy_id"); add("dose_unit_rx"); }}));
    objectClasses.add(new ObjectClass(5, "Table_5", new ArrayList() {{ add("transfer_id"); add("eventtype"); add("hadm_id"); }}));
    objectClasses.add(new ObjectClass(6, "Table_6", new ArrayList() {{ add("transfer_id"); add("eventtype"); }}));
    objectClasses.add(new ObjectClass(7, "Table_7", new ArrayList() {{ add("hadm_id"); add("location"); }}));
    objectClasses.add(new ObjectClass(8, "Table_8", new ArrayList() {{ add("medication"); add("frequency"); add("pharmacy_id"); add("status"); }}));
    objectClasses.add(new ObjectClass(9, "Table_8", new ArrayList() {{ add("medication"); add("frequency"); add("pharmacy_id"); add("hadm_id"); add("status"); }}));
    List<ObjectClass> result = SchemaDetector.mergeTables(objectClasses);
    assertEquals(5, result.size());
  }
}
