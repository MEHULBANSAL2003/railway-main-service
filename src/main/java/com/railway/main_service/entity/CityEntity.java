package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;

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
public class CityEntity {

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

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

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
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
