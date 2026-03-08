package com.railway.main_service.service.trainStopService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.trainStop.AddTrainStopRequest;
import com.railway.main_service.dto.request.trainStop.UpdateTrainStopRequest;
import com.railway.main_service.dto.response.trainStop.TrainStopResponse;
import com.railway.main_service.entity.StationEntity;
import com.railway.main_service.entity.TrainEntity;
import com.railway.main_service.entity.TrainStopEntity;
import com.railway.main_service.repository.StationRepository;
import com.railway.main_service.repository.TrainRepository;
import com.railway.main_service.repository.TrainStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service @Loggable @Slf4j @RequiredArgsConstructor
public class TrainStopServiceImpl implements TrainStopService {

  private final TrainRepository     trainRepository;
  private final TrainStopRepository trainStopRepository;
  private final StationRepository   stationRepository;

  // ── Get All ───────────────────────────────────────────────────────────────
  @Override
  public List<TrainStopResponse> getAllByTrain(String trainNumber) {
    TrainEntity train = findTrain(trainNumber);
    List<TrainStopEntity> stops =
      trainStopRepository.findAllByTrainId(train.getTrainId());
    return buildResponses(stops, trainNumber);
  }

  // ── Add Stop ──────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainStopResponse addStop(String trainNumber, AddTrainStopRequest req) {
    TrainEntity train = findTrain(trainNumber);

