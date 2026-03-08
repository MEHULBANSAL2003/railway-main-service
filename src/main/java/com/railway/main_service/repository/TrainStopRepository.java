package com.railway.main_service.repository;

import com.railway.main_service.entity.TrainStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainStopRepository extends JpaRepository<TrainStopEntity, Long> {

  // All stops for a train ordered by stop_number
  @Query("SELECT ts FROM TrainStopEntity ts " +
    "JOIN FETCH ts.station s " +
    "WHERE ts.train.trainId = :trainId " +
    "ORDER BY ts.stopNumber ASC")
  List<TrainStopEntity> findAllByTrainId(@Param("trainId") Long trainId);

  // Ownership check — stop belongs to this train
  Optional<TrainStopEntity> findByStopIdAndTrain_TrainId(Long stopId, Long trainId);

  // Duplicate checks
  boolean existsByTrain_TrainIdAndStation_Id(Long trainId, Long stationId);
  boolean existsByTrain_TrainIdAndStopNumber(Long trainId, Integer stopNumber);

  // Max stop number — used to auto-append
  @Query("SELECT COALESCE(MAX(ts.stopNumber), 0) FROM TrainStopEntity ts " +
    "WHERE ts.train.trainId = :trainId")
  int findMaxStopNumber(@Param("trainId") Long trainId);

  // Count stops
  int countByTrain_TrainId(Long trainId);

  // Find stop just before a given km — used for km ordering validation
  @Query("SELECT ts FROM TrainStopEntity ts " +
    "WHERE ts.train.trainId = :trainId " +
    "AND ts.stopNumber = :stopNumber - 1")
  Optional<TrainStopEntity> findPreviousStop(
    @Param("trainId") Long trainId,
    @Param("stopNumber") int stopNumber);

  // Find stop just after a given stop number — used for km ordering validation
  @Query("SELECT ts FROM TrainStopEntity ts " +
    "WHERE ts.train.trainId = :trainId " +
    "AND ts.stopNumber = :stopNumber + 1")
  Optional<TrainStopEntity> findNextStop(
    @Param("trainId") Long trainId,
    @Param("stopNumber") int stopNumber);

  // Shift stop numbers UP by 1 from a given position (for mid-route insert)
  @Modifying
  @Query("UPDATE TrainStopEntity ts SET ts.stopNumber = ts.stopNumber + 1 " +
    "WHERE ts.train.trainId = :trainId AND ts.stopNumber >= :fromStop")
  void shiftStopNumbersUp(
    @Param("trainId") Long trainId,
    @Param("fromStop") int fromStop);

  // Shift stop numbers DOWN by 1 after deletion
  @Modifying
  @Query("UPDATE TrainStopEntity ts SET ts.stopNumber = ts.stopNumber - 1 " +
    "WHERE ts.train.trainId = :trainId AND ts.stopNumber > :afterStop")
  void shiftStopNumbersDown(
    @Param("trainId") Long trainId,
    @Param("afterStop") int afterStop);
}
