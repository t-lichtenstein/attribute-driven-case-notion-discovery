package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Event extends EstimationElement {
    private int id;
    private String caseId;
    private Map<String, String> attributes;
    private Long timestamp;
    private String activity;
    private List<ObjectInstance> entityReferences;

    public Event(int id, String caseId, Long timestamp, String activity, Map<String, String> attributes) {
        this.id = id;
        this.caseId = caseId;
        this.activity = activity;
        this.timestamp = timestamp;
        this.attributes = attributes;
        this.entityReferences = new ArrayList<>();
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public int getId() {
        return id;
    }

    public List<ObjectInstance> getInstanceReferences() {
        return entityReferences;
    }

    public void addEntityReference(ObjectInstance entityReference) {
        this.entityReferences.add(entityReference);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[" + this.getCaseId() + "] " + this.getId() + ": " + this.getActivity() + " (");
        this.getAttributes().entrySet().stream().forEach(attribute -> {
            sb.append(attribute.getKey() + " -> " + attribute.getValue() + ", ");
        });
        sb.append(")");
        return sb.toString();
    }


    @Override
    public String getName() {
        return this.getActivity();
    }
}
