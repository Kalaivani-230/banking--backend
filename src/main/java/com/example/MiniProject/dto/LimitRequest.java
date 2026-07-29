package com.example.MiniProject.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class LimitRequest {
    private BigDecimal perTxnLimit;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private Instant effectiveFrom;
    private Instant effectiveTo;
	public BigDecimal getPerTxnLimit() {
		return perTxnLimit;
	}
	public void setPerTxnLimit(BigDecimal perTxnLimit) {
		this.perTxnLimit = perTxnLimit;
	}
	public BigDecimal getDailyLimit() {
		return dailyLimit;
	}
	public void setDailyLimit(BigDecimal dailyLimit) {
		this.dailyLimit = dailyLimit;
	}
	public BigDecimal getMonthlyLimit() {
		return monthlyLimit;
	}
	public void setMonthlyLimit(BigDecimal monthlyLimit) {
		this.monthlyLimit = monthlyLimit;
	}
	public Instant getEffectiveFrom() {
		return effectiveFrom;
	}
	public void setEffectiveFrom(Instant effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}
	public Instant getEffectiveTo() {
		return effectiveTo;
	}
	public void setEffectiveTo(Instant effectiveTo) {
		this.effectiveTo = effectiveTo;
	}
	

    // getters/setters
}
