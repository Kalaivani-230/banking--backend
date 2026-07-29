package com.example.MiniProject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "master_countries")
public class MasterCountry {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}