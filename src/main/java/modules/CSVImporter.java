package modules;

import models.Event;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVImporter {

  private static final int caseIdColumnIndex = 0;
  private static final int timestampColumnIndex = 1;
  private static final int activityColumnIndex = 2;

  public static List<Event> load(String filePath, String columnSeparator) throws IOException {
    System.out.print("Importing CSV-file... ");
    List<Event> events = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new FileReader(filePath));
    String row;
    row = reader.readLine();
    String[] columns = row.split(columnSeparator, -1);
    int eventId = 0;

    while ((row = reader.readLine()) != null) {
      String[] data = row.split(columnSeparator, -1);
      Map<String, String> attributes = new HashMap();
      String caseId = data[CSVImporter.caseIdColumnIndex];
      Long timestamp = Long.parseLong(data[CSVImporter.timestampColumnIndex]);
      String activity = data[CSVImporter.activityColumnIndex];
      for (int i = 3; i < data.length; i++) {
        if (!data[i].equals("")) {
          attributes.put(columns[i], data[i]);
        }
      }
      events.add(new Event(eventId, caseId, timestamp, activity, attributes));
      eventId++;
    }

    System.out.println("done.\nDetected " + events.size() + " events\n");
    return events;
  }
}