    // Resolve station
    String stationCode = req.getStationCode().trim().toUpperCase();
    StationEntity station = stationRepository.findByStationCode(stationCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "STATION_NOT_FOUND",
        "Station not found: " + stationCode));

    if (!station.getIsActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "STATION_INACTIVE",
        "Station '" + stationCode + "' is inactive and cannot be added as a stop.");

    // Duplicate station check
    if (trainStopRepository.existsByTrain_TrainIdAndStation_Id(
      train.getTrainId(), station.getId()))
      throw new BaseException(HttpStatus.CONFLICT, "STOP_ALREADY_EXISTS",
        "Station '" + stationCode + "' is already a stop on train " + trainNumber + ".");

    // Determine stop number
    int maxStop   = trainStopRepository.findMaxStopNumber(train.getTrainId());
    int stopNumber;

    if (req.getStopNumber() != null) {
      stopNumber = req.getStopNumber();
      if (stopNumber <= maxStop) {
        // Insert at position — shift existing stops down
        trainStopRepository.shiftStopNumbersUp(train.getTrainId(), stopNumber);
      }
    } else {
      // Append at end
      stopNumber = maxStop + 1;
    }

    // ── Validations ──────────────────────────────────────────────────────
    // First stop must have km = 0
    if (stopNumber == 1 && req.getKmFromSource() != 0)
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_KM",
        "First stop (source) must have km_from_source = 0.");

    // KM must be increasing — check previous stop
    if (stopNumber > 1) {
      trainStopRepository.findPreviousStop(train.getTrainId(), stopNumber)
        .ifPresent(prev -> {
          if (req.getKmFromSource() <= prev.getKmFromSource())
            throw new BaseException(HttpStatus.BAD_REQUEST, "KM_NOT_INCREASING",
              "KM from source (" + req.getKmFromSource() + ") must be greater than " +
                "previous stop '" + prev.getStation().getStationCode() +
                "' (" + prev.getKmFromSource() + " km).");
        });
    }

    // KM must be less than next stop (if inserting mid-route)
    trainStopRepository.findNextStop(train.getTrainId(), stopNumber)
      .ifPresent(next -> {
        if (req.getKmFromSource() >= next.getKmFromSource())
          throw new BaseException(HttpStatus.BAD_REQUEST, "KM_NOT_INCREASING",
            "KM from source (" + req.getKmFromSource() + ") must be less than " +
              "next stop '" + next.getStation().getStationCode() +
              "' (" + next.getKmFromSource() + " km).");
      });

    // First stop — no arrival time allowed
    if (stopNumber == 1 && req.getArrivalTime() != null && !req.getArrivalTime().isBlank())
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TIMING",
        "First stop (source) cannot have an arrival time.");

    // Non-first stop — arrival time required
    if (stopNumber > 1 && (req.getArrivalTime() == null || req.getArrivalTime().isBlank()))
      throw new BaseException(HttpStatus.BAD_REQUEST, "MISSING_ARRIVAL",
        "Arrival time is required for stop #" + stopNumber + ".");

    // Parse times
    LocalTime arrival   = parseTime(req.getArrivalTime(),   "arrival");
    LocalTime departure = parseTime(req.getDepartureTime(), "departure");

    TrainStopEntity entity = TrainStopEntity.builder()
      .train(train)
      .station(station)
      .stopNumber(stopNumber)
      .kmFromSource(req.getKmFromSource())
      .arrivalTime(arrival)
      .departureTime(departure)
      .dayNumber(req.getDayNumber())
      .createdBy(SecurityUtils.getCurrentAdminId())
      .build();

    TrainStopEntity saved = trainStopRepository.save(entity);
    log.info("Stop added: train={} station={} stop#={}", trainNumber, stationCode, stopNumber);

    return toResponse(saved, null, trainNumber, "Stop added successfully.");
  }

  // ── Update Stop ───────────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainStopResponse updateStop(String trainNumber, Long stopId,
                                      UpdateTrainStopRequest req) {
    TrainEntity train = findTrain(trainNumber);
    TrainStopEntity stop = trainStopRepository
      .findByStopIdAndTrain_TrainId(stopId, train.getTrainId())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "STOP_NOT_FOUND",
        "Stop not found on train " + trainNumber + "."));

    int totalStops = trainStopRepository.countByTrain_TrainId(train.getTrainId());
    boolean isFirst = stop.getStopNumber() == 1;
    boolean isLast  = stop.getStopNumber() == totalStops;

    // Update km
    if (req.getKmFromSource() != null) {
      if (isFirst && req.getKmFromSource() != 0)
        throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_KM",
          "First stop must have km_from_source = 0.");

      // Validate ordering against neighbours
      if (!isFirst) {
        trainStopRepository.findPreviousStop(train.getTrainId(), stop.getStopNumber())
          .ifPresent(prev -> {
            if (req.getKmFromSource() <= prev.getKmFromSource())
              throw new BaseException(HttpStatus.BAD_REQUEST, "KM_NOT_INCREASING",
                "KM (" + req.getKmFromSource() + ") must be greater than previous stop " +
                  prev.getStation().getStationCode() + " (" + prev.getKmFromSource() + " km).");
          });
      }
      if (!isLast) {
        trainStopRepository.findNextStop(train.getTrainId(), stop.getStopNumber())
          .ifPresent(next -> {
            if (req.getKmFromSource() >= next.getKmFromSource())
              throw new BaseException(HttpStatus.BAD_REQUEST, "KM_NOT_INCREASING",
                "KM (" + req.getKmFromSource() + ") must be less than next stop " +
                  next.getStation().getStationCode() + " (" + next.getKmFromSource() + " km).");
          });
      }
      stop.setKmFromSource(req.getKmFromSource());
    }

    // Update arrival (blocked on first stop)
    if (req.getArrivalTime() != null) {
      if (isFirst && !req.getArrivalTime().isBlank())
        throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TIMING",
          "First stop cannot have an arrival time.");
      stop.setArrivalTime(req.getArrivalTime().isBlank()
        ? null : parseTime(req.getArrivalTime(), "arrival"));
    }

    // Update departure (blocked on last stop)
    if (req.getDepartureTime() != null) {
      if (isLast && !req.getDepartureTime().isBlank())
        throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TIMING",
          "Last stop cannot have a departure time.");
      stop.setDepartureTime(req.getDepartureTime().isBlank()
        ? null : parseTime(req.getDepartureTime(), "departure"));
    }

    if (req.getDayNumber() != null) stop.setDayNumber(req.getDayNumber());

    stop.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    TrainStopEntity saved = trainStopRepository.save(stop);

    return toResponse(saved, null, trainNumber, "Stop updated successfully.");
  }

  // ── Delete Stop ───────────────────────────────────────────────────────────
  @Override
  @Transactional
  public void deleteStop(String trainNumber, Long stopId) {
    TrainEntity train = findTrain(trainNumber);
    TrainStopEntity stop = trainStopRepository
      .findByStopIdAndTrain_TrainId(stopId, train.getTrainId())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "STOP_NOT_FOUND",
        "Stop not found on train " + trainNumber + "."));

    int deletedStopNumber = stop.getStopNumber();
    trainStopRepository.delete(stop);

    // Re-sequence stop numbers
    trainStopRepository.shiftStopNumbersDown(train.getTrainId(), deletedStopNumber);
    log.info("Stop deleted: train={} station={}", trainNumber,
      stop.getStation().getStationCode());
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private TrainEntity findTrain(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber.trim())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private LocalTime parseTime(String time, String fieldName) {
    if (time == null || time.isBlank()) return null;
    try {
      return LocalTime.parse(time.trim());
    } catch (DateTimeParseException e) {
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TIME_FORMAT",
        "Invalid " + fieldName + " time format. Use HH:mm (e.g. 06:30).");
    }
  }

  // Build response list with kmFromPrevious derived
  private List<TrainStopResponse> buildResponses(List<TrainStopEntity> stops,
                                                 String trainNumber) {
    List<TrainStopResponse> result = new ArrayList<>();
    Integer prevKm = null;
    for (TrainStopEntity stop : stops) {
      Integer kmFromPrev = prevKm != null ? stop.getKmFromSource() - prevKm : null;
      result.add(toResponse(stop, kmFromPrev, trainNumber, null));
      prevKm = stop.getKmFromSource();
    }
    return result;
  }

  private TrainStopResponse toResponse(TrainStopEntity e, Integer kmFromPrevious,
                                       String trainNumber, String message) {
    return TrainStopResponse.builder()
      .stopId(e.getStopId())
      .trainNumber(trainNumber)
      .stationId(e.getStation().getId())
      .stationCode(e.getStation().getStationCode())
      .stationName(e.getStation().getStationName())
      .stationType(e.getStation().getStationType() != null
        ? e.getStation().getStationType().name() : null)
      .stopNumber(e.getStopNumber())
      .kmFromSource(e.getKmFromSource())
      .kmFromPrevious(kmFromPrevious)
      .arrivalTime(e.getArrivalTime() != null
        ? e.getArrivalTime().toString() : null)
      .departureTime(e.getDepartureTime() != null
        ? e.getDepartureTime().toString() : null)
      .dayNumber(e.getDayNumber())
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }
}
