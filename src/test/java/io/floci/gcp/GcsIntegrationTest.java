package io.floci.gcp;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GcsIntegrationTest {

    private static final String PROJECT = "test-project";
    private static final String BUCKET = "it-bucket-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String OBJECT_KEY = "test/hello.txt";
    private static final byte[] CONTENT = "hello, floci-gcp!".getBytes(StandardCharsets.UTF_8);

    private static Storage storage;

    @BeforeAll
    static void setUp() {
        // Quarkus @QuarkusTest binds REST on port 8081 (quarkus.http.test-port default).
        storage = StorageOptions.newBuilder()
                .setHost("http://localhost:8081")
                .setProjectId(PROJECT)
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    @Order(1)
    void createBucket() {
        Bucket bucket = storage.create(BucketInfo.of(BUCKET));
        assertEquals(BUCKET, bucket.getName());
    }

    @Test
    @Order(2)
    void getBucket() {
        Bucket bucket = storage.get(BUCKET);
        assertNotNull(bucket);
        assertEquals(BUCKET, bucket.getName());
    }

    @Test
    @Order(3)
    void listBuckets() {
        List<String> names = new ArrayList<>();
        storage.list().iterateAll().forEach(b -> names.add(b.getName()));
        assertTrue(names.contains(BUCKET));
    }

    @Test
    @Order(4)
    void uploadObject() {
        BlobInfo info = BlobInfo.newBuilder(BUCKET, OBJECT_KEY)
                .setContentType("text/plain")
                .build();
        Blob blob = storage.create(info, CONTENT);
        assertEquals(BUCKET, blob.getBucket());
        assertEquals(OBJECT_KEY, blob.getName());
        assertEquals("text/plain", blob.getContentType());
        assertEquals(CONTENT.length, blob.getSize());
    }

    @Test
    @Order(5)
    void downloadObject() {
        byte[] downloaded = storage.readAllBytes(BUCKET, OBJECT_KEY);
        assertArrayEquals(CONTENT, downloaded);
    }

    @Test
    @Order(6)
    void getObjectMetadata() {
        Blob blob = storage.get(BlobId.of(BUCKET, OBJECT_KEY));
        assertNotNull(blob);
        assertEquals("text/plain", blob.getContentType());
        assertNotNull(blob.getCreateTime());
    }

    @Test
    @Order(7)
    void listObjects() {
        List<String> names = new ArrayList<>();
        storage.list(BUCKET).iterateAll().forEach(b -> names.add(b.getName()));
        assertTrue(names.contains(OBJECT_KEY));
    }

    @Test
    @Order(8)
    void uploadSecondObject() {
        BlobInfo info = BlobInfo.newBuilder(BUCKET, "test/second.txt").build();
        storage.create(info, "second".getBytes(StandardCharsets.UTF_8));

        List<String> names = new ArrayList<>();
        storage.list(BUCKET).iterateAll().forEach(b -> names.add(b.getName()));
        assertTrue(names.contains("test/second.txt"));
    }

    @Test
    @Order(9)
    void deleteObject() {
        boolean deleted = storage.delete(BlobId.of(BUCKET, OBJECT_KEY));
        assertTrue(deleted);
        assertNull(storage.get(BlobId.of(BUCKET, OBJECT_KEY)));
    }

    @Test
    @Order(10)
    void deleteBucket() {
        // Delete remaining object first
        storage.delete(BlobId.of(BUCKET, "test/second.txt"));
        boolean deleted = storage.delete(BUCKET);
        assertTrue(deleted);
        assertNull(storage.get(BUCKET));
    }
}
