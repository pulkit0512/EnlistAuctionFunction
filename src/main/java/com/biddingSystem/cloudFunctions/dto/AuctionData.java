package com.biddingSystem.cloudFunctions.dto;

import java.util.Map;

public class AuctionData {
    private String userEmail;
    private String itemCategory;
    private String itemName;
    private Double basePrice;
    private String currencyCode;
    private Double convertedBasePrice;
    private String expirationTime;
    private String convertedExpirationTime;
    private Long expirationInSeconds;
    private Map<String, String> itemAttributes;

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Double basePrice) {
        this.basePrice = basePrice;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Double getConvertedBasePrice() {
        return convertedBasePrice;
    }

    public void setConvertedBasePrice(Double convertedBasePrice) {
        this.convertedBasePrice = convertedBasePrice;
    }

    public String getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(String expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getConvertedExpirationTime() {
        return convertedExpirationTime;
    }

    public void setConvertedExpirationTime(String convertedExpirationTime) {
        this.convertedExpirationTime = convertedExpirationTime;
    }

    public Long getExpirationInSeconds() {
        return expirationInSeconds;
    }

    public void setExpirationInSeconds(Long expirationInSeconds) {
        this.expirationInSeconds = expirationInSeconds;
    }

    public Map<String, String> getItemAttributes() {
        return itemAttributes;
    }

    public void setItemAttributes(Map<String, String> itemAttributes) {
        this.itemAttributes = itemAttributes;
    }
}