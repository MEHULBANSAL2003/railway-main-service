package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name = "route_stops",
  schema = "railway_main",
  uniqueConstraints = {
    // A station can appear only once per route
    @UniqueConstraint(name = "uk_route_station",
      columnNames = { "route_id", "station_id" }),
    // Stop numbers must be unique per route
    @UniqueConstraint(name = "uk_route_stop_number",
      columnNames = { "route_id", "stop_number" })
  }
)
public class RouteStopEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "stop_id")
  private Long stopId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "route_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_route_stop_route"))
  private RouteEntity route;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "station_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_route_stop_station"))
  private StationEntity station;

  // Position in sequence — 1 = source, last = destination
  @Column(name = "stop_number", nullable = false)
  private Integer stopNumber;

  // Distance from the first stop (source) in km
  // Source stop always = 0
  @Column(name = "km_from_source", nullable = false)
  private Integer kmFromSource;

  // Day of journey — 1 = same day as departure
  // 2 = next day (for overnight trains like Rajdhani)
  @Column(name = "day_number", nullable = false)
  @Builder.Default
  private Integer dayNumber = 1;

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
