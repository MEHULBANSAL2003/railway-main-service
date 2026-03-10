package com.railway.main_service.entity;

import com.railway.main_service.enums.JourneyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name   = "journeys",
  schema = "railway_main",
  uniqueConstraints = {
    @UniqueConstraint(
      name        = "uk_journey_train_date",
      columnNames = {"train_id", "journey_date"}
    )
  }
)
public class JourneyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "journey_id")
  private Long journeyId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
    name       = "train_id",
    nullable   = false,
    foreignKey = @ForeignKey(name = "fk_journey_train")
  )
  private TrainEntity train;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
    name       = "schedule_id",
    nullable   = false,
    foreignKey = @ForeignKey(name = "fk_journey_schedule")
  )
  private TrainScheduleEntity schedule;

  @Column(name = "journey_date", nullable = false)
  private LocalDate journeyDate;

  // Only true when admin manually cancels a specific run
  @Column(name = "is_cancelled", nullable = false)
  @Builder.Default
  private Boolean isCancelled = false;

  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  // Set true by ChartPreparationJob 4 hours before departure
  @Column(name = "chart_prepared", nullable = false)
  @Builder.Default
  private Boolean chartPrepared = false;

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

  // ── Derive status at runtime — never stored ───────────────────────────────
  @Transient
  public JourneyStatus deriveStatus(LocalTime sourceDeparture) {
    if (Boolean.TRUE.equals(isCancelled)) return JourneyStatus.CANCELLED;

    LocalDate today = LocalDate.now();
    LocalTime now   = LocalTime.now();

    if (journeyDate.isAfter(today)) return JourneyStatus.SCHEDULED;

    if (journeyDate.isEqual(today)) {
      if (sourceDeparture == null || now.isBefore(sourceDeparture))
        return JourneyStatus.SCHEDULED;
      return JourneyStatus.DEPARTED;
    }

    return JourneyStatus.COMPLETED;
  }
}
