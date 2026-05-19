package io.floci.gcp.test;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatastoreTest {

    private static final String KIND = "TestEmployee";
    private static final String ENTITY_NAME = TestFixtures.uniqueName("emp");

    private static Datastore datastore;
    private static Key entityKey;

    @BeforeAll
    static void setUp() {
        datastore = TestFixtures.datastoreClient();
        KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND);
        entityKey = keyFactory.newKey(ENTITY_NAME);
    }

    @AfterAll
    static void tearDown() {
        // Ensure cleanup even if tests fail mid-way
        try {
            datastore.delete(entityKey);
        } catch (DatastoreException ignored) {
        }
    }

    @Test
    @Order(1)
    void putEntity() {
        Entity entity = Entity.newBuilder(entityKey)
                .set("name", "Charlie")
                .set("department", "Engineering")
                .set("salary", 95000L)
                .set("active", true)
                .build();

        datastore.put(entity);

        Entity retrieved = datastore.get(entityKey);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getString("name")).isEqualTo("Charlie");
    }

    @Test
    @Order(2)
    void getEntityAndVerifyProperties() {
        Entity entity = datastore.get(entityKey);

        assertThat(entity).isNotNull();
        assertThat(entity.getString("name")).isEqualTo("Charlie");
        assertThat(entity.getString("department")).isEqualTo("Engineering");
        assertThat(entity.getLong("salary")).isEqualTo(95000L);
        assertThat(entity.getBoolean("active")).isTrue();
    }

    @Test
    @Order(3)
    void updateEntityProperty() {
        Entity existing = datastore.get(entityKey);
        assertThat(existing).isNotNull();

        Entity updated = Entity.newBuilder(existing)
                .set("salary", 100000L)
                .set("title", "Senior Engineer")
                .build();

        datastore.update(updated);

        Entity retrieved = datastore.get(entityKey);
        assertThat(retrieved.getLong("salary")).isEqualTo(100000L);
        assertThat(retrieved.getString("title")).isEqualTo("Senior Engineer");
        assertThat(retrieved.getString("name")).isEqualTo("Charlie");
    }

    @Test
    @Order(4)
    void queryEntitiesByKindWithFilter() {
        // Add a second entity to make the query meaningful
        KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND);
        Key secondKey = keyFactory.newKey(TestFixtures.uniqueName("emp"));

        Entity secondEntity = Entity.newBuilder(secondKey)
                .set("name", "Diana")
                .set("department", "Marketing")
                .set("salary", 80000L)
                .set("active", false)
                .build();

        datastore.put(secondEntity);

        try {
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind(KIND)
                    .setFilter(PropertyFilter.eq("active", true))
                    .build();

            QueryResults<Entity> results = datastore.run(query);

            List<String> names = new ArrayList<>();
            while (results.hasNext()) {
                Entity e = results.next();
                names.add(e.getString("name"));
            }

            assertThat(names).contains("Charlie");
            assertThat(names).doesNotContain("Diana");
        } finally {
            datastore.delete(secondKey);
        }
    }

    @Test
    @Order(5)
    void deleteEntity() {
        datastore.delete(entityKey);

        Entity retrieved = datastore.get(entityKey);
        assertThat(retrieved).isNull();
    }
}
