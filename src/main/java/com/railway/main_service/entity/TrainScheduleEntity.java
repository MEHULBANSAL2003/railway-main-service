package com.railway.main_service.entity;

import com.railway.main_service.enums.RunDay;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name   = "train_schedules",
  schema = "railway_main"
)
public class TrainScheduleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "schedule_id")
  private Long scheduleId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "train_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_schedule_train"))
  private TrainEntity train;

  @Column(name = "runs_on_days", nullable = false, length = 50)
  private String runsOnDays;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

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

  @Transient
  public boolean isCurrentlyActive() {
    return isActiveOn(LocalDate.now());
  }

  @Transient
  public boolean isActiveOn(LocalDate date) {
    if (date.isBefore(startDate)) return false;
    return endDate == null || !date.isAfter(endDate);
  }

  @Transient
  public Set<RunDay> getRunDaysAsSet() {
    if (runsOnDays == null || runsOnDays.isBlank()) return EnumSet.noneOf(RunDay.class);
    Set<RunDay> days = EnumSet.noneOf(RunDay.class);
    for (String d : runsOnDays.split(",")) {
      try { days.add(RunDay.valueOf(d.trim())); } catch (IllegalArgumentException ignored) {}
    }
    return days;
  }

  public static String toDayString(Set<RunDay> days) {
    if (days == null || days.isEmpty()) return "";
    return days.stream()
      .sorted(java.util.Comparator.comparingInt(Enum::ordinal))
      .map(Enum::name)
      .collect(java.util.stream.Collectors.joining(","));
  }
}
