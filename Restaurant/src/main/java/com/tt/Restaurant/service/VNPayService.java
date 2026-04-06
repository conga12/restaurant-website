package com.tt.Restaurant.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface VNPayService {
    String createPaymentUrl(String txnRef, long amount, HttpServletRequest request);
    boolean verifySignature(Map<String, String> fields, String secureHash);
}