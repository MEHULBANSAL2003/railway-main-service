package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name = "train_type_coach_types",
  schema = "railway_main",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_train_type_coach",
      columnNames = { "train_type_id", "coach_type_id" })
  }
)
public class TrainTypeCoachEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "train_type_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_ttc_train_type"))
  private TrainTypeEntity trainType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "coach_type_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_ttc_coach_type"))
  private CoachTypeEntity coachType;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() { createdAt = LocalDateTime.now(); }
}
