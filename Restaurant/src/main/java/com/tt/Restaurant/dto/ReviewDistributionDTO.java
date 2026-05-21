package com.tt.Restaurant.dto;

import java.util.Map;

public record ReviewDistributionDTO(long total, double avg, Map<String,Integer> distribution) {}