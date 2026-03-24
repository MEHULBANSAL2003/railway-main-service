package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
  name = "zones",
  indexes = {
    @Index(name = "idx_zone_code", columnList = "code"),
    @Index(name = "idx_zone_name", columnList = "name")
  },
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_zone_code", columnNames = "code"),
    @UniqueConstraint(name = "uk_zone_name", columnNames = "name")
  }
)
public class ZoneEntity implements Activatable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "zone_id")
  private Long id;

  @Column(nullable = false, length = 10)
  private String code;   // NR, WR, SR

  @Column(nullable = false, length = 150)
  private String name;   // Northern Railway

  // ── Effective date range (replaces isActive) ────────────────────────────
  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_till")
  private LocalDate effectiveTill;

  @Column(name = "reason", length = 500)
  private String reason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // One zone has many stations
  @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<StationEntity> stations = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (effectiveFrom == null) effectiveFrom = LocalDate.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
