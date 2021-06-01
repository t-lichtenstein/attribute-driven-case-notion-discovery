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
  public static List<Event> load(String filePath, String columnSeparator, boolean debuggingMode) throws IOException {
    System.out.print("Importing CSV-file... ");

    int caseIdColumnIndex = -1;
    int timestampColumnIndex = 0;
    int activityColumnIndex = 1;
    int firstOptionalAttributeColumnIndex = 2;

    if (debuggingMode) {
      caseIdColumnIndex = 0;
      timestampColumnIndex = 1;
      activityColumnIndex = 2;
      firstOptionalAttributeColumnIndex = 3;
    }

    List<Event> events = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new FileReader(filePath));
    String row;
    row = reader.readLine();
    String[] columns = row.split(columnSeparator, -1);
    int eventId = 0;

    while ((row = reader.readLine()) != null) {
      String[] data = row.split(columnSeparator, -1);
      Map<String, String> attributes = new HashMap();
      String caseId = debuggingMode ? data[caseIdColumnIndex] : "";
      Long timestamp = Long.parseLong(data[timestampColumnIndex]);
      String activity = data[activityColumnIndex];
      for (int i = firstOptionalAttributeColumnIndex; i < data.length; i++) {
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
