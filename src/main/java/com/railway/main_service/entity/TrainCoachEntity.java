package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name = "train_coaches",
  schema = "railway_main"
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

  @Column(name = "coach_count", nullable = false)
  private Integer coachCount;

  @Column(name = "tatkal_seats", nullable = false)
  @Builder.Default
  private Integer tatkalSeats = 0;

  @Column(name = "rac_seats", nullable = false)
  @Builder.Default
  private Integer racSeats = 0;

  @Column(name = "waitlist_limit", nullable = false)
  @Builder.Default
  private Integer waitlistLimit = 0;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  // ── Effective date range ──────────────────────────────
  // effectiveFrom: config is valid from this date onwards
  // effectiveTo:   null = still active (no end date)
  // When config changes → close this row (set effectiveTo) + insert new row
  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(name = "change_reason", length = 500)
  private String changeReason;

  // ── Audit ─────────────────────────────────────────────
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
    // Default effectiveFrom to today if not explicitly set
    if (effectiveFrom == null) effectiveFrom = LocalDate.now();
  }

  @PreUpdate
  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

  // ── Derived helper ────────────────────────────────────
  @Transient
  public boolean isCurrentlyActive() {
    LocalDate today = LocalDate.now();
    return Boolean.TRUE.equals(isActive)
      && !effectiveFrom.isAfter(today)
      && (effectiveTo == null || !effectiveTo.isBefore(today));
  }
}
