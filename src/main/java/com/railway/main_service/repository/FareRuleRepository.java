package com.railway.main_service.repository;

import com.railway.main_service.entity.FareRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FareRuleRepository extends JpaRepository<FareRuleEntity, Long> {

  // ── Duplicate check before insert ─────────────────────────────────────────
  // Includes quota — GENERAL and TATKAL are separate rows for same train+coach combo
  boolean existsByTrainType_TypeCodeAndCoachType_TypeCodeAndQuota_QuotaCodeAndEffectiveFrom(
    String trainTypeCode, String coachTypeCode, String quotaCode, LocalDate effectiveFrom);

  // ── Count active rules linked to a parent on a given date ─────────────────
  // Active = effectiveFrom <= date AND (effectiveUntil IS NULL OR effectiveUntil >= date)
  @Query("SELECT COUNT(f) FROM FareRuleEntity f " +
    "WHERE f.coachType.typeCode = :typeCode " +
    "AND f.effectiveFrom <= :date " +
    "AND (f.effectiveUntil IS NULL OR f.effectiveUntil >= :date)")
  int countActiveByCoachTypeCodeOnDate(
    @Param("typeCode") String typeCode,
    @Param("date")     LocalDate date);

  @Query("SELECT COUNT(f) FROM FareRuleEntity f " +
    "WHERE f.trainType.typeCode = :typeCode " +
    "AND f.effectiveFrom <= :date " +
    "AND (f.effectiveUntil IS NULL OR f.effectiveUntil >= :date)")
  int countActiveByTrainTypeCodeOnDate(
    @Param("typeCode") String typeCode,
    @Param("date")     LocalDate date);

  @Query("SELECT COUNT(f) FROM FareRuleEntity f " +
    "WHERE f.quota.quotaCode = :quotaCode " +
    "AND f.effectiveFrom <= :date " +
    "AND (f.effectiveUntil IS NULL OR f.effectiveUntil >= :date)")
  int countActiveByQuotaCodeOnDate(
    @Param("quotaCode") String quotaCode,
    @Param("date")      LocalDate date);

  // ── Cascade end-date — set effectiveUntil when parent is deactivated ──────
  // Sets effectiveUntil on currently-open rules (effectiveUntil IS NULL)
  @Modifying
  @Query("UPDATE FareRuleEntity f SET f.effectiveUntil = :endDate, f.updatedBy = :adminId " +
    "WHERE f.coachType.typeCode = :typeCode " +
    "AND f.effectiveFrom <= :endDate " +
    "AND f.effectiveUntil IS NULL")
  int endDateByCoachTypeCode(@Param("typeCode") String typeCode,
                             @Param("endDate")  LocalDate endDate,
                             @Param("adminId")  Long adminId);

  @Modifying
  @Query("UPDATE FareRuleEntity f SET f.effectiveUntil = :endDate, f.updatedBy = :adminId " +
    "WHERE f.trainType.typeCode = :typeCode " +
    "AND f.effectiveFrom <= :endDate " +
    "AND f.effectiveUntil IS NULL")
  int endDateByTrainTypeCode(@Param("typeCode") String typeCode,
                             @Param("endDate")  LocalDate endDate,
                             @Param("adminId")  Long adminId);

  @Modifying
  @Query("UPDATE FareRuleEntity f SET f.effectiveUntil = :endDate, f.updatedBy = :adminId " +
    "WHERE f.quota.quotaCode = :quotaCode " +
    "AND f.effectiveFrom <= :endDate " +
    "AND f.effectiveUntil IS NULL")
  int endDateByQuotaCode(@Param("quotaCode") String quotaCode,
                         @Param("endDate")   LocalDate endDate,
                         @Param("adminId")   Long adminId);

  // ── Admin table — all rules with optional filters ─────────────────────────
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN FETCH f.trainType tt JOIN FETCH f.coachType ct JOIN FETCH f.quota q " +
    "WHERE (:trainTypeCode IS NULL OR tt.typeCode = CAST(:trainTypeCode AS string)) " +
    "AND (:coachTypeCode IS NULL OR ct.typeCode = CAST(:coachTypeCode AS string)) " +
    "AND (:quotaCode IS NULL OR q.quotaCode = CAST(:quotaCode AS string)) " +
    "ORDER BY tt.typeCode ASC, ct.typeCode ASC, q.quotaCode ASC, f.effectiveFrom DESC")
  List<FareRuleEntity> findAllForAdmin(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode,
    @Param("quotaCode") String quotaCode);

  // ── Current active rule for a combo on a given date ───────────────────────
  // Active is derived from dates: effectiveFrom <= date AND (effectiveUntil IS NULL OR effectiveUntil >= date)
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN FETCH f.trainType tt JOIN FETCH f.coachType ct JOIN FETCH f.quota q " +
    "WHERE tt.typeCode = :trainTypeCode AND ct.typeCode = :coachTypeCode " +
    "AND q.quotaCode = :quotaCode " +
    "AND f.effectiveFrom <= :date " +
    "AND (f.effectiveUntil IS NULL OR f.effectiveUntil >= :date) " +
    "ORDER BY f.effectiveFrom DESC")
  Optional<FareRuleEntity> findCurrentRule(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode,
    @Param("quotaCode") String quotaCode,
    @Param("date") LocalDate date);

  // ── Full history for one combo — shown in history drawer ──────────────────
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN FETCH f.trainType tt JOIN FETCH f.coachType ct JOIN FETCH f.quota q " +
    "WHERE tt.typeCode = :trainTypeCode AND ct.typeCode = :coachTypeCode " +
    "AND q.quotaCode = :quotaCode " +
    "ORDER BY f.effectiveFrom DESC")
  List<FareRuleEntity> findAllByCombo(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode,
    @Param("quotaCode") String quotaCode);

  // ── Open rule (no effectiveUntil) for a combo ─────────────────────────────
  // Used when adding a new rule — auto-closes the previous open rule
  // by setting its effectiveUntil = newRule.effectiveFrom - 1 day
  @Query("SELECT f FROM FareRuleEntity f " +
    "JOIN f.trainType tt JOIN f.coachType ct JOIN f.quota q " +
    "WHERE tt.typeCode = :trainTypeCode AND ct.typeCode = :coachTypeCode " +
    "AND q.quotaCode = :quotaCode " +
    "AND f.effectiveUntil IS NULL " +
    "ORDER BY f.effectiveFrom DESC")
  Optional<FareRuleEntity> findOpenRule(
    @Param("trainTypeCode") String trainTypeCode,
    @Param("coachTypeCode") String coachTypeCode,
    @Param("quotaCode") String quotaCode);
}
