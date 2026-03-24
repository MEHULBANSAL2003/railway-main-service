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
public class TrainCoachEntity implements Activatable {

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

  // ── Effective date range ──
  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_till")
  private LocalDate effectiveTill;

  @Column(name = "reason", length = 500)
  private String reason;

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
    if (effectiveFrom == null) effectiveFrom = LocalDate.now();
  }

  @PreUpdate
  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
