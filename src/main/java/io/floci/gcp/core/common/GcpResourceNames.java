package io.floci.gcp.core.common;

/**
 * Utilities for parsing and building GCP resource name strings
 * (e.g. {@code projects/{project}/topics/{topic}}).
 */
public final class GcpResourceNames {

    private GcpResourceNames() {}

    /** Extracts the project ID from a resource name segment {@code projects/{project}/...}. */
    public static String parseProject(String resourceName) {
        if (resourceName == null) {
            return null;
        }
        int start = resourceName.indexOf("projects/");
        if (start < 0) {
            return null;
        }
        start += "projects/".length();
        int end = resourceName.indexOf('/', start);
        return end < 0 ? resourceName.substring(start) : resourceName.substring(start, end);
    }

    /** Extracts the last path segment (the resource ID) from a full resource name. */
    public static String lastSegment(String resourceName) {
        if (resourceName == null || resourceName.isEmpty()) {
            return resourceName;
        }
        int slash = resourceName.lastIndexOf('/');
        return slash < 0 ? resourceName : resourceName.substring(slash + 1);
    }

    public static String topic(String project, String topic) {
        return "projects/" + project + "/topics/" + topic;
    }

    public static String subscription(String project, String subscription) {
        return "projects/" + project + "/subscriptions/" + subscription;
    }

    public static String secret(String project, String secret) {
        return "projects/" + project + "/secrets/" + secret;
    }

    public static String secretVersion(String project, String secret, String version) {
        return "projects/" + project + "/secrets/" + secret + "/versions/" + version;
    }

    public static String firestoreDocument(String project, String database, String path) {
        return "projects/" + project + "/databases/" + database + "/documents/" + path;
    }
}
