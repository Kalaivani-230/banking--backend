package com.example.MiniProject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "master_customer_types")
public class MasterCustomerType {

    @Id
    @Column(length = 20)
    private String code; // RETAIL, CORPORATE

    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}