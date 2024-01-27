package com.biddingSystem.cloudFunctions.service;

import com.biddingSystem.cloudFunctions.dto.AuctionData;

public interface IngestService {
    String ingestAuctionItem(AuctionData auctionData);
    void createCloudTask(String auctionId, String category, long expiryInSeconds);
}
