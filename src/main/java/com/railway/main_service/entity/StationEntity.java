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
  private Long station_id;

  @Column(nullable = false, unique = true, length = 255)
  @NotBlank(message = "Station Code is required")
  private String station_code;

  @Column(nullable = false, unique = true, length = 255)
  @NotBlank(message = "Station Name is required")
  private String station_name;

  @Column(nullable = false, length = 255)
  @NotBlank(message = "City is required")
  private String city;

  @Column(nullable = false, length = 255)
  @NotBlank(message = "State is required")
  private String state;

  @Column(nullable = false, length = 255)
  @NotBlank(message = "Zone is required")
  private String zone;

  @Column(nullable = false)
  @NotBlank(message = "Number of Platforms is required")
  private int num_platforms;

  @Column(nullable = false)
  @NotBlank(message = "Is Junction is required")
  private boolean is_junction;

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
