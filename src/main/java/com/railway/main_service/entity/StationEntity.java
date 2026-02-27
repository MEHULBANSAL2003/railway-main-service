package com.railway.main_service.entity;

import com.railway.main_service.enums.StationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
  name = "stations",
  indexes = {
    @Index(name = "idx_station_code", columnList = "station_code"),
    @Index(name = "idx_station_name", columnList = "station_name"),
    @Index(name = "idx_station_city", columnList = "city_id"),
    @Index(name = "idx_station_zone", columnList = "zone_id")
  },
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_station_code", columnNames = "station_code")
  }
)
public class StationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "station_id")
  private Long id;

  @Column(name = "station_code", nullable = false, unique = true, length = 10)
  private String stationCode;  // e.g., "NDLS", "PUNE", "MAS"

  @Column(name = "station_name", nullable = false, length = 100, unique = true)
  private String stationName;  // e.g., "New Delhi", "Pune Junction"

  // ===== FOREIGN KEY: Many stations belong to one city =====
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
    name = "city_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "fk_station_city")
  )
  private CityEntity city;
  // ========================================================

  // ===== FOREIGN KEY: Many stations belong to one zone =====
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
    name = "zone_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "fk_station_zone")
  )
  private ZoneEntity zone;
  // ========================================================

  @Enumerated(EnumType.STRING)
  @Column(name = "station_type", nullable = false, length = 30)
  private StationType stationType;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "num_platforms", nullable = false)
  private Integer numPlatforms;

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

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
