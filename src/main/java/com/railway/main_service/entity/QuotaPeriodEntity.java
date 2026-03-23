package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name   = "quota_periods",
  schema = "railway_main",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_quota_period",
    columnNames = {"quota_id", "effective_from"}
  ),
  indexes = @Index(name = "idx_quota_period_lookup", columnList = "quota_id, effective_from, effective_till")
)
public class QuotaPeriodEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "period_id")
  private Long periodId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quota_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_quota_period"))
  private QuotaEntity quota;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_till")
  private LocalDate effectiveTill;

  @Column(name = "reason", length = 500)
  private String reason; 

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() { createdAt = LocalDateTime.now(); }

  @Transient
  public boolean coversDate(LocalDate date) {
    if (date.isBefore(effectiveFrom)) return false;
    return effectiveTill == null || !date.isAfter(effectiveTill);
  }
}
