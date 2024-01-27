package com.biddingSystem.cloudFunctions.dao.impl;

import com.biddingSystem.cloudFunctions.dao.SpannerIngestAuctionDAO;
import com.biddingSystem.cloudFunctions.dto.AuctionData;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;

import java.util.logging.Logger;

public class SpannerIngestAuctionDAOImpl implements SpannerIngestAuctionDAO {
    private static volatile SpannerIngestAuctionDAOImpl spannerIngestAuctionDAO;
    private SpannerIngestAuctionDAOImpl() {

    }

    public static SpannerIngestAuctionDAOImpl getInstance() {
        if (spannerIngestAuctionDAO == null) {
            synchronized (SpannerIngestAuctionDAOImpl.class) {
                if (spannerIngestAuctionDAO == null) {
                    spannerIngestAuctionDAO = new SpannerIngestAuctionDAOImpl();
                }
            }
        }
        return spannerIngestAuctionDAO;
    }
    private static final Logger LOGGER = Logger.getLogger(SpannerIngestAuctionDAOImpl.class.getName());
    private static final String INSERT_SQL = "INSERT INTO AUCTION (CATEGORY, BASE_PRICE, MAX_BID_PRICE, C_USER_ID, AUCTION_CREATION_TIME, AUCTION_EXPIRY_TIME)\n" +
            "VALUES (@category, @basePrice, null, null, CURRENT_TIMESTAMP, @expiryTime) THEN RETURN AUCTION_ID";

    private static final String DELETE_SQL = "DELETE FROM AUCTION WHERE AUCTION_ID = @auctionId";

    private DatabaseClient databaseClient;

    @Override
    public String createAuction(AuctionData auctionData) {
        LOGGER.info("Inside create auction for Spanner.");
        Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(auctionData.getExpirationInSeconds(), 0);

        return databaseClient.readWriteTransaction().run(transaction-> {
            Statement statement = Statement.newBuilder(INSERT_SQL)
                    .bind("category")
                    .to(auctionData.getItemCategory())
                    .bind("basePrice")
                    .to(auctionData.getConvertedBasePrice())
                    .bind("expiryTime")
                    .to(timestamp)
                    .build();

            try (ResultSet resultSet = transaction.executeQuery(statement)) {
                if (resultSet.next()) {
                    String auctionId = resultSet.getString("AUCTION_ID");
                    LOGGER.info("Data inserted in Spanner for AuctionId: " + auctionId);
                    return auctionId;
                } else {
                    return null;
                }
            }
        });
    }

    @Override
    public void deleteAuction(String auctionId) {
        databaseClient.readWriteTransaction()
                .run(transaction -> {
                    Statement statement = Statement.newBuilder(DELETE_SQL)
                            .bind("auctionID")
                            .to(auctionId)
                            .build();
                    transaction.executeUpdate(statement);
                    LOGGER.info("Record deleted.");
                    return null;
                });
    }

    public void setDatabaseClient(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }
}
