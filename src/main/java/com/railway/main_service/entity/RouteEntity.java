package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name = "routes",
  schema = "railway_main",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_route_code", columnNames = "route_code")
  }
)
public class RouteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_id")
  private Long routeId;

  // e.g. BTI-DLI-01
  @Column(name = "route_code", nullable = false, length = 20, unique = true)
  private String routeCode;

  // e.g. Bathinda Delhi Via Mansa
  @Column(name = "route_name", nullable = false, length = 150)
  private String routeName;

  // Derived from last stop km_from_source — stored for quick access
  @Column(name = "total_km")
  private Integer totalKm;

  // Source station — derived from stop_number=1, stored for fast filtering
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_station_id",
    foreignKey = @ForeignKey(name = "fk_route_source"))
  private StationEntity sourceStation;

  // Destination station — derived from last stop, stored for fast filtering
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "destination_station_id",
    foreignKey = @ForeignKey(name = "fk_route_destination"))
  private StationEntity destinationStation;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "updated_by")
  private Long updatedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

  @PreUpdate
  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
