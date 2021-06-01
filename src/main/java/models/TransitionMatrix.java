package models;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public abstract class TransitionMatrix<Source extends SourceSequence<Element>, Element extends EstimationElement> extends Thread {

  private double[][] matrix;
  private List<Source> sources;
  private List<Element> elements;
  public Map<String, Integer> transitionEntryMap;
  private final String identifier;
  protected final int startActivity;
  protected final int endActivity;
  protected final List<String> activities;

  public TransitionMatrix(List<Element> elements, String identifier) {
    this.identifier = identifier;
    this.elements = elements;
    this.sources = new ArrayList<>();
    List<String> entries = elements.stream().map(element -> element.getName()).distinct().collect(Collectors.toList());
    this.transitionEntryMap = new HashMap<>();
    this.activities = new ArrayList<>();
    this.startActivity = 0;
    for (int i = 0; i < entries.size(); i++) {
      this.transitionEntryMap.put(entries.get(i), i + 1);
      activities.add(entries.get(i));
    }
    this.endActivity = this.transitionEntryMap.size() + 1;
    this.clearTransitionMatrix();
  }

  protected abstract Source getSuitableSource(List<Source> activeSources, Element element);

  protected abstract Source createSource();

  @Override
  public void run() {
    this.estimate();
  }

  public void estimate() {
    int iteration = 0;
    List<List<Source>> previousSources = new ArrayList<>();
    System.out.println("(" + this.getIdentifier() + ") - Initializing source sequence");
    this.estimateSequences();
    System.out.println("(" + this.getIdentifier() + ") - Finished initialization ");
    while (!this.containsSequences(previousSources, this.sources))  {
      iteration++;
      previousSources.add(new ArrayList<>(this.sources));
      System.out.println("(" + this.getIdentifier() + ") - Starting estimation iteration " + iteration);
      this.updateMatrix();
      this.estimateSequences();
      System.out.println("(" + this.getIdentifier() + ") - Finished estimation iteration " + iteration);
    }
    this.updateMatrix();
    System.out.println("(" + this.getIdentifier() + ") - Finished source sequence estimation after " + iteration + " iteration(s)");
    this.postProcessing();
  }

  protected void estimateSequences() {
    this.sources = new ArrayList<>();
    for (Element element : this.elements) {
      Source suitableSource = this.getSuitableSource(this.sources, element);
      if (suitableSource == null) {
        suitableSource = this.createSource();
        this.sources.add(suitableSource);
      }
      suitableSource.add(element);
    }
  }

  protected void updateMatrix() {
    this.clearTransitionMatrix();
    for (Source source : this.sources) {
      incrementTransitionProbability(this.startActivity, this.transitionEntryMap.get(source.getFirstElement().getName()));
      incrementTransitionProbability(this.transitionEntryMap.get(source.getLastElement().getName()), this.endActivity);
      for (int i = 0; i < source.getElements().size() - 1; i++) {
        incrementTransitionProbability(source.getElements().get(i), source.getElements().get(i + 1));
      }
    }

    this.normalizeTransitionMatrix();
  }

  public Map<String, Integer> getTransitionEntryMap() {
    return transitionEntryMap;
  }

  public void setTransitionEntryMap(Map<String, Integer> transitionEntryMap) {
    this.transitionEntryMap = transitionEntryMap;
  }

  public int getStartActivity() {
    return startActivity;
  }

  public int getEndActivity() {
    return endActivity;
  }

  public double getTransitionProbability(int from, int to) {
    return this.matrix[from][to];
  }

  public double getTransitionProbability(Element from, Element to) {
    return this.getTransitionProbability(this.transitionEntryMap.get(from.getName()), this.transitionEntryMap.get(to.getName()));
  }

  public void incrementTransitionProbability(int from, int to) {
    this.matrix[from][to] += 1;
  }

  public void incrementTransitionProbability(Element from, Element to) {
    this.incrementTransitionProbability(this.transitionEntryMap.get(from.getName()), this.transitionEntryMap.get(to.getName()));
  }

  public void clearTransitionMatrix() {
    this.matrix = new double[this.transitionEntryMap.size() + 2][this.transitionEntryMap.size() + 2];
  }

  public void normalizeTransitionMatrix() {
    for (int i = 0; i < this.matrix.length; i++) {
      double[] row = this.matrix[i];
      double rowSum = Arrays.stream(row).sum();
      this.matrix[i] = Arrays.stream(row).map(entry -> {
        if (entry > 0) {
          return entry / rowSum;
        }
        return 0.0;
      }).toArray();
    }
  }

  private boolean containsSequences(List<List<Source>> previousSequences, List<Source> currentSequence) {
    for (List<Source> sequence : previousSequences) {

      // Check for number of sources
      if (sequence.size() != currentSequence.size()) {
        continue;
      }

      // Check for same sources
      boolean sourcesAreEqual = true;
      for (int i = 0; i < sequence.size(); i++) {
        Source originalSource = sequence.get(i);
        Source currentSource = currentSequence.get(i);

        if (originalSource.getElements().size() != currentSource.getElements().size()) {
          sourcesAreEqual = false;
          break;
        }

        for (int j = 0; j < originalSource.getElements().size(); j++) {
          Element originalElement = originalSource.getElements().get(j);
          Element currentElement = currentSource.getElements().get(j);
          if (originalElement.getId() != currentElement.getId()) {
            sourcesAreEqual = false;
            break;
          }
        }

        if (!sourcesAreEqual) {
          break;
        }
      }

      if (!sourcesAreEqual) {
        continue;
      }
      return true;
    }
    return false;
  }

  public List<Source> getSources() {
    return sources;
  }

  public void setSources(List<Source> sources) {
    this.sources = sources;
  }

  public List<Element> getElements() {
    return elements;
  }

  public void setElements(List<Element> elements) {
    this.elements = elements;
  }

  public String getProcess() {
    StringBuilder sb = new StringBuilder();

    sb.append("o: START\n");
    for (String activity : this.activities) {
      sb.append(this.transitionEntryMap.get(activity) + ": " + activity + "\n");
    }
    sb.append("x: END\n\n");
    sb.append("    ");
    for (int topIndex = 0; topIndex < this.matrix.length; topIndex++) {
      if (topIndex == 0) {
        sb.append("  o  ");
      } else if (topIndex == this.matrix.length - 1) {
        sb.append("  x  ");
      } else {
        String topIndexValue = "  " + topIndex;
        if (topIndex < 100) {
          topIndexValue += " ";
        }
        if (topIndex < 10) {
          topIndexValue += " ";
        }
        sb.append(topIndexValue);
      }
    }
    sb.append("\n    ");
    for (int topIndex = 0; topIndex < this.matrix.length; topIndex++) {
      sb.append("-----");
    }
    sb.append("\n");

    DecimalFormat decimalFormat = new DecimalFormat("#.##");

    for (int from = 0; from < this.matrix.length; from++) {
      if (from == 0) {
        sb.append("o  |");
      } else if (from == this.matrix.length - 1) {
        sb.append("x  |");
      } else {
        if (String.valueOf(from).length() == 1) {
          sb.append(from + "  |");
        } else {
          sb.append(from + " |");
        }
      }
      for (int to = 0; to < this.matrix[from].length; to++) {
        if (this.matrix[from][to] == 0) {
          sb.append("  -  ");
        } else {
          String value = decimalFormat.format(this.matrix[from][to]);
          if (value.length() == 1) {
            value += ".00";
          }
          while (value.length() < 4) {
            value += 0;
          }
          sb.append(" " + value);
        }
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public List<String> getActivities() {
    return this.activities;
  }

  public String getIdentifier() {
    return identifier;
  }

  protected void postProcessing() {

  }
}
