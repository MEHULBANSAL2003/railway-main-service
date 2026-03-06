package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fare_rules", schema = "railway_main",
  uniqueConstraints = @UniqueConstraint(
    name = "uq_fare_rule_combo",
    columnNames = {"train_type_id", "coach_type_id", "quota_id", "effective_from"}
  )
)
public class FareRuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rule_id")
  private Long ruleId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "train_type_id", nullable = false)
  private TrainTypeEntity trainType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coach_type_id", nullable = false)
  private CoachTypeEntity coachType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quota_id", nullable = false)
  private QuotaEntity quota;

  @Column(name = "base_fare_per_km", nullable = false, precision = 8, scale = 4)
  private BigDecimal baseFarePerKm;

  @Column(name = "min_fare", nullable = false, precision = 8, scale = 2)
  private BigDecimal minFare;

  @Column(name = "tatkal_charge", nullable = false, precision = 8, scale = 2)
  @Builder.Default
  private BigDecimal tatkalCharge = BigDecimal.ZERO;

  @Column(name = "reservation_charge", nullable = false, precision = 6, scale = 2)
  @Builder.Default
  private BigDecimal reservationCharge = BigDecimal.ZERO;

  @Column(name = "superfast_charge", nullable = false, precision = 6, scale = 2)
  @Builder.Default
  private BigDecimal superfastCharge = BigDecimal.ZERO;

  @Column(name = "gst_pct", nullable = false, precision = 4, scale = 2)
  @Builder.Default
  private BigDecimal gstPct = BigDecimal.ZERO;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_until")
  private LocalDate effectiveUntil;

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
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
