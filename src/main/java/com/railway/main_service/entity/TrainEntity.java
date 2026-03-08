package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
  name   = "trains",
  schema = "railway_main",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_train_number",
      columnNames = "train_number"
    )
  }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "train_id")
  private Long trainId;

  // Official Indian Railways train number — immutable after creation
  @Column(name = "train_number", nullable = false, length = 5)
  private String trainNumber;

  // Display name shown to passengers on tickets, search results
  @Column(name = "train_name", nullable = false, length = 150)
  private String trainName;

  // Drives fare rules and superfast charge logic
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name         = "train_type_id",
    nullable     = false,
    foreignKey   = @ForeignKey(name = "fk_train_train_type")
  )
  private TrainTypeEntity trainType;

  // Zone that operates this train — WR, CR, NR etc.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name         = "zone_id",
    nullable     = false,
    foreignKey   = @ForeignKey(name = "fk_train_zone")
  )
  private ZoneEntity zone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name       = "route_id",
    foreignKey = @ForeignKey(name = "fk_train_route")
  )
  private RouteEntity route;

  // Physical feature — does this train have onboard food service
  @Column(name = "pantry_car", nullable = false)
  @Builder.Default
  private Boolean pantrycar = false;

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
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
