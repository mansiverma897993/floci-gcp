package io.floci.gcp.test;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GcsTest {

    private static final String BUCKET_NAME = TestFixtures.uniqueName("test-bucket");
    private static final String OBJECT_NAME = "test-object.txt";
    private static final String OBJECT_CONTENT = "Hello, GCP Cloud Storage!";

    private static Storage storage;

    @BeforeAll
    static void setUp() {
        storage = TestFixtures.storageClient();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (storage instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    @Test
    @Order(1)
    void createBucket() {
        Bucket bucket = storage.create(BucketInfo.of(BUCKET_NAME));

        assertThat(bucket.getName()).isEqualTo(BUCKET_NAME);
    }

    @Test
    @Order(2)
    void uploadObject() {
        BlobId blobId = BlobId.of(BUCKET_NAME, OBJECT_NAME);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("text/plain")
                .build();

        Blob blob = storage.create(blobInfo, OBJECT_CONTENT.getBytes(StandardCharsets.UTF_8));

        assertThat(blob.getName()).isEqualTo(OBJECT_NAME);
        assertThat(blob.getBucket()).isEqualTo(BUCKET_NAME);
        assertThat(blob.getContentType()).isEqualTo("text/plain");
        assertThat(blob.getSize()).isEqualTo(OBJECT_CONTENT.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    @Order(3)
    void downloadAndVerifyObjectContent() {
        BlobId blobId = BlobId.of(BUCKET_NAME, OBJECT_NAME);
        Blob blob = storage.get(blobId);

        assertThat(blob).isNotNull();

        byte[] content = blob.getContent();
        String downloadedContent = new String(content, StandardCharsets.UTF_8);

        assertThat(downloadedContent).isEqualTo(OBJECT_CONTENT);
    }

    @Test
    @Order(4)
    void listObjectsInBucket() {
        List<String> objectNames = new ArrayList<>();
        storage.list(BUCKET_NAME).iterateAll().forEach(blob -> objectNames.add(blob.getName()));

        assertThat(objectNames).contains(OBJECT_NAME);
    }

    @Test
    @Order(5)
    void deleteObjectAndBucket() {
        BlobId blobId = BlobId.of(BUCKET_NAME, OBJECT_NAME);
        boolean objectDeleted = storage.delete(blobId);
        assertThat(objectDeleted).isTrue();

        // Verify object is gone
        Blob deletedBlob = storage.get(blobId);
        assertThat(deletedBlob).isNull();

        // Delete the bucket
        Bucket bucket = storage.get(BUCKET_NAME);
        assertThat(bucket).isNotNull();
        boolean bucketDeleted = bucket.delete();
        assertThat(bucketDeleted).isTrue();

        // Verify bucket is gone
        Bucket deletedBucket = storage.get(BUCKET_NAME);
        assertThat(deletedBucket).isNull();
    }
}
