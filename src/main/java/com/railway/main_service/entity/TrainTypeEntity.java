package com.railway.main_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "train_types", schema = "railway_main")
public class TrainTypeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "type_id")
  private Long typeId;

  @Column(name = "type_code", nullable = false, length = 20, unique = true)
  private String typeCode;

  @Column(name = "type_name", nullable = false, length = 100)
  private String typeName;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "typical_speed_kmh")
  private Integer typicalSpeedKmh;

  @Column(name = "is_superfast", nullable = false)
  @Builder.Default
  private Boolean isSuperfast = false;

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
