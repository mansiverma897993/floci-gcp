package io.floci.gcp.services.pubsub.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoredTopic {

    private String name;
    private Map<String, String> labels;
    private String messageRetentionDuration;

    public StoredTopic() {}

    public StoredTopic(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, String> getLabels() { return labels; }
    public void setLabels(Map<String, String> labels) { this.labels = labels; }

    public String getMessageRetentionDuration() { return messageRetentionDuration; }
    public void setMessageRetentionDuration(String messageRetentionDuration) {
        this.messageRetentionDuration = messageRetentionDuration;
    }
}
