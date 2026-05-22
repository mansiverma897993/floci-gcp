package io.floci.gcp.core.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GcpResourceNamesTest {

    @Test
    void parseProject_standardPath() {
        assertEquals("my-project", GcpResourceNames.parseProject("projects/my-project/topics/my-topic"));
    }

    @Test
    void parseProject_pathWithoutTrailingSegment() {
        assertEquals("my-project", GcpResourceNames.parseProject("projects/my-project"));
    }

    @Test
    void parseProject_null() {
        assertNull(GcpResourceNames.parseProject(null));
    }

    @Test
    void parseProject_noProjectsPrefix() {
        assertNull(GcpResourceNames.parseProject("topics/my-topic"));
    }

    @Test
    void parseProject_deepPath() {
        assertEquals("p", GcpResourceNames.parseProject("projects/p/databases/(default)/documents/col/doc"));
    }

    @Test
    void lastSegment_standard() {
        assertEquals("my-topic", GcpResourceNames.lastSegment("projects/my-project/topics/my-topic"));
    }

    @Test
    void lastSegment_singleSegment() {
        assertEquals("my-topic", GcpResourceNames.lastSegment("my-topic"));
    }

    @Test
    void lastSegment_null() {
        assertNull(GcpResourceNames.lastSegment(null));
    }

    @Test
    void lastSegment_empty() {
        assertEquals("", GcpResourceNames.lastSegment(""));
    }

    @Test
    void topic_buildsCorrectName() {
        assertEquals("projects/proj/topics/t", GcpResourceNames.topic("proj", "t"));
    }

    @Test
    void subscription_buildsCorrectName() {
        assertEquals("projects/proj/subscriptions/s", GcpResourceNames.subscription("proj", "s"));
    }

    @Test
    void secret_buildsCorrectName() {
        assertEquals("projects/proj/secrets/sec", GcpResourceNames.secret("proj", "sec"));
    }

    @Test
    void secretVersion_buildsCorrectName() {
        assertEquals("projects/proj/secrets/sec/versions/1", GcpResourceNames.secretVersion("proj", "sec", "1"));
    }

    @Test
    void firestoreDocument_buildsCorrectName() {
        assertEquals(
                "projects/proj/databases/(default)/documents/col/doc",
                GcpResourceNames.firestoreDocument("proj", "(default)", "col/doc"));
    }
}
