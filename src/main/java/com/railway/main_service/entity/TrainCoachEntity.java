package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name = "train_coaches",
  schema = "railway_main",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_train_coach_type",
      columnNames = { "train_id", "coach_type_id" })
  }
)
public class TrainCoachEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "coach_id")
  private Long coachId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "train_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_train_coach_train"))
  private TrainEntity train;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "coach_type_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_train_coach_type"))
  private CoachTypeEntity coachType;

  // How many physical coaches of this type (e.g. 6 → S1…S6)
  @Column(name = "coach_count", nullable = false)
  private Integer coachCount;

  // Per coach — scales with coachCount
  // e.g. 8 tatkal × 6 coaches = 48 total tatkal seats
  @Column(name = "tatkal_seats", nullable = false)
  @Builder.Default
  private Integer tatkalSeats = 0;

  // Per coach — physically tied to each coach (side berths in SL/3A)
  // e.g. 4 RAC × 6 coaches = 24 total RAC seats
  @Column(name = "rac_seats", nullable = false)
  @Builder.Default
  private Integer racSeats = 0;

  // Flat total for this coach class on this train
  // IRCTC sets one WL pool per class, not per individual coach
  // e.g. SL waitlist cap = 200 regardless of coach count
  @Column(name = "waitlist_limit", nullable = false)
  @Builder.Default
  private Integer waitlistLimit = 0;

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
