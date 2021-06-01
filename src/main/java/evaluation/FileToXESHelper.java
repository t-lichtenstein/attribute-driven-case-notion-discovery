package evaluation;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileToXESHelper {
  public static void main(String[] args) {
    String fname = "hb_mim";
    String fileLocation = "E:\\Repositories\\case-notion-detector\\src\\main\\resources\\csv-logs\\Evaluation\\Hospital_Billing\\" + fname + ".txt";

    List<List<String>> traces = new ArrayList<>();
    traces.add(new ArrayList<>());

    try {
      File myObj = new File(fileLocation);
      Scanner myReader = new Scanner(myObj);
      while (myReader.hasNextLine()) {
        String data = myReader.nextLine();
        if (data.equals("")) {
          traces.add(new ArrayList<>());
        } else {
          traces.get(traces.size() - 1).add(data);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

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

    for (List<String> trace : traces) {
      Element logTrace = new Element("trace", nameSpace);
      log.addContent(logTrace);
      for (String event : trace) {
        Element logEvent = new Element("event", nameSpace);
        logTrace.addContent(logEvent);

        Element name = new Element("string", nameSpace);
        name.setAttribute("value", event);
        name.setAttribute("key", "concept:name");
        logEvent.addContent(name);

        Element timestamp = new Element("date", nameSpace);
        String utcTimestamp = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toString();
        Matcher timestampMatcher = timestampPattern.matcher(utcTimestamp);
        utcTimestamp = timestampMatcher.replaceAll("");
        timestamp.setAttribute("value", utcTimestamp);
        timestamp.setAttribute("key", "time:timestamp");

        logEvent.addContent(timestamp);
      }
    }

    XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
    String targetFilePath = "E:\\Repositories\\case-notion-detector\\src\\main\\resources\\csv-logs\\Evaluation\\Hospital_Billing\\Results\\" + fname + ".xes";
    try {
      xmlOutputter.output(document, new FileWriter(new File(targetFilePath)));
      System.out.println("done.\n\nXES-log was stored in: " + targetFilePath);
    } catch (IOException e) {
      System.out.println("aborted.\n");
      e.printStackTrace();
    }
  }
}
