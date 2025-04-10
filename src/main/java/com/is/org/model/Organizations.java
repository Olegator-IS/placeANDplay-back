package com.is.org.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organizations", schema = "organizations")
public class Organizations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "current_location")
    private Long currentLocation;

    @Column(name = "org_type")
    private String orgType;

    @Column(name = "status")
    private String status;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "price_category")
    private String priceCategory;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Type(type = "jsonb")
    @Column(name = "attributes", columnDefinition = "jsonb")
    private JsonNode attributes;

    @Column(name = "address")
    private String address;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}