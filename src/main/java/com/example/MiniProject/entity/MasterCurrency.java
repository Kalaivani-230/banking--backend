package com.example.MiniProject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "master_currencies")
public class MasterCurrency {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    private String symbol;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}