package io.floci.gcp;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.DeleteSubscriptionRequest;
import com.google.pubsub.v1.DeleteTopicRequest;
import com.google.pubsub.v1.GetSubscriptionRequest;
import com.google.pubsub.v1.GetTopicRequest;
import com.google.pubsub.v1.ListSubscriptionsRequest;
import com.google.pubsub.v1.ListTopicsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PublishRequest;
import com.google.pubsub.v1.PublisherGrpc;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriberGrpc;
import com.google.pubsub.v1.Topic;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PubSubIntegrationTest {

    private static final String PROJECT = "test-project";
    private static final String TOPIC_ID = "it-topic-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String SUB_ID = "it-sub-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TOPIC_NAME = "projects/" + PROJECT + "/topics/" + TOPIC_ID;
    private static final String SUB_NAME = "projects/" + PROJECT + "/subscriptions/" + SUB_ID;

    private static ManagedChannel channel;
    private static TopicAdminClient topicAdmin;
    private static SubscriptionAdminClient subAdmin;
    private static PublisherGrpc.PublisherBlockingStub publisher;
    private static SubscriberGrpc.SubscriberBlockingStub subscriber;

    @BeforeAll
    static void setUp() throws Exception {
        // Quarkus @QuarkusTest binds gRPC on port 8081 (use-separate-server=false).
        channel = ManagedChannelBuilder.forAddress("127.0.0.1", 8081)
                .usePlaintext()
                .build();

        TransportChannelProvider channelProvider = InstantiatingGrpcChannelProvider.newBuilder()
                .setEndpoint("127.0.0.1:8081")
                .setChannelConfigurator(b -> b.usePlaintext())
                .build();

        topicAdmin = TopicAdminClient.create(TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(NoCredentialsProvider.create())
                .build());

        subAdmin = SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(NoCredentialsProvider.create())
                .build());

        publisher = PublisherGrpc.newBlockingStub(channel);
        subscriber = SubscriberGrpc.newBlockingStub(channel);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (topicAdmin != null) topicAdmin.close();
        if (subAdmin != null) subAdmin.close();
        if (channel != null) channel.shutdown();
    }

    @Test
    @Order(1)
    void createTopic() {
        Topic topic = topicAdmin.createTopic(ProjectTopicName.of(PROJECT, TOPIC_ID));
        assertEquals(TOPIC_NAME, topic.getName());
    }

    @Test
    @Order(2)
    void getTopic() {
        Topic topic = topicAdmin.getTopic(GetTopicRequest.newBuilder().setTopic(TOPIC_NAME).build());
        assertEquals(TOPIC_NAME, topic.getName());
    }

    @Test
    @Order(3)
    void listTopics() {
        List<String> names = new ArrayList<>();
        topicAdmin.listTopics(ListTopicsRequest.newBuilder()
                .setProject(ProjectName.of(PROJECT).toString())
                .build())
                .iterateAll()
                .forEach(t -> names.add(t.getName()));
        assertTrue(names.contains(TOPIC_NAME));
    }

    @Test
    @Order(4)
    void createSubscription() {
        Subscription sub = subAdmin.createSubscription(Subscription.newBuilder()
                .setName(SUB_NAME)
                .setTopic(TOPIC_NAME)
                .setPushConfig(PushConfig.getDefaultInstance())
                .setAckDeadlineSeconds(10)
                .build());
        assertEquals(SUB_NAME, sub.getName());
        assertEquals(TOPIC_NAME, sub.getTopic());
    }

    @Test
    @Order(5)
    void getSubscription() {
        Subscription sub = subAdmin.getSubscription(
                GetSubscriptionRequest.newBuilder().setSubscription(SUB_NAME).build());
        assertEquals(SUB_NAME, sub.getName());
    }

    @Test
    @Order(6)
    void listSubscriptions() {
        List<String> names = new ArrayList<>();
        subAdmin.listSubscriptions(ListSubscriptionsRequest.newBuilder()
                .setProject(ProjectName.of(PROJECT).toString())
                .build())
                .iterateAll()
                .forEach(s -> names.add(s.getName()));
        assertTrue(names.contains(SUB_NAME));
    }

    @Test
    @Order(7)
    void publishAndPullMessage() {
        var publishResponse = publisher.publish(PublishRequest.newBuilder()
                .setTopic(TOPIC_NAME)
                .addMessages(PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8("hello from integration test"))
                        .putAttributes("env", "test")
                        .build())
                .build());
        assertEquals(1, publishResponse.getMessageIdsCount());
        assertFalse(publishResponse.getMessageIds(0).isBlank());

        PullResponse pull = subscriber.pull(PullRequest.newBuilder()
                .setSubscription(SUB_NAME)
                .setMaxMessages(10)
                .build());
        assertFalse(pull.getReceivedMessagesList().isEmpty());
        ReceivedMessage msg = pull.getReceivedMessages(0);
        assertEquals("hello from integration test", msg.getMessage().getData().toStringUtf8());
        assertEquals("test", msg.getMessage().getAttributesMap().get("env"));

        // ack so subsequent tests start with an empty queue
        subscriber.acknowledge(AcknowledgeRequest.newBuilder()
                .setSubscription(SUB_NAME)
                .addAckIds(msg.getAckId())
                .build());
    }

    @Test
    @Order(8)
    void acknowledgeRemovesMessageFromQueue() {
        publisher.publish(PublishRequest.newBuilder()
                .setTopic(TOPIC_NAME)
                .addMessages(PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8("ack-test-message"))
                        .build())
                .build());

        PullResponse pull = subscriber.pull(PullRequest.newBuilder()
                .setSubscription(SUB_NAME)
                .setMaxMessages(10)
                .build());
        assertFalse(pull.getReceivedMessagesList().isEmpty());

        AcknowledgeRequest.Builder ackReq = AcknowledgeRequest.newBuilder()
                .setSubscription(SUB_NAME);
        pull.getReceivedMessagesList().forEach(m -> ackReq.addAckIds(m.getAckId()));
        subscriber.acknowledge(ackReq.build());

        PullResponse empty = subscriber.pull(PullRequest.newBuilder()
                .setSubscription(SUB_NAME)
                .setMaxMessages(10)
                .build());
        assertTrue(empty.getReceivedMessagesList().isEmpty());
    }

    @Test
    @Order(9)
    void publishMultipleMessages() {
        publisher.publish(PublishRequest.newBuilder()
                .setTopic(TOPIC_NAME)
                .addMessages(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("msg-1")).build())
                .addMessages(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("msg-2")).build())
                .addMessages(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("msg-3")).build())
                .build());

        PullResponse pull = subscriber.pull(PullRequest.newBuilder()
                .setSubscription(SUB_NAME)
                .setMaxMessages(10)
                .build());
        assertEquals(3, pull.getReceivedMessagesList().size());

        // ack all so cleanup tests start clean
        AcknowledgeRequest.Builder ackReq = AcknowledgeRequest.newBuilder()
                .setSubscription(SUB_NAME);
        pull.getReceivedMessagesList().forEach(m -> ackReq.addAckIds(m.getAckId()));
        subscriber.acknowledge(ackReq.build());
    }

    @Test
    @Order(11)
    void deleteSubscription() {
        subAdmin.deleteSubscription(
                DeleteSubscriptionRequest.newBuilder().setSubscription(SUB_NAME).build());

        List<String> names = new ArrayList<>();
        subAdmin.listSubscriptions(ListSubscriptionsRequest.newBuilder()
                .setProject(ProjectName.of(PROJECT).toString())
                .build())
                .iterateAll()
                .forEach(s -> names.add(s.getName()));
        assertFalse(names.contains(SUB_NAME));
    }

    @Test
    @Order(12)
    void deleteTopic() {
        topicAdmin.deleteTopic(DeleteTopicRequest.newBuilder().setTopic(TOPIC_NAME).build());

        List<String> names = new ArrayList<>();
        topicAdmin.listTopics(ListTopicsRequest.newBuilder()
                .setProject(ProjectName.of(PROJECT).toString())
                .build())
                .iterateAll()
                .forEach(t -> names.add(t.getName()));
        assertFalse(names.contains(TOPIC_NAME));
    }
}
