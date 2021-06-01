package modules;

import models.Column;
import models.Event;

import java.util.*;
import java.util.stream.Collectors;

public class SimilarityWeightEvaluator {

  private static int countUniqueValues(List<String> values) {
    Set<String> uniqueValues = new HashSet();
    values.forEach(value -> uniqueValues.add(value));
    return uniqueValues.size();
  }

  // Interpretation of the given ratio
  private static double distributionValue(double ratio) {
    double gradient = 20;
    return Math.exp(-gradient * Math.pow((ratio - 0.5), 2));
  }

  public static double determineSimilarityWeights(List<Column> columns, List<Event> events) {
    List<Double> weights = new ArrayList<>();
    for (Column column : columns) {
      double weight = SimilarityWeightEvaluator.getSimilarityWeight(column, events.size());
      weights.add(weight);
    }

    // Normalize weights
    double sum = 0;
    for (int i = 0; i < weights.size(); i++) {
      double weight = weights.get(i);
      sum += weight;
      columns.get(i).setSimilarityWeight(weight);
    }

    return sum;
  }

  public static double getSimilarityWeight(Column column, int eventSize) {
    List<String> values = column
        .getEntries()
        .stream()
        .map(entry -> entry.getValue())
        .filter(value -> value != null)
        .collect(Collectors.toList());

    if (values.isEmpty()) {
      return 0;
    }

    double distinctValues = countUniqueValues(values);

    double completenessFactor = (double) values.size() / (double) eventSize;
    double distinctFactor = distinctValues/ (double) values.size();
    double distributionFactor = distributionValue(distinctFactor);

    double result = completenessFactor * distributionFactor;

    return result;
  }

}
