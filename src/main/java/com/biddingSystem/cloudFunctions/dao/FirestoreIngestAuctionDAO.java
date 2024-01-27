package com.biddingSystem.cloudFunctions.dao;

import com.biddingSystem.cloudFunctions.dto.AuctionData;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;

public interface FirestoreIngestAuctionDAO {
    ApiFuture<WriteResult> addAuctionData(String auctionId, AuctionData auctionData);
}
