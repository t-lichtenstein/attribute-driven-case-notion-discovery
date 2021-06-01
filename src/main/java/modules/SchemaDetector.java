package modules;

import models.*;

import java.util.*;
import java.util.stream.Collectors;

// Concept inspired by: https://doi.org/10.1007/978-3-030-62522-1_5
public class SchemaDetector {

  public static void populateColumns(List<ObjectClass> objectClasses, List<Event> events) {
    for (ObjectClass objectClass : objectClasses) {
      List<Column> columns = new ArrayList<>(objectClass.getAttributes().values());
      // Populate columns with values
      for (Event event : events) {
        for (Column column : columns) {
          String value = event.getAttributes().get(column.getName());
          if (value != null && !value.isEmpty()) {
            column.add(event, value);
          }
        }
      }
    }
  }

  public static Map<String, Activity> extractActivities(List<Event> events) {
    Map<String, Activity> activityMap = new HashMap<>();
    for (Event event: events) {
      String activityName = event.getActivity();
      if (!activityMap.containsKey(activityName)) {
        activityMap.put(activityName, new Activity(activityName));
      }
    }
    return activityMap;
  }

  public static Map<String, Attribute> extractAttributes(List<Event> events) {
    Map<String, Attribute> attributeMap = new HashMap<>();
    for (Event event: events) {
      for (String attributeName : event.getAttributes().keySet()) {
        if (!attributeMap.containsKey(attributeName)) {
          attributeMap.put(attributeName, new Attribute(attributeName));
        }
        Attribute attribute = attributeMap.get(attributeName);
        attribute.addEvent(event);
      }
    }
    return attributeMap;
  }

  private static void filterLists(A2AMapping mapping, List<Activity> usedActivities, List<Attribute> usedAttributes) {
    for (Activity activity : usedActivities) {
      mapping.getActivities().remove(activity.getName());
    }
    for (Attribute attribute : usedAttributes) {
      mapping.getAttributes().remove(attribute.getName());
    }
  }

  /**
   * Merge related object classes to create mutually separated object classes with no overlapping attributes
   * @param objectClasses
   * @return
   */
  public static List<ObjectClass> mergeTables(List<ObjectClass> objectClasses) {
    List<Set<String>> columnSets = objectClasses.stream()
            .map(table -> (Set<String>) (new HashSet(table.getAttributeNames())))
            .collect(Collectors.toList());

    // Detect keys (columns that appear in multiple tables)
    Set<String> keys = new HashSet<>();
    Set<String> knownAttributes = new HashSet<>();

    for (Set<String> columnSet : columnSets) {
      for (String columnName : columnSet) {
        if (knownAttributes.contains(columnName)) {
          keys.add(columnName);
        }
        knownAttributes.add(columnName);
      }
    }

    List<TableCandidate> tableCandidates = new ArrayList<>();

    for (int i = 0; i < columnSets.size(); i++) {
      Set<String> setKeys = new HashSet<>();
      Set<String> setAttributes = new HashSet<>();
      for (String column : columnSets.get(i)) {
        if (keys.contains(column)) {
          setKeys.add(column);
        } else {
          setAttributes.add(column);
        }
      }
      tableCandidates.add(new TableCandidate(i, setKeys, setAttributes));
    }

    List<ObjectClass> mergedObjectClasses = new ArrayList<>();

    while(!tableCandidates.isEmpty()) {
      int minimumKeyCount = tableCandidates.stream().map(c -> c.getKeys().size()).mapToInt(x -> x).min().getAsInt();

      Optional<TableCandidate> optCandidate = tableCandidates.stream().filter(c -> c.getKeys().size() == minimumKeyCount).findFirst();
      if (!optCandidate.isPresent()) {
        break;
      }

      TableCandidate candidate = optCandidate.get();

      if (candidate.getKeys().isEmpty()) {
        mergedObjectClasses.add(new ObjectClass(mergedObjectClasses.size(), "ObjectClass_" + mergedObjectClasses.size(), new ArrayList<>(candidate.getAttributes())));
        List<TableCandidate> filteredCandidates = tableCandidates.stream().filter(c -> c.getId() != candidate.getId()).collect(Collectors.toList());
        tableCandidates = filteredCandidates;
        continue;
      }

      List<TableCandidate> mergeCandidates = new ArrayList<>();
      mergeCandidates.add(candidate);

      for (TableCandidate possibleMergeCandidate : tableCandidates) {
        if (candidate.getKeys().size() == possibleMergeCandidate.getKeys().size() && candidate.getId() != possibleMergeCandidate.getId()) {
          boolean sameKeySet = true;
          for (String candidateKey : candidate.getKeys()) {
            if (!possibleMergeCandidate.getKeys().contains(candidateKey)) {
              sameKeySet = false;
            }
          }
          if (sameKeySet) {
            mergeCandidates.add(possibleMergeCandidate);
          }
        }
      }

      Set<String> mergeCandidateColumns = new HashSet<>();
      for (TableCandidate tableCandidate : mergeCandidates) {
        mergeCandidateColumns.addAll(tableCandidate.getKeys());
        mergeCandidateColumns.addAll(tableCandidate.getAttributes());
      }

      mergedObjectClasses.add(new ObjectClass(mergedObjectClasses.size(), "ObjectClass_" + mergedObjectClasses.size(), new ArrayList<>(mergeCandidateColumns)));

      for (TableCandidate tableCandidate : tableCandidates) {
        if (tableCandidate.getId() != candidate.getId()) {
          for (String candidateKey : candidate.getKeys()) {
            tableCandidate.deleteKey(candidateKey);
          }
        }
      }

      Set<Integer> usedCandidateIds = mergeCandidates
              .stream()
              .map(c -> c.getId())
              .collect(Collectors.toSet());
      List<TableCandidate> filteredTableCandidates = tableCandidates
              .stream()
              .filter(c -> !usedCandidateIds.contains(c.getId()))
              .collect(Collectors.toList());
      tableCandidates = filteredTableCandidates;
    }

    return mergedObjectClasses;
  }

