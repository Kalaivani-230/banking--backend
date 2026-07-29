package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "master_corridors",
       uniqueConstraints = @UniqueConstraint(columnNames = {"from_country_code", "to_country_code"}))
public class MasterCorridor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="from_country_code", nullable = false, length = 10)
    private String fromCountryCode;

    @Column(name="to_country_code", nullable = false, length = 10)
    private String toCountryCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFromCountryCode() { return fromCountryCode; }
    public void setFromCountryCode(String fromCountryCode) { this.fromCountryCode = fromCountryCode; }

    public String getToCountryCode() { return toCountryCode; }
    public void setToCountryCode(String toCountryCode) { this.toCountryCode = toCountryCode; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}