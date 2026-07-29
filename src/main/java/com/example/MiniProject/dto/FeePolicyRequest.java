package com.example.MiniProject.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class FeePolicyRequest {
    public String policyName;
    public Long corridorId;
    public String sendCurrency;
    public String receiveCurrency;
    public String channelCode;
    public String customerTypeCode;
    public BigDecimal amountMin;
    public BigDecimal amountMax;
    public Integer priority;
    public Instant effectiveFrom;
    public Instant effectiveTo;

    public List<FeeComponentRequest> components;
}