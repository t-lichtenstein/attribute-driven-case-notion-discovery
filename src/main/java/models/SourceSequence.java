package models;

import java.util.ArrayList;
import java.util.List;

public class SourceSequence<Element> {

  private List<Element> elements;

  public SourceSequence() {
    this.elements = new ArrayList<>();
  }

  public void add(Element element) {
    this.elements.add(element);
  }

  public List<Element> getElements() {
    return this.elements;
  }

  public Element getFirstElement() {
    return this.elements.isEmpty()
        ? null
        : this.elements.get(0);
  }

  public Element getLastElement() {
    return this.elements.isEmpty()
        ? null
        : this.elements.get(this.elements.size() - 1);
  }
}
