package evaluation;

import info.debatty.java.stringsimilarity.Levenshtein;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GSimScoreCalculator {

  private static List<List<String>> parseXesLogToSequences(String path) {
    File xesLog = new File(path);

    if (!xesLog.exists()) {
      System.out.println("The specified xes log does not exist");
      System.exit(1);
    }

    XParser parser = new XesXmlParser();

    if (!parser.canParse(xesLog)) {
      System.out.println("The specified xes log can not be parsed");
      System.exit(1);
    }

    System.out.print("Start parsing log \"" + path + "\"... ");

    List<XLog> logs = new ArrayList<>();

    try {
      logs = parser.parse(xesLog);
    } catch (Exception e) {
      System.out.println("\nAn error occurred when parsing the log");
      e.printStackTrace();
    }

    if (logs.size() == 0) {
      System.out.println("\nCould not identify an xes log in the given file");
      System.exit(1);
    }

    System.out.println("Done");


    List<XTrace> log = logs.get(0);

    List<List<String>> result = new ArrayList<>();
    for (XTrace xTrace : log) {
      List<String> trace = new ArrayList<>();
      for (XEvent xEvent : xTrace) {
        trace.add(xEvent.getAttributes().get("concept:name").toString());
      }
      result.add(trace);
    }
    return result;
  }

  public static List<List<String>> parseCsvLogToSequences(String path, String delimiter) {
    File csvLog = new File(path);

    if (!csvLog.exists()) {
      System.out.println("The specified csv log does not exist");
      System.exit(1);
    }

    Map<String, List<String>> traces = new HashMap<>();

    try {
    BufferedReader br = new BufferedReader(new FileReader(csvLog));
      String line;
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] values = line.split(delimiter);

        List<String> trace = traces.getOrDefault(values[0], new ArrayList<>());
        trace.add(values[2]);
        traces.put(values[0], trace);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<List<String>> result = new ArrayList<>();

    for (String traceId : traces.keySet()) {
      result.add(traces.get(traceId));
    }

    return result;
  }

  // Normalized Levenshtein Distance Metric based on: https://doi.org/10.1109/TPAMI.2007.1078
  private static double similarityScore(String a, String b) {
    Levenshtein l = new Levenshtein();
    double distance = l.distance(a, b);
    return 1 - ((2 * distance) / (a.length() + b.length() + distance));
  }

  public static void main(String[] args) {
    List<List<String>> originalLog = parseCsvLogToSequences("", "");
    List<List<String>> estimatedLog = parseXesLogToSequences("");

    // Transform traces to symbol sequences
    String[] substitutes = new String[] {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
    Map<String,String> substitutionMap = new HashMap<>();

    // Determine unique activities for both logs
    Set<String> activitySet = new HashSet<>();

    for (List<String> originalTrace : originalLog) {
      for (String originalActivity : originalTrace) {
        activitySet.add(originalActivity);
      }
    }

    for (List<String> estimatedTrace : originalLog) {
      for (String estimatedActivity : estimatedTrace) {
        activitySet.add(estimatedActivity);
      }
    }

    List<String> activities = new ArrayList<>(activitySet);

    for (int i = 0; i < activities.size(); i++) {
      substitutionMap.put(activities.get(i),substitutes[i]);
    }

    // Create activity sequences for original log
    List<String> originalSequences = originalLog.stream().map(trace -> {
      String sequence = "";
      for (String activity : trace) {
        sequence += substitutionMap.get(activity);
      }
      return sequence;
    }).collect(Collectors.toList());

    // Determine variants for original log
    Map<String, Integer> originalVariants = new HashMap<>();
    for (String traceSequence : originalSequences) {
      Integer occurrences = originalVariants.get(traceSequence);
      if (occurrences == null) {
        occurrences = 0;
      }
      originalVariants.put(traceSequence, occurrences + 1);
    }

    Map<String, Double> originalVariantsPercentages = new HashMap<>();
    originalVariants.forEach((trace, count) -> originalVariantsPercentages.put(trace, (double) count / (double) originalSequences.size()));

    // Create activity sequences for estimated log
    List<String> estimatedSequences = estimatedLog.stream().map(trace -> {
      String sequence = "";
      for (String activity : trace) {
        sequence += substitutionMap.get(activity);
      }
      return sequence;
    }).collect(Collectors.toList());

    // Determine variants for estimated log
    Map<String, Integer> estimatedVariants = new HashMap<>();
    for (String traceSequence : estimatedSequences) {
      Integer occurrences = estimatedVariants.get(traceSequence);
      if (occurrences == null) {
        occurrences = 0;
      }
      estimatedVariants.put(traceSequence, occurrences + 1);
    }

    Map<String, Double> estimatedVariantsPercentages = new HashMap<>();
    estimatedVariants.forEach((trace, count) -> estimatedVariantsPercentages.put(trace, (double) count / (double) estimatedSequences.size()));

    Map<String, String> estimatedToOriginalVariantMap = new HashMap<>();
    Map<String, List<String>> originalToEstimatedVariantMap = new HashMap<>();
    Map<String, Double> estimatedToOriginalSimilarity = new HashMap<>();

    List<Double> percentages = new ArrayList<>();

    // Identify affiliations between estimated and original variants
    for (String estimatedTrace: estimatedVariants.keySet()) {
      double maxSimilarity = 0;
      String matchedTrace = "";
      for (String originalTrace : originalVariants.keySet()) {
        double similarity = GSimScoreCalculator.similarityScore(estimatedTrace, originalTrace);
        if (similarity > maxSimilarity) {
          maxSimilarity = similarity;
          matchedTrace = originalTrace;
        }
      }

      estimatedToOriginalVariantMap.put(estimatedTrace, matchedTrace);
      estimatedToOriginalSimilarity.put(estimatedTrace, maxSimilarity);

      List<String> otherEstimatedTraces = originalToEstimatedVariantMap.get(matchedTrace);
      if (otherEstimatedTraces == null) {
        otherEstimatedTraces = new ArrayList<>();
      }
      otherEstimatedTraces.add(estimatedTrace);
      originalToEstimatedVariantMap.put(matchedTrace, otherEstimatedTraces);

      percentages.add(maxSimilarity);
    }

    double result = 0;

    // Determine G-sim-score
    for (String originalVariant : originalVariants.keySet()) {
      double originalDistribution = originalVariantsPercentages.get(originalVariant);
      List<String> relatedEstimatedVariants = originalToEstimatedVariantMap.get(originalVariant);

      double estimationDistribution = 0;

      if (relatedEstimatedVariants != null) {
        for (String estimatedVariant : relatedEstimatedVariants) {
          estimationDistribution += estimatedToOriginalSimilarity.get(estimatedVariant) * estimatedVariantsPercentages.get(estimatedVariant);
        }
      }

      result += Math.sqrt(originalDistribution * estimationDistribution);
    }

    System.out.println();
    System.out.println("G_sim-score: " + round(result * 100, 4) + " %");
    System.out.println("Average similarity: " + round(percentages.stream().mapToDouble(value -> value).average().getAsDouble() * 100, 4) + " %");
    System.out.println("Min similarity: " + round(percentages.stream().mapToDouble(value -> value).min().getAsDouble() * 100, 4) + " %");
    System.out.println("Max similarity: " + round(percentages.stream().mapToDouble(value -> value).max().getAsDouble() * 100,4) + " %");
  }

  public static double round(double number, int places) {
    double factor = Math.pow(10, places);
    return Math.round(number * factor) / factor;
  }
}
