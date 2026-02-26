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
  name = "states",
  indexes = {
    @Index(name = "idx_state_code", columnList = "code"),
    @Index(name = "idx_state_name", columnList = "name")
  },
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_state_code", columnNames = "code"),
    @UniqueConstraint(name = "uk_state_name", columnNames = "name")
  }
)
public class StateEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "state_id")
  private Long id;

  @Column(nullable = false, length = 5)
  private String code;   // MH, DL, TN

  @Column(nullable = false, length = 100)
  private String name;   // Maharashtra, Delhi

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // One state has many cities
  @OneToMany(mappedBy = "state", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<CityEntity> cities = new ArrayList<>();

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
