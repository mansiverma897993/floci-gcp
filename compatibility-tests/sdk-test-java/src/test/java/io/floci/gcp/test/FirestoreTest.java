package io.floci.gcp.test;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FirestoreTest {

    private static final String COLLECTION = "test-users";
    private static final String DOC_ID = TestFixtures.uniqueName("user");

    private static Firestore firestore;

    @BeforeAll
    static void setUp() {
        firestore = TestFixtures.firestoreClient();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (firestore != null) {
            firestore.close();
        }
    }

    @Test
    @Order(1)
    void setDocument() throws ExecutionException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Alice");
        data.put("age", 30L);
        data.put("email", "alice@example.com");
        data.put("active", true);

        DocumentReference docRef = firestore.collection(COLLECTION).document(DOC_ID);
        WriteResult result = docRef.set(data).get();

        assertThat(result.getUpdateTime()).isNotNull();
    }

    @Test
    @Order(2)
    void getDocumentAndVerifyFields() throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(DOC_ID).get().get();

        assertThat(snapshot.exists()).isTrue();
        assertThat(snapshot.getString("name")).isEqualTo("Alice");
        assertThat(snapshot.getLong("age")).isEqualTo(30L);
        assertThat(snapshot.getString("email")).isEqualTo("alice@example.com");
        assertThat(snapshot.getBoolean("active")).isTrue();
    }

    @Test
    @Order(3)
    void updateDocumentField() throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION).document(DOC_ID);
        WriteResult result = docRef.update("age", 31L, "city", "New York").get();

        assertThat(result.getUpdateTime()).isNotNull();

        DocumentSnapshot snapshot = docRef.get().get();
        assertThat(snapshot.getLong("age")).isEqualTo(31L);
        assertThat(snapshot.getString("city")).isEqualTo("New York");
        assertThat(snapshot.getString("name")).isEqualTo("Alice");
    }

    @Test
    @Order(4)
    void queryDocumentsWithFilter() throws ExecutionException, InterruptedException {
        // Add a second document to make the query meaningful
        String secondDocId = TestFixtures.uniqueName("user");
        Map<String, Object> secondData = new HashMap<>();
        secondData.put("name", "Bob");
        secondData.put("age", 25L);
        secondData.put("active", false);

        firestore.collection(COLLECTION).document(secondDocId).set(secondData).get();

        // Query active users
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION)
                .whereEqualTo("active", true)
                .get()
                .get();

        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
        assertThat(documents).isNotEmpty();

        List<String> names = documents.stream()
                .map(doc -> doc.getString("name"))
                .toList();
        assertThat(names).contains("Alice");
        assertThat(names).doesNotContain("Bob");

        // Clean up second document
        firestore.collection(COLLECTION).document(secondDocId).delete().get();
    }

    @Test
    @Order(5)
    void deleteDocument() throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION).document(DOC_ID);
        WriteResult result = docRef.delete().get();

        assertThat(result.getUpdateTime()).isNotNull();

        DocumentSnapshot snapshot = docRef.get().get();
        assertThat(snapshot.exists()).isFalse();
    }
}
