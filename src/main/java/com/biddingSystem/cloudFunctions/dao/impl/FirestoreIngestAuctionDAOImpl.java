package com.biddingSystem.cloudFunctions.dao.impl;

import com.biddingSystem.cloudFunctions.dao.FirestoreIngestAuctionDAO;
import com.biddingSystem.cloudFunctions.dto.AuctionData;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

import java.util.logging.Logger;

public class FirestoreIngestAuctionDAOImpl implements FirestoreIngestAuctionDAO {
    private static volatile FirestoreIngestAuctionDAOImpl firestoreIngestAuctionDAO;
    private FirestoreIngestAuctionDAOImpl(){

    }

    public static FirestoreIngestAuctionDAOImpl getInstance() {
        if (firestoreIngestAuctionDAO == null) {
            synchronized (FirestoreIngestAuctionDAOImpl.class) {
                if (firestoreIngestAuctionDAO == null) {
                    firestoreIngestAuctionDAO = new FirestoreIngestAuctionDAOImpl();
                }
            }
        }
        return firestoreIngestAuctionDAO;
    }
    private static final Logger LOGGER = Logger.getLogger(FirestoreIngestAuctionDAOImpl.class.getName());
    private Firestore firestoreClient;

    @Override
    public ApiFuture<WriteResult> addAuctionData(String auctionId, AuctionData auctionData) {
        LOGGER.info("Adding auction to firestore.");
        DocumentReference docRef = firestoreClient.collection(auctionData.getItemCategory()).document(auctionId);

        //asynchronously write data
        return docRef.set(auctionData);
    }

    public void setFirestoreClient(Firestore firestoreClient) {
        this.firestoreClient = firestoreClient;
    }
}
