package com.biddingSystem.cloudFunctions.service.impl;

import com.biddingSystem.cloudFunctions.clientConfig.ClientConfig;
import com.biddingSystem.cloudFunctions.dao.impl.FirestoreIngestAuctionDAOImpl;
import com.biddingSystem.cloudFunctions.dao.impl.SpannerIngestAuctionDAOImpl;
import com.biddingSystem.cloudFunctions.dto.AuctionData;
import com.google.cloud.firestore.Firestore;
import com.google.common.testing.TestLogHandler;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.logging.Logger;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class IngestServiceImplTest {
    private static final Logger LOGGER = Logger.getLogger(IngestServiceImpl.class.getName());
    private IngestServiceImpl ingestService;
    private final Gson gson = new Gson();

    @Mock
    private SpannerIngestAuctionDAOImpl spannerIngestAuctionDAO;

    @Mock
    private FirestoreIngestAuctionDAOImpl firestoreIngestAuctionDAO;

    private TestLogHandler LOG_HANDLER;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        LOG_HANDLER = new TestLogHandler();
        LOGGER.addHandler(LOG_HANDLER);

        ingestService = IngestServiceImpl.getInstance();
        SpannerIngestAuctionDAOImpl spannerIngestAuctionDAO = SpannerIngestAuctionDAOImpl.getInstance();
        spannerIngestAuctionDAO.setDatabaseClient(ClientConfig.getInstance().databaseClient());
        ingestService.setSpannerIngestAuctionDAO(spannerIngestAuctionDAO);

        FirestoreIngestAuctionDAOImpl firestoreIngestAuctionDAO = FirestoreIngestAuctionDAOImpl.getInstance();
        firestoreIngestAuctionDAO.setFirestoreClient(ClientConfig.getInstance().firestoreClient());
        ingestService.setFirestoreIngestAuctionDAO(firestoreIngestAuctionDAO);

        ingestService.setCloudTasksClient(ClientConfig.getInstance().cloudTasksClient());
    }

    @Test
    public void testIngestAuctionItemSpannerFailure() {
        ingestService.setSpannerIngestAuctionDAO(spannerIngestAuctionDAO);
        Mockito.when(spannerIngestAuctionDAO.createAuction(getAuctionData())).thenReturn(null);
        String response = ingestService.ingestAuctionItem(getAuctionData());
        Assert.assertNull(response);
        assertThat(LOG_HANDLER.getStoredLogRecords().get(1).getMessage()).isEqualTo("Unable to ingest in Spanner.");
    }

    @Test
    public void testIngestAuctionItemFirestoreFailure() {
        ingestService.setFirestoreIngestAuctionDAO(firestoreIngestAuctionDAO);
        Mockito.when(firestoreIngestAuctionDAO.addAuctionData(anyString(), any())).thenReturn(null);
        AuctionData auctionData = getAuctionData();
        auctionData.setExpirationTime(LocalDateTime.now().plusDays(4).toString());
        auctionData.setExpirationInSeconds(LocalDateTime.now().plusDays(4).toEpochSecond(ZoneOffset.UTC));
        auctionData.setConvertedBasePrice(2200.00);
        auctionData.setBasePrice(-1.0);
        String response = ingestService.ingestAuctionItem(auctionData);

        Assert.assertNull(response);
        assertThat(LOG_HANDLER.getStoredLogRecords().get(1).getMessage()).contains("Auction Id:");
        assertThat(LOG_HANDLER.getStoredLogRecords().get(2).getMessage()).isEqualTo("Unable to ingest in Firestore, deleting spanner entry.");
    }

    @Test
    public void testIngestAuctionItem() throws IOException {
        AuctionData auctionData = getAuctionData();
        auctionData.setExpirationTime(LocalDateTime.now().plusDays(4).toString());
        auctionData.setExpirationInSeconds(LocalDateTime.now().plusDays(4).toEpochSecond(ZoneOffset.UTC));
        auctionData.setConvertedBasePrice(2200.00);
        auctionData.setBasePrice(-1.0);
        String response = ingestService.ingestAuctionItem(auctionData);
        Assert.assertNotNull(response);
        assertThat(LOG_HANDLER.getStoredLogRecords().get(1).getMessage()).contains("Auction Id:");

        // Deleting test data
        Firestore firestore = ClientConfig.getInstance().firestoreClient();

        firestore.collection(auctionData.getItemCategory()).document(response).delete();
        SpannerIngestAuctionDAOImpl.getInstance().deleteAuction(response);
    }

    @Test
    public void testCreateCloudTask() throws IOException {
        ingestService.createCloudTask("1234", "Car", LocalDateTime.now().plusDays(5).toEpochSecond(ZoneOffset.UTC));
        assertThat(LOG_HANDLER.getStoredLogRecords().get(0).getMessage()).contains("Task created:");
        String message = LOG_HANDLER.getStoredLogRecords().get(0).getMessage();
        String taskName = message.substring(message.lastIndexOf(' ') + 1);
        ClientConfig.getInstance().cloudTasksClient().deleteTask(taskName);
    }

    private AuctionData getAuctionData() {
        String data = "{\"itemCategory\" : \"Car\", \"itemName\" : \"BMW A6\", \"basePrice\" : \"2200\", \"currencyCode\" : \"USD\", \"expirationTime\" : \"2024-02-05T11:50:55\", \"itemAttributes\" : {\"yearOfPurchase\" : \"2022\", \"distanceTravelled\" : \"270KM\", \"color\" : \"Black\"}}";

        return gson.fromJson(data, AuctionData.class);
    }
}