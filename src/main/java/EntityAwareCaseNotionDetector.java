import helpers.ConcurrencyHelper;
import models.*;
import modules.*;
import org.apache.commons.cli.*;
import helpers.EventCollector;
import helpers.ObjectLifecycleDetector;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class EntityAwareCaseNotionDetector {

  public static void main(String[] args) {

    // Parse command line arguments
    Options options = new Options();
    options.addOption("i", "input", true, "Specify location of the CSV-file representing the unlabeled event log taken as input");
    options.addOption("o", "output", true, "Specify directory for the resulting XES-logs");
    options.addOption("s", "separator", true, "Set column separator of the CSV-file specified as input (default: ';')");
    options.addOption("t", "threshold", true, "Set similarity threshold 0 <= t <= 1\n(default: 0.7)");
    options.addOption("v", "verbose", false, "Enable detailed console output");
    options.addOption("d", "debugging", false, "Enable debugging mode");
    options.addOption("h", "help", false, "Show available options");

    String inputFilePath = "";
    String outputDirectoryPath = "";
    String columnSeparator = ";";
    double selectedSimilarityThreshold = 0.7;
    boolean verbose = false;
    boolean debuggingMode = false;

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    try {
      CommandLine cmd = parser.parse(options, args);
      if (cmd.hasOption("h")) {
        formatter.printHelp( "java -jar entity-aware-case-notion-detection.jar [OPTIONS] -i [CSV-FILE] -o [DIRECTORY]", options );
        System.exit(0);
      }
      if (!cmd.hasOption("i") || !cmd.hasOption("o")) {
        System.out.println("Please provide an input event log (-i) and an output directory (-o) via the command line arguments");
        System.exit(0);
      }
      inputFilePath = cmd.getOptionValue("i");
      outputDirectoryPath = cmd.getOptionValue("o");
      if (cmd.hasOption("t")) {
        String thresholdValue = cmd.getOptionValue("t");
        if (thresholdValue != null) {
          selectedSimilarityThreshold = Double.parseDouble(thresholdValue);
        }
      }

      if (cmd.hasOption("s")) {
        columnSeparator = cmd.getOptionValue("s");
      }
      verbose = cmd.hasOption("v");
      debuggingMode = cmd.hasOption("d");
    } catch (ParseException e) {
      e.printStackTrace();
      System.exit(1);
    }

    final double similarityThreshold = selectedSimilarityThreshold;

    // Import Event Log from CSV
    List<Event> eventList = null;
    String inputFileName = "";

    try {
      eventList = CSVImporter.load(inputFilePath, columnSeparator, debuggingMode);
      inputFileName = Paths.get(inputFilePath).getFileName().toString();
      int dotIndex = inputFileName.lastIndexOf(".");
      inputFileName = (dotIndex == -1) ? inputFileName : inputFileName.substring(0, dotIndex);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    final List<Event> events = eventList;

    // Step 1: Event Log Subdivision

    // Detect Data Model
    List<ObjectClass> objectClasses = SchemaDetector.extractTables(events);

    // Determine number of cores for multiprocessing
    int cores = Math.min(objectClasses.size(), Runtime.getRuntime().availableProcessors() - 1); // Keep one core free to ensure responsiveness
    if (cores < 1) {
      cores = 1;
    }

    System.out.println("Continuing using " + cores + " threads\n");

    ThreadPoolExecutor executor =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);

    // Assign Events to Object Classes
    EventCollector.createEventChunks(executor, objectClasses, events);
    System.out.println();

    // Step 2: Instance Detection

    // Determine similarity weights and display data objects
    for (ObjectClass objectClass : objectClasses) {
      Map<String, Column> columns = objectClass.getAttributes();
      System.out.println(objectClass.getName() + " columns: " + columns.size());
      double totalScore = SimilarityWeightEvaluator.determineSimilarityWeights(new ArrayList(columns.values()), objectClass.getRelevantEvents());
      objectClass.setHighestSimilarityScore(totalScore);
      for (Column column : columns.values()) {
        System.out.println(column.getName() + " (similarity weight = " + column.getSimilarityWeight() / totalScore + ") [absolute similarity weight = " + column.getSimilarityWeight() + "]");
      }
      System.out.println();
    }

    System.out.println("Detecting object lifecycles... ");
    List<Future<Void>> lifecycleThreads = ConcurrencyHelper.startAll(executor, objectClasses.stream()
        .map(objectClass -> new ObjectLifecycleDetector(objectClass, similarityThreshold))
        .collect(Collectors.toList()));
    ConcurrencyHelper.syncAll(lifecycleThreads);

    System.out.println("done.\n");
    executor.shutdown();

    // Create references between events and instances
    System.out.print("Assigning instances to events... ");
    for (ObjectClass objectClass : objectClasses) {
      objectClass.getInstances().stream().forEach(dataObjectInstance -> {
        dataObjectInstance.getElements().stream().forEach(event -> {
          event.addEntityReference(dataObjectInstance);
        });
      });
    }
    System.out.println("done.");

    // Print life cycle for each object class
    for (ObjectClass objectClass : objectClasses) {
      System.out.println("\nTable: " + objectClass.getName() + "\n(extracted " + objectClass.getInstances().size() + " instances from " + objectClass.getRelevantEvents().size() + " events)\n");
      System.out.println(objectClass.getLifecycle());
    }

    // Print most common variants for object classes
    if (verbose) {
      for (ObjectClass objectClass : objectClasses) {
        System.out.println("Variants for table " + objectClass.getName());
        HashMap<String, Integer> variants = new HashMap<>();
        for (ObjectInstance instance : objectClass.getInstances()) {
          String variant = instance.getElements().stream().map(event -> event.getActivity()).collect(Collectors.joining(" -> "));
          if (!variants.containsKey(variant)) {
            variants.put(variant, 0);
          }
          variants.put(variant, variants.get(variant) + 1);
        }

        for (int i = 0; i < Math.min(20, variants.size()); i++) {
          int maxScore = 0;
          String variant = "";
          for (String key : variants.keySet()) {
            if (variants.get(key) > maxScore) {
              variant = key;
              maxScore = variants.get(key);
            }
          }
          variants.remove(variant);
          System.out.println(i + ". " + variant + " (" + maxScore + ")");
        }
        System.out.println();
      }
    }

    // Step 3: Trace Generation

    // Determine root object classes
    List<ObjectClass> rootObjectClasses = events.get(0).getInstanceReferences().stream().map(instance -> instance.getObjectClass()).collect(Collectors.toList());
    System.out.println("Root object class candidates: " + rootObjectClasses.stream().map(objectClass -> objectClass.getName()).collect(Collectors.joining(", ")));

    // Select root object class to generate the case notion
    Scanner scanner = new Scanner(System.in);
    boolean terminate = false;
    // Repeat to create multiple case notions if possible
    while(!terminate) {
      ObjectClass rootObjectClassCandidate = null;
      while (rootObjectClassCandidate == null) {
        System.out.println("\nSelect root object class: ");
        String rootTableName = scanner.next();
        for (ObjectClass objectClass : rootObjectClasses) {
          if (objectClass.getName().equals(rootTableName) || objectClass.getName().equals("ObjectClass_" + rootTableName)) {
            rootObjectClassCandidate = objectClass;
            break;
          }
        }
        if (rootObjectClassCandidate == null) {
          System.out.println("Could not find table " + rootTableName + "\nPlease try again.");
        }
      }

      final ObjectClass rootObjectClass = rootObjectClassCandidate;

      // Generate traces
      try {
        EventLog eventLog = TraceBuilder.buildEventLog(rootObjectClass, events);
        XESLogGenerator.serializeLog(eventLog, rootObjectClass, outputDirectoryPath, inputFileName);
      } catch (Exception e) {
        e.printStackTrace();
      }

      System.out.print("\nCreate another log? (y/n): ");
      String createNewLogDecision = scanner.next();
      if (!createNewLogDecision.equalsIgnoreCase("y")) {
        terminate = true;
      }
    }
    System.out.println("Program terminated");
  }
}
