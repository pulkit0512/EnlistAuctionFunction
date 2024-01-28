package com.biddingSystem.cloudFunctions.service.impl;

import com.biddingSystem.cloudFunctions.dao.impl.FirestoreIngestAuctionDAOImpl;
import com.biddingSystem.cloudFunctions.dao.impl.SpannerIngestAuctionDAOImpl;
import com.biddingSystem.cloudFunctions.dto.AuctionData;
import com.biddingSystem.cloudFunctions.service.IngestService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.tasks.v2.*;
import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IngestServiceImpl implements IngestService {
    private static volatile IngestServiceImpl ingestService;
    private IngestServiceImpl(){

    }

    public static IngestServiceImpl getInstance() {
        if (ingestService == null) {
            synchronized (IngestServiceImpl.class) {
                if (ingestService == null) {
                    ingestService = new IngestServiceImpl();
                }
            }
        }
        return ingestService;
    }
    private static final Logger LOGGER = Logger.getLogger(IngestServiceImpl.class.getName());
    private static final Gson gson = new Gson();
    private static final String PROJECT_NAME = MoreObjects.firstNonNull(System.getenv("PROJECT_NAME"), "biddingsystem-411900");
    private static final String LOCATION_ID = MoreObjects.firstNonNull(System.getenv("LOCATION_ID"), "asia-south1");
    private static final String QUEUE_ID = MoreObjects.firstNonNull(System.getenv("QUEUE_ID"), "AuctionQueue");
    private static final String SERVICE_ACCOUNT_EMAIL = MoreObjects.firstNonNull(System.getenv("SERVICE_ACCOUNT_EMAIL"),
            "929883829834-compute@developer.gserviceaccount.com");

    private static final String URL = MoreObjects.firstNonNull(System.getenv("URL"),
            "https://asia-south1-biddingsystem-411900.cloudfunctions.net/NotifyUserFunction");

    // Construct the fully qualified queue name.
    private static final String QUEUE_PATH = QueueName.of(PROJECT_NAME, LOCATION_ID, QUEUE_ID).toString();

    // Add your service account email to construct the OIDC token.
    // in order to add an authentication header to the request.
    private static final OidcToken.Builder OIDC_TOKEN_BUILDER = OidcToken.newBuilder()
            .setServiceAccountEmail(SERVICE_ACCOUNT_EMAIL);

    private SpannerIngestAuctionDAOImpl spannerIngestAuctionDAO;

    private FirestoreIngestAuctionDAOImpl firestoreIngestAuctionDAO;

    private CloudTasksClient cloudTasksClient;

    @Override
    public String ingestAuctionItem(AuctionData auctionData) {
        LOGGER.info("Inside Spanner Ingest Auction Item");
        String auctionId = spannerIngestAuctionDAO.createAuction(auctionData);
        if (auctionId == null) {
            LOGGER.log(Level.WARNING, "Unable to ingest in Spanner.");
            return null;
        }
        LOGGER.info("Auction Id: " + auctionId);

        ApiFuture<WriteResult> firestoreIngestion = firestoreIngestAuctionDAO.addAuctionData(auctionId, auctionData);
        try {
            firestoreIngestion.get();
            return auctionId;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to ingest in Firestore, deleting spanner entry.");
            spannerIngestAuctionDAO.deleteAuction(auctionId);
            return null;
        }
    }

    @Override
    public void createCloudTask(String auctionId, String category, long expiryInSeconds) {
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(expiryInSeconds)
                .build();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("auctionId", auctionId);
        jsonObject.addProperty("category", category);
        String payload = gson.toJson(jsonObject);

        // Construct the task body.
        Task.Builder taskBuilder = Task.newBuilder()
                .setHttpRequest(
                        HttpRequest.newBuilder()
                                .setBody(ByteString.copyFrom(payload, Charset.defaultCharset()))
                                .setHttpMethod(HttpMethod.POST)
                                .setUrl(URL)
                                .setOidcToken(OIDC_TOKEN_BUILDER)
                                .build()
                )
                .setScheduleTime(timestamp);

        // Send create task request.
        Task task = cloudTasksClient.createTask(QUEUE_PATH, taskBuilder.build());
        LOGGER.info("Task created: " + task.getName());
    }

    public void setSpannerIngestAuctionDAO(SpannerIngestAuctionDAOImpl spannerIngestAuctionDAO) {
        this.spannerIngestAuctionDAO = spannerIngestAuctionDAO;
    }

    public void setFirestoreIngestAuctionDAO(FirestoreIngestAuctionDAOImpl firestoreIngestAuctionDAO) {
        this.firestoreIngestAuctionDAO = firestoreIngestAuctionDAO;
    }

    public void setCloudTasksClient(CloudTasksClient cloudTasksClient) {
        this.cloudTasksClient = cloudTasksClient;
    }
}
