package com.biddingSystem.cloudFunctions.dao;

import com.biddingSystem.cloudFunctions.dto.AuctionData;

public interface SpannerIngestAuctionDAO {
    String createAuction(AuctionData auctionData);
    void deleteAuction(String auctionId);
}
