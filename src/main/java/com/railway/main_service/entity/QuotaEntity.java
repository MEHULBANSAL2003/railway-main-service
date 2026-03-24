package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "quotas", schema = "railway_main")
public class QuotaEntity implements Activatable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "quota_id")
  private Long quotaId;

  @Column(name = "quota_code", nullable = false, unique = true, length = 20)
  private String quotaCode;

  @Column(name = "quota_name", nullable = false, length = 50)
  private String quotaName;

  @Column(name = "description", length = 255)
  private String description;

  // ── Effective date range (replaces isActive) ────────────────────────────
  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_till")
  private LocalDate effectiveTill;

  @Column(name = "reason", length = 500)
  private String reason;

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
    createdAt = updatedAt = LocalDateTime.now();
    if (effectiveFrom == null) effectiveFrom = LocalDate.now();
  }

  @PreUpdate
  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
