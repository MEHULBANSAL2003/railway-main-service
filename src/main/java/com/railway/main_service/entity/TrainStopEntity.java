package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name   = "train_stops",
  schema = "railway_main",
  uniqueConstraints = {
    // A station can appear only once per train
    @UniqueConstraint(name = "uk_train_stop_station",
      columnNames = { "train_id", "station_id" }),
    // Stop numbers must be unique per train
    @UniqueConstraint(name = "uk_train_stop_number",
      columnNames = { "train_id", "stop_number" })
  }
)
public class TrainStopEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "stop_id")
  private Long stopId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "train_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_train_stop_train"))
  private TrainEntity train;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "station_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_train_stop_station"))
  private StationEntity station;

  // 1 = source, last = destination
  @Column(name = "stop_number", nullable = false)
  private Integer stopNumber;

  // Distance from source station in km. Source = 0.
  @Column(name = "km_from_source", nullable = false)
  private Integer kmFromSource;

  // null for first stop (source — no arrival)
  @Column(name = "arrival_time")
  private LocalTime arrivalTime;

  // null for last stop (destination — no departure)
  @Column(name = "departure_time")
  private LocalTime departureTime;

  // 1 = same day as train departure, 2 = next day (overnight trains)
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
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
