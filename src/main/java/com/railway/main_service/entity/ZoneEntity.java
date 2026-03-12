package com.railway.main_service.entity;

import com.railway.common.models.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "zones")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "zone_id", updatable = false, nullable = false)
  private Long zoneId;

  @Column(name = "name", nullable = false, length = 100)
  private String zoneName;

  @Column(name = "code", nullable = false, length = 10)
  private String zoneCode;
}
