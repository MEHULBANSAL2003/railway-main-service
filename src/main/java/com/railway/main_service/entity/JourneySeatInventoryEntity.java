package com.railway.main_service.entity;

import com.railway.main_service.enums.QuotaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(
  name = "journey_seat_inventory",
  schema = "railway_main",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_inventory_journey_coach_quota",
    columnNames = { "journey_id", "coach_id", "quota_type" }
  )
)
public class JourneySeatInventoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "inventory_id")
  private Long inventoryId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "journey_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_inventory_journey"))
  private JourneyEntity journey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "coach_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_inventory_coach"))
  private TrainCoachEntity trainCoach;

  @Enumerated(EnumType.STRING)
  @Column(name = "quota_type", nullable = false, length = 10)
  private QuotaType quotaType;               // GENERAL | TATKAL

  // ── Confirmed seats ───────────────────────────────────────────────────────
  // Snapshot at journey creation: coachType.totalSeats × trainCoach.coachCount
  @Column(name = "total_seats", nullable = false)
  private Integer totalSeats;

  @Column(name = "booked_confirmed", nullable = false)
  @Builder.Default
  private Integer bookedConfirmed = 0;

  // ── RAC — GENERAL only, null for TATKAL ──────────────────────────────────
  // Snapshot: trainCoach.racSeats × trainCoach.coachCount
  @Column(name = "total_rac")
  private Integer totalRac;

  @Column(name = "booked_rac")
  @Builder.Default
  private Integer bookedRac = 0;

  // ── Waitlist — GENERAL only, null for TATKAL ─────────────────────────────
  // Snapshot: trainCoach.waitlistLimit (flat — not multiplied by coachCount)
  @Column(name = "waitlist_limit")
  private Integer waitlistLimit;

  @Column(name = "booked_waitlist")
  @Builder.Default
  private Integer bookedWaitlist = 0;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // ── Derived helpers (used during booking) ────────────────────────────────

  @Transient
  public int availableConfirmed() {
    return totalSeats - bookedConfirmed;
  }

  @Transient
  public int availableRac() {
    if (totalRac == null || totalRac == 0) return 0;
    return totalRac - bookedRac;
  }

  @Transient
  public int availableWaitlist() {
    if (waitlistLimit == null || waitlistLimit == 0) return 0;
    return waitlistLimit - bookedWaitlist;
  }

  @Transient
  public boolean hasAvailability() {
    return availableConfirmed() > 0 || availableRac() > 0 || availableWaitlist() > 0;
  }
}
