package com.railway.main_service.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stations", indexes = {
  @Index(name = "idx_station_code", columnList = "station_code"),
  @Index(name = "idx_station_name", columnList = "station_name")
})
public class StationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "station_id")  // Add column name mapping
  private Long id;  // Change to 'id' (Java naming convention)

  @Column(nullable = false, unique = true, length = 255)
  private String stationCode;  // Use camelCase

  @Column(nullable = false, unique = true, length = 255)
  private String stationName;

  @Column(nullable = false, length = 255)
  private String city;

  @Column(nullable = false, length = 255)
  private String state;

  @Column(nullable = false, length = 255)
  private String zone;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(nullable = false)
  private int numPlatforms;  // Remove @NotBlank

  @Column(nullable = false)
  private boolean isJunction;  // Remove @NotBlank

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_by")
  private Long updatedBy;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_by")
  private Long deletedBy;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }
}
