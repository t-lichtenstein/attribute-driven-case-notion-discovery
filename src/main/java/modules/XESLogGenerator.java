package modules;

import models.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XESLogGenerator {

  public static void serializeLog(EventLog eventLog, ObjectClass objectClass, String filePath, String inputFileName) {
    System.out.print("Serializing log... ");
    Document document = new Document();
    Namespace nameSpace = Namespace.getNamespace("http://www.xes-standard.org");
    Element log = new Element("log", nameSpace);
    log.setAttribute("xes.version", "2.0");

    Element conceptExtension = new Element("extension", nameSpace);
    conceptExtension.setAttribute("uri", "http://www.xes-standard.org/concept.xesext");
    conceptExtension.setAttribute("prefix", "concept");
    conceptExtension.setAttribute("name", "Concept");
    log.addContent(conceptExtension);

    Element timeExtension = new Element("extension", nameSpace);
    timeExtension.setAttribute("uri", "http://www.xes-standard.org/time.xesext");
    timeExtension.setAttribute("prefix", "time");
    timeExtension.setAttribute("name", "Time");
    log.addContent(timeExtension);

    document.setRootElement(log);

    Pattern timestampPattern = Pattern.compile("\\[.*\\]");
    Pattern missingSecondsPattern = Pattern.compile("(\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d)([\\+\\-]\\d\\d:\\d\\d)");
    Pattern missingMillisecondsPattern = Pattern.compile("(\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d)([\\+\\-]\\d\\d:\\d\\d)");

    for (Trace trace : eventLog.getSources()) {
      Element logTrace = new Element("trace", nameSpace);
      log.addContent(logTrace);
      List<Event> events = trace.getEvents()
              .stream()
              .distinct()
              .sorted(Comparator.comparing(Event::getTimestamp))
              .collect(Collectors.toList());
      for (Event event : events) {
        Element logEvent = new Element("event", nameSpace);
        logTrace.addContent(logEvent);

        Element name = new Element("string", nameSpace);
        name.setAttribute("value", event.getActivity());
        name.setAttribute("key", "concept:name");
        logEvent.addContent(name);

        Element timestamp = new Element("date", nameSpace);
        String utcTimestamp = Instant.ofEpochMilli(event.getTimestamp()).atZone(ZoneId.systemDefault()).toString();
        Matcher timestampMatcher = timestampPattern.matcher(utcTimestamp);
        utcTimestamp = timestampMatcher.replaceAll("");

        Matcher missingSecondsMatcher = missingSecondsPattern.matcher(utcTimestamp);
        if (missingSecondsMatcher.matches()) {
          utcTimestamp = missingSecondsMatcher.replaceAll("$1:00$2");
        }

        Matcher missingMillisecondsMatcher = missingMillisecondsPattern.matcher(utcTimestamp);
        if (missingMillisecondsMatcher.matches()) {
          utcTimestamp = missingMillisecondsMatcher.replaceAll("$1.000$2");
        }

        timestamp.setAttribute("value", utcTimestamp);
        timestamp.setAttribute("key", "time:timestamp");

        for (Map.Entry<String,String> entry : event.getAttributes().entrySet()) {
          Element attribute = new Element("string", nameSpace);
          attribute.setAttribute("key", entry.getKey());
          attribute.setAttribute("value", entry.getValue());
          logEvent.addContent(attribute);
        }
        logEvent.addContent(timestamp);
      }
    }

    XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
    SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
    String dateString = format.format(new Date());
    String targetFilePath = Paths.get(filePath, "output_log-" + inputFileName.toLowerCase() + "-" + objectClass.getName().toLowerCase() + "-" + dateString + ".xes").toString();
    try {
      xmlOutputter.output(document, new FileWriter(targetFilePath));
      System.out.println("done.\n\nXES-log was stored in: " + targetFilePath);
    } catch (IOException e) {
      System.out.println("aborted.\n");
      e.printStackTrace();
    }
  }
}
