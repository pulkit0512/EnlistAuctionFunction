package com.biddingSystem.cloudFunctions;

import com.biddingSystem.cloudFunctions.service.impl.IngestServiceImpl;
import com.google.common.testing.TestLogHandler;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RunWith(JUnit4.class)
public class EnlistAuctionItemFunctionTest {
    private static final Logger LOGGER = Logger.getLogger(EnlistAuctionItemFunction.class.getName());
    private static final String PROJECT_NAME = "biddingsystem-411900";
    private static final String TOPIC = "enlistItemTopic";
    private TestLogHandler LOG_HANDLER;

    @Mock
    private IngestServiceImpl ingestService;

    private EnlistAuctionItemFunction enlistAuctionItemFunction;

    @Before
    public void beforeMethod() {
        MockitoAnnotations.openMocks(this);
        LOG_HANDLER = new TestLogHandler();
        LOGGER.addHandler(LOG_HANDLER);

        enlistAuctionItemFunction = new EnlistAuctionItemFunction();
        enlistAuctionItemFunction.setIngestService(ingestService);
    }

    @Test
    public void accept() throws IOException {
        Mockito.when(ingestService.ingestAuctionItem(any())).thenReturn("12345");
        Mockito.doNothing().when(ingestService).createCloudTask("12345", "Car", 1714927855);

        String message = "{\"auctionId\":0,\"itemCategory\":\"Car\",\"itemName\":\"BMW A6\",\"basePrice\":2200.0,\"currencyCode\":\"EUR\",\"convertedBasePrice\":2395.14,\"expirationTime\":\"2024-05-05T11:50:55\",\"convertedExpirationTime\":\"2024-05-05T11:50:55-05:00[America/Chicago]\",\"expirationInSeconds\":1714927855,\"itemAttributes\":{\"yearOfPurchase\":\"2022\",\"distanceTravelled\":\"280KM\"}}";
        String data = "{\"message\":{\"data\":\"eyJhdWN0aW9uSWQiOjAsIml0ZW1DYXRlZ29yeSI6IkNhciIsIml0ZW1OYW1lIjoiQk1XIEE2IiwiYmFzZVByaWNlIjoyMjAwLjAsImN1cnJlbmN5Q29kZSI6IkVVUiIsImNvbnZlcnRlZEJhc2VQcmljZSI6MjM5NS4xNCwiZXhwaXJhdGlvblRpbWUiOiIyMDI0LTA1LTA1VDExOjUwOjU1IiwiY29udmVydGVkRXhwaXJhdGlvblRpbWUiOiIyMDI0LTA1LTA1VDExOjUwOjU1LTA1OjAwW0FtZXJpY2EvQ2hpY2Fnb10iLCJleHBpcmF0aW9uSW5TZWNvbmRzIjoxNzE0OTI3ODU1LCJpdGVtQXR0cmlidXRlcyI6eyJ5ZWFyT2ZQdXJjaGFzZSI6IjIwMjIiLCJkaXN0YW5jZVRyYXZlbGxlZCI6IjI4MEtNIn19\",\"messageId\":\"10117184544648254\",\"message_id\":\"10117184544648254\",\"publishTime\":\"2024-01-21T19:20:22.097Z\",\"publish_time\":\"2024-01-21T19:20:22.097Z\"},\"subscription\":\"projects/biddingsystem-411900/subscriptions/eventarc-asia-south2-function-1-616442-sub-172\"}";

        CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId("12345")
                .withData(data.getBytes())
                .withSource(URI.create("https://pubsub.googleapis.com/projects/" + PROJECT_NAME + "/topics/" + TOPIC))
                .withDataContentType("text/plain")
                .withType("com.google.cloud.pubsub.topic.publish")
                .build();

        enlistAuctionItemFunction.accept(cloudEvent);

        assertThat(LOG_HANDLER.getStoredLogRecords().get(0).getMessage()).isEqualTo("Event for list auction triggered!!");
        assertThat(LOG_HANDLER.getStoredLogRecords().get(1).getMessage()).isEqualTo("Pub/Sub message: " + message);
        assertThat(LOG_HANDLER.getStoredLogRecords().get(2).getMessage()).isEqualTo("Auction Created with id: 12345");
    }

    @Test
    public void acceptWithOutData() throws IOException {
        CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId("12345")
                .withoutData()
                .withSource(URI.create("https://pubsub.googleapis.com/projects/" + PROJECT_NAME + "/topics/" + TOPIC))
                .withDataContentType("text/plain")
                .withType("com.google.cloud.pubsub.topic.publish")
                .build();

        enlistAuctionItemFunction.accept(cloudEvent);

        assertThat(LOG_HANDLER.getStoredLogRecords().get(0).getMessage()).isEqualTo("Event for list auction triggered!!");
        assertThat(LOG_HANDLER.getStoredLogRecords().get(1).getMessage()).isEqualTo("Event data is null.");
    }

}