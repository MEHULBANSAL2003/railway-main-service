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
  name = "cities",
  indexes = {
    @Index(name = "idx_city_name", columnList = "name"),
    @Index(name = "idx_city_state", columnList = "state_id")
  },
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_city_name_state",
      columnNames = {"name", "state_id"}
    )
  }
)
public class CityEntity implements Activatable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "city_id")
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  // Many cities belong to one state
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
    name = "state_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "fk_city_state")
  )
  private StateEntity state;

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

  // One city has many stations
  @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
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
