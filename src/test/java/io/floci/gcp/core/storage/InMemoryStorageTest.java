package io.floci.gcp.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryStorageTest {

    private InMemoryStorage<String, String> storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage<>();
    }

    @Test
    void putAndGet() {
        storage.put("k", "v");
        assertEquals(Optional.of("v"), storage.get("k"));
    }

    @Test
    void getMissingReturnsEmpty() {
        assertEquals(Optional.empty(), storage.get("missing"));
    }

    @Test
    void delete() {
        storage.put("k", "v");
        storage.delete("k");
        assertEquals(Optional.empty(), storage.get("k"));
    }

    @Test
    void deleteNonExistentIsNoop() {
        assertDoesNotThrow(() -> storage.delete("absent"));
    }

    @Test
    void scanMatchingPredicate() {
        storage.put("prefix/a", "1");
        storage.put("prefix/b", "2");
        storage.put("other/c", "3");

        List<String> results = storage.scan(k -> k.startsWith("prefix/"));
        assertEquals(2, results.size());
        assertTrue(results.containsAll(List.of("1", "2")));
    }

    @Test
    void scanNoMatchReturnsEmpty() {
        storage.put("a", "1");
        assertTrue(storage.scan(k -> k.startsWith("z")).isEmpty());
    }

    @Test
    void scanEmptyStorageReturnsEmpty() {
        assertTrue(storage.scan(k -> true).isEmpty());
    }

    @Test
    void keys() {
        storage.put("a", "1");
        storage.put("b", "2");
        assertTrue(storage.keys().containsAll(List.of("a", "b")));
        assertEquals(2, storage.keys().size());
    }

    @Test
    void keysIsUnmodifiable() {
        storage.put("a", "1");
        assertThrows(UnsupportedOperationException.class, () -> storage.keys().add("x"));
    }

    @Test
    void clear() {
        storage.put("a", "1");
        storage.put("b", "2");
        storage.clear();
        assertEquals(Optional.empty(), storage.get("a"));
        assertTrue(storage.keys().isEmpty());
    }

    @Test
    void overwriteExistingKey() {
        storage.put("k", "v1");
        storage.put("k", "v2");
        assertEquals(Optional.of("v2"), storage.get("k"));
    }

    @Test
    void flushAndLoadAreNoops() {
        storage.put("k", "v");
        assertDoesNotThrow(() -> storage.flush());
        assertDoesNotThrow(() -> storage.load());
        assertEquals(Optional.of("v"), storage.get("k"));
    }
}
