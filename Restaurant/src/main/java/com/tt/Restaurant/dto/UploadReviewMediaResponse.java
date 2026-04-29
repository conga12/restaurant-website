package com.tt.Restaurant.dto;

import java.util.List;

public class UploadReviewMediaResponse {
    private List<String> urls;

    public UploadReviewMediaResponse() {}
    public UploadReviewMediaResponse(List<String> urls) { this.urls = urls; }

    public List<String> getUrls() { return urls; }
    public void setUrls(List<String> urls) { this.urls = urls; }
}