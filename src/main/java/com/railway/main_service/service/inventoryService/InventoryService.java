package com.railway.main_service.service.inventoryService;

import com.railway.common.exceptions.BaseException;
import com.railway.main_service.dto.response.inventory.CoachInventoryResponse;
import com.railway.main_service.dto.response.inventory.JourneyInventoryResponse;
import com.railway.main_service.dto.response.inventory.QuotaInventoryResponse;
import com.railway.main_service.entity.JourneyEntity;
import com.railway.main_service.entity.JourneySeatInventoryEntity;
import com.railway.main_service.entity.TrainCoachEntity;
import com.railway.main_service.entity.TrainStopEntity;
import com.railway.main_service.enums.QuotaType;
import com.railway.main_service.repository.JourneyRepository;
import com.railway.main_service.repository.JourneySeatInventoryRepository;
import com.railway.main_service.repository.TrainStopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

  private static final DateTimeFormatter DATE_FMT =
    DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final JourneySeatInventoryRepository inventoryRepository;
  private final JourneyRepository              journeyRepository;
  private final TrainStopRepository            trainStopRepository;

  @Transactional(readOnly = true)
  public JourneyInventoryResponse getForJourney(String trainNumber, Long journeyId) {

    JourneyEntity journey = journeyRepository.findById(journeyId)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND,
        "JOURNEY_NOT_FOUND", "Journey not found: " + journeyId));

    if (!journey.getTrain().getTrainNumber().equals(trainNumber))
      throw new BaseException(HttpStatus.BAD_REQUEST,
        "JOURNEY_TRAIN_MISMATCH", "Journey does not belong to train " + trainNumber);

    LocalTime srcDep = trainStopRepository
      .findSourceStop(journey.getTrain().getTrainId())
      .map(TrainStopEntity::getDepartureTime)
      .orElse(null);

    List<JourneySeatInventoryEntity> rows = inventoryRepository.findByJourneyId(journeyId);

    // Group by coachId
    Map<Long, List<JourneySeatInventoryEntity>> byCoach = rows.stream()
      .collect(Collectors.groupingBy(r -> r.getTrainCoach().getCoachId()));

    List<CoachInventoryResponse> coaches = new ArrayList<>();

    byCoach.forEach((coachId, coachRows) -> {
      TrainCoachEntity tc = coachRows.get(0).getTrainCoach();

      JourneySeatInventoryEntity generalRow = coachRows.stream()
        .filter(r -> r.getQuotaType() == QuotaType.GENERAL)
        .findFirst().orElse(null);

      JourneySeatInventoryEntity tatkalRow = coachRows.stream()
        .filter(r -> r.getQuotaType() == QuotaType.TATKAL)
        .findFirst().orElse(null);

      coaches.add(CoachInventoryResponse.builder()
        .coachId(coachId)
        .coachTypeCode(tc.getCoachType().getTypeCode())
        .coachTypeName(tc.getCoachType().getTypeName())
        .isAc(Boolean.TRUE.equals(tc.getCoachType().getIsAc()))
        .coachCount(tc.getCoachCount())
        .general(generalRow != null ? toQuotaDto(generalRow) : null)
        .tatkal(tatkalRow  != null ? toQuotaDto(tatkalRow)  : null)
        .build());
    });

    coaches.sort((a, b) -> a.getCoachTypeCode().compareTo(b.getCoachTypeCode()));

    return JourneyInventoryResponse.builder()
      .journeyId(journeyId)
      .journeyDate(journey.getJourneyDate().format(DATE_FMT))
      .status(journey.deriveStatus(srcDep).name())
      .coaches(coaches)
      .build();
  }

  private QuotaInventoryResponse toQuotaDto(JourneySeatInventoryEntity r) {
    return QuotaInventoryResponse.builder()
      .inventoryId(r.getInventoryId())
      .quotaType(r.getQuotaType().name())
      .totalSeats(r.getTotalSeats())
      .bookedConfirmed(r.getBookedConfirmed())
      .availableConfirmed(r.availableConfirmed())
      .totalRac(r.getTotalRac())
      .bookedRac(r.getTotalRac() != null ? r.getBookedRac() : null)
      .availableRac(r.getTotalRac() != null ? r.availableRac() : null)
      .waitlistLimit(r.getWaitlistLimit())
      .bookedWaitlist(r.getWaitlistLimit() != null ? r.getBookedWaitlist() : null)
      .availableWaitlist(r.getWaitlistLimit() != null ? r.availableWaitlist() : null)
      .build();
  }
}
