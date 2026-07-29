package com.example.MiniProject.dto;

import java.math.BigDecimal;

public class FeeComponentRequest {
    public String componentType;
    public String calcType;
    public BigDecimal value;
    public BigDecimal minFee;
    public BigDecimal maxFee;
    public Boolean isEstimated;
}