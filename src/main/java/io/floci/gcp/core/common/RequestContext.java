package io.floci.gcp.core.common;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds per-request values extracted from the incoming request.
 * Populated by {@link ProjectContextFilter} before any handler runs.
 */
@RequestScoped
public class RequestContext {

    private String projectId;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
