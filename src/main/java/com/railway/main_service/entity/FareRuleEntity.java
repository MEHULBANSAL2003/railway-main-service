package com.railway.main_service.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    columnNames = {"train_type_id", "coach_type_id", "effective_from"}
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

  @Column(name = "base_fare_per_km", nullable = false, precision = 8, scale = 2)
  private BigDecimal baseFarePerKm;

  @Column(name = "min_fare", nullable = false, precision = 8, scale = 2)
  private BigDecimal minFare;

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
  private LocalDate effectiveUntil;       // NULL = currently active

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
