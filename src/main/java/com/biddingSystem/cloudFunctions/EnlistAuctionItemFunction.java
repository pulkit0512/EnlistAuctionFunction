package com.biddingSystem.cloudFunctions;

import com.biddingSystem.cloudFunctions.clientConfig.ClientConfig;
import com.biddingSystem.cloudFunctions.dao.impl.FirestoreIngestAuctionDAOImpl;
import com.biddingSystem.cloudFunctions.dao.impl.SpannerIngestAuctionDAOImpl;
import com.biddingSystem.cloudFunctions.dto.AuctionData;
import com.biddingSystem.cloudFunctions.service.impl.IngestServiceImpl;
import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.gson.Gson;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;

import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

public class EnlistAuctionItemFunction implements CloudEventsFunction {
    private static final Logger LOGGER = Logger.getLogger(EnlistAuctionItemFunction.class.getName());
    private volatile IngestServiceImpl ingestService;
    private static final Gson gson = new Gson();

    @Override
    public void accept(CloudEvent cloudEvent) throws IOException {
        if (ingestService == null) {
            synchronized (EnlistAuctionItemFunction.class) {
                if (ingestService == null) {
                    createIngestServiceObject();
                }
            }
        }

        LOGGER.info("Event for list auction triggered!!");
        CloudEventData eventData = cloudEvent.getData();
        if (eventData == null) {
            LOGGER.info("Event data is null.");
            return;
        }
        String cloudEventData = new String(eventData.toBytes());

        MessagePublishedData publishedData = gson.fromJson(cloudEventData, MessagePublishedData.class);
        // Get the message from the data
        Message message = publishedData.getMessage();
        // Get the base64-encoded data from the message & decode it
        String encodedData = message.getData();
        String decodedData = new String(Base64.getDecoder().decode(encodedData));
        LOGGER.info("Pub/Sub message: " + decodedData);

        AuctionData auctionData = gson.fromJson(decodedData, AuctionData.class);

        String auctionId = ingestService.ingestAuctionItem(auctionData);
        if (auctionId != null) {
            LOGGER.info("Auction Created with id: " + auctionId);
            ingestService.createCloudTask(auctionId, auctionData.getItemCategory(), auctionData.getExpirationInSeconds());
        }
    }

    public void setIngestService(IngestServiceImpl ingestService) {
        this.ingestService = ingestService;
    }

    private void createIngestServiceObject() throws IOException {
        ingestService = IngestServiceImpl.getInstance();
        ingestService.setCloudTasksClient(ClientConfig.getInstance().cloudTasksClient());

        SpannerIngestAuctionDAOImpl spannerIngestAuctionDAO = SpannerIngestAuctionDAOImpl.getInstance();
        spannerIngestAuctionDAO.setDatabaseClient(ClientConfig.getInstance().databaseClient());
        ingestService.setSpannerIngestAuctionDAO(spannerIngestAuctionDAO);

        FirestoreIngestAuctionDAOImpl firestoreIngestAuctionDAO = FirestoreIngestAuctionDAOImpl.getInstance();
        firestoreIngestAuctionDAO.setFirestoreClient(ClientConfig.getInstance().firestoreClient());
        ingestService.setFirestoreIngestAuctionDAO(firestoreIngestAuctionDAO);
    }
}