  public static A2AMapping generateA2AMapping(List<Event> events) {
    A2AMapping mapping = new A2AMapping();

    Map<String, Activity> activities = extractActivities(events);
    Map<String, Attribute> attributes = extractAttributes(events);

    for (Event event : events) {
      Activity activity = activities.get(event.getActivity());
      for (String attributeName : event.getAttributes().keySet()) {
        Attribute attribute = attributes.get(attributeName);
        mapping.incrementActivityToAttributeRelation(activity, attribute);
      }
    }

    for (Activity activity : activities.values()) {
      for (Attribute attribute : attributes.values()) {
        int relationCount = mapping.getRelationCounter(activity, attribute);
        if (relationCount > 0) {
          ActivityAttributeRelation relation = new ActivityAttributeRelation(activity, attribute, relationCount);
          activity.addAttributeRelation(relation);
          attribute.addActivityRelation(relation);
        }
      }
    }

    mapping.setActivities(activities);
    mapping.setAttributes(attributes);
    return mapping;
  }

  public static List<ObjectClass> detectIsolatedActivityIsolatedAttributeRelations(A2AMapping mapping, List<ObjectClass> objectClasses) {
    // Isolated activity, isolated attribute
    List<Activity> isolatedActivities = new ArrayList<>();
    List<Attribute> isolatedAttributes = new ArrayList<>();

    for (Attribute attribute : mapping.getAttributes().values()) {
      if (attribute.getActivityRelations().size() == 1 && attribute.getActivityRelations().get(0).getActivity().getAttributeRelations().size() == 1) {
        objectClasses.add(new ObjectClass(objectClasses.size(),"ObjectClass_" + objectClasses.size(), new ArrayList() {{ add(attribute.getName()); }}));
        isolatedActivities.add(attribute.getActivityRelations().get(0).getActivity());
        isolatedAttributes.add(attribute);
      }
    }
    filterLists(mapping, isolatedActivities, isolatedAttributes);

    return objectClasses;
  }

  public static List<ObjectClass> detectNonIsolatedActivityIsolatedAttributeRelations(A2AMapping mapping, List<ObjectClass> objectClasses) {
    // Non-isolated activity, isolated attribute
    List<Activity> nonIsolatedActivities = new ArrayList<>();
    List<Attribute> isolatedAttributes = new ArrayList<>();

    for (Attribute attribute : mapping.getAttributes().values()) {
      List<ActivityAttributeRelation> relations = attribute.getActivityRelations();
      List<Integer> relationCounts = new ArrayList<>();
      boolean candidate = true;
      for (ActivityAttributeRelation relation : relations) {
        if (relation.getActivity().getAttributeRelations().size() > 1 || relationCounts.contains(relation.getRelationCounter())) {
          candidate = false;
          break;
        }
        relationCounts.add(relation.getRelationCounter());
      }
      if (candidate) {
        objectClasses.add(new ObjectClass(objectClasses.size(), "ObjectClass_" + objectClasses.size(), new ArrayList() {{ add(attribute.getName()); }}));
        isolatedAttributes.add(attribute);
        nonIsolatedActivities.addAll(relations.stream().map(relation -> relation.getActivity()).collect(Collectors.toList()));
      }
    }

    filterLists(mapping, nonIsolatedActivities, isolatedAttributes);
    return objectClasses;
  }

  public static List<ObjectClass> detectIsolatedActivityNonIsolatedAttributeRelations(A2AMapping mapping, List<ObjectClass> objectClasses) {
    // Isolated activity, non-isolated attribute
    List<Activity> isolatedActivities = new ArrayList<>();
    List<Attribute> nonIsolatedAttributes = new ArrayList<>();

    for (Activity activity : mapping.getActivities().values()) {
      List<ActivityAttributeRelation> relations = activity.getAttributeRelations();
      if (relations.stream().anyMatch(relation -> relation.getAttribute().getActivityRelations().size() > 1)) {
        break;
      }
      isolatedActivities.add(activity);
      Map<Integer, List<Attribute>> rowCountMatcher = new HashMap();
      for (ActivityAttributeRelation relation : relations) {
        int count = relation.getRelationCounter();
        if (!rowCountMatcher.containsKey(count)) {
          rowCountMatcher.put(count, new ArrayList<>());
        }
        rowCountMatcher.get(count).add(relation.getAttribute());
      }
      List<Integer> rowCounts = new ArrayList<>(rowCountMatcher.keySet());
      Collections.sort(rowCounts);
      Collections.reverse(rowCounts);

      List<List<Attribute>> attributeClusters = new ArrayList<>();

      for (int rowCount : rowCounts) {
        List<Attribute> relatedAttributes = rowCountMatcher.get(rowCount);
        for (Attribute attribute : relatedAttributes) {
          nonIsolatedAttributes.add(attribute);

          boolean matched = false;
          for (List<Attribute> cluster : attributeClusters) {
            for (Attribute column : cluster) {
              Set<Integer> superSet = column.getEventIds();
              Set<Integer> subset = attribute.getEventIds();
              if (superSet.containsAll(subset)) {
                cluster.add(attribute);
                matched = true;
                break;
              }
            }
          }
          if (!matched) {
            attributeClusters.add(new ArrayList() {{ add(attribute); }});
          }
        }
      }

      for (List<Attribute> cluster : attributeClusters) {
        objectClasses.add(new ObjectClass(objectClasses.size(), "ObjectClass_" + objectClasses.size(), cluster.stream().map(Attribute::getName).collect(Collectors.toList())));
      }
    }

    filterLists(mapping, isolatedActivities, nonIsolatedAttributes);
    return objectClasses;
  }

  public static List<ObjectClass> detectNonIsolatedActivityNonIsolatedAttributeRelations(A2AMapping mapping, List<ObjectClass> objectClasses) {
    // Non-isolated activity, non-isolated attribute

    // Split islands into fragments
    for (Activity activity : mapping.getActivities().values()) {
      A2AMapping fragmentMapping = new A2AMapping();

      Activity fragmentActivity = new Activity(activity.getName());
      Map<String, Activity> activityMap = new HashMap() {{ put(fragmentActivity.getName(), fragmentActivity); }};

      Map<String, Attribute> attributeMap = new HashMap();
      for (ActivityAttributeRelation relation : activity.getAttributeRelations()) {
        Attribute fragmentAttribute = new Attribute(relation.getAttribute().getName());
        attributeMap.put(fragmentAttribute.getName(), fragmentAttribute);

        ActivityAttributeRelation fragmentRelation = new ActivityAttributeRelation(fragmentActivity, fragmentAttribute, relation.getRelationCounter());
        fragmentActivity.addAttributeRelation(fragmentRelation);
        fragmentAttribute.addActivityRelation(fragmentRelation);
      }

      fragmentMapping.setActivities(activityMap);
      fragmentMapping.setAttributes(attributeMap);

      detectIsolatedActivityIsolatedAttributeRelations(fragmentMapping, objectClasses);
      detectIsolatedActivityNonIsolatedAttributeRelations(fragmentMapping, objectClasses);
    }

    return objectClasses;
  }

  public static List<ObjectClass> extractTables(List<Event> events) {
    System.out.print("Reading tables... ");
    List<ObjectClass> objectClasses = new ArrayList<>();

    A2AMapping mapping = generateA2AMapping(events);

    detectIsolatedActivityIsolatedAttributeRelations(mapping, objectClasses);
    detectIsolatedActivityNonIsolatedAttributeRelations(mapping, objectClasses);
    detectNonIsolatedActivityIsolatedAttributeRelations(mapping, objectClasses);
    detectNonIsolatedActivityNonIsolatedAttributeRelations(mapping, objectClasses);

    objectClasses = SchemaDetector.mergeTables(objectClasses);
    SchemaDetector.populateColumns(objectClasses, events);

    System.out.println("done.\nDetected " + objectClasses.size() + " tables\n");
    for (ObjectClass objectClass : objectClasses) {
      System.out.println(objectClass.getName() + ": ");
      for (String columnName : objectClass.getAttributeNames()) {
        System.out.println("  - " + columnName);
      }
      System.out.println("");
    }

    return objectClasses;
  }
}
