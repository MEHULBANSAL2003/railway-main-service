package com.railway.main_service.service.trainService;

import com.railway.common.exceptions.BaseException;
import com.railway.common.logging.Loggable;
import com.railway.common.security.SecurityUtils;
import com.railway.main_service.dto.request.ActivateRequest;
import com.railway.main_service.dto.request.DeactivateRequest;
import com.railway.main_service.dto.request.train.AddTrainRequest;
import com.railway.main_service.dto.request.train.UpdateTrainRequest;
import com.railway.main_service.dto.response.DeactivationResponse;
import com.railway.main_service.dto.response.PageResponse;
import com.railway.main_service.dto.response.PeriodResponse;
import com.railway.main_service.dto.response.cascade.CascadeInfoResponse;
import com.railway.main_service.dto.response.train.BulkUploadResponse;
import com.railway.main_service.dto.response.train.ReturnTrainResponse;
import com.railway.main_service.dto.response.train.TrainResponse;
import com.railway.main_service.entity.*;
import com.railway.main_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Loggable
@Slf4j
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

  private final TrainRepository                trainRepository;
  private final TrainTypeRepository            trainTypeRepository;
  private final ZoneRepository                 zoneRepository;
  private final TrainPeriodRepository          trainPeriodRepository;
  private final TrainTypePeriodRepository      trainTypePeriodRepository;
  private final TrainCoachRepository           trainCoachRepository;
  private final TrainScheduleRepository        trainScheduleRepository;
  private final JourneyRepository              journeyRepository;
  private final JourneySeatInventoryRepository journeySeatInventoryRepository;

  // ── Add ───────────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainResponse addTrain(AddTrainRequest request) {

    String trainNumber = request.getTrainNumber().trim();
    String trainName   = request.getTrainName().trim();
    String typeCode    = request.getTrainTypeCode().trim().toUpperCase();
    String zoneCode    = request.getZoneCode().trim().toUpperCase();

    if (trainRepository.existsByTrainNumber(trainNumber))
      throw new BaseException(HttpStatus.CONFLICT, "TRAIN_NUMBER_EXISTS",
        "Train number '" + trainNumber + "' already exists.");

    TrainTypeEntity trainType = trainTypeRepository.findByTypeCode(typeCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_TYPE_NOT_FOUND",
        "Train type not found: " + typeCode));

    LocalDate today = LocalDate.now();

    // Validate train type is active using period-based check
    if (!trainTypePeriodRepository.isActiveOnDate(trainType.getTypeId(), today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "TRAIN_TYPE_INACTIVE",
        "Train type '" + typeCode + "' is inactive.");

    ZoneEntity zone = findActiveZone(zoneCode);

    Long adminId = SecurityUtils.getCurrentAdminId();

    TrainEntity entity = TrainEntity.builder()
      .trainNumber(trainNumber)
      .trainName(trainName)
      .trainType(trainType)
      .zone(zone)
      .pantrycar(request.getPantrycar())
      .createdBy(adminId)
      .build();

    TrainEntity saved = trainRepository.save(entity);

    // Create initial active period: effectiveFrom=today, effectiveTill=null
    TrainPeriodEntity initialPeriod = TrainPeriodEntity.builder()
      .train(saved)
      .effectiveFrom(today)
      .effectiveTill(null)
      .reason("Train created")
      .createdBy(adminId)
      .build();
    trainPeriodRepository.save(initialPeriod);

    return toResponse(saved, "Train added successfully.");
  }

  // ── Update ────────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public TrainResponse updateTrain(String trainNumber, UpdateTrainRequest request) {

    TrainEntity entity = findByNumber(trainNumber);

    if (request.getTrainName() != null && !request.getTrainName().isBlank()) {
      entity.setTrainName(request.getTrainName());
    }

    if (request.getZoneCode() != null && !request.getZoneCode().isBlank()) {
      ZoneEntity zone = findActiveZone(request.getZoneCode().trim().toUpperCase());
      entity.setZone(zone);
    }

    if (request.getPantrycar() != null)
      entity.setPantrycar(request.getPantrycar());

    entity.setUpdatedBy(SecurityUtils.getCurrentAdminId());
    return toResponse(trainRepository.save(entity), "Train updated successfully.");
  }

  // ── Deactivate ────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public DeactivationResponse deactivate(String trainNumber, DeactivateRequest request) {

    TrainEntity entity = findByNumber(trainNumber);
    Long trainId       = entity.getTrainId();
    Long adminId       = SecurityUtils.getCurrentAdminId();
    LocalDate today    = LocalDate.now();
    LocalDate fromDate = request.getFromDate();
    LocalDate tillDate = request.getTillDate();

    // a. Validate fromDate >= today
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FROM_DATE",
        "fromDate cannot be in the past.");

    // b. Validate tillDate > fromDate if provided
    if (tillDate != null && !tillDate.isAfter(fromDate))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TILL_DATE",
        "tillDate must be after fromDate.");

    // c. Close current open period: set effectiveTill = fromDate - 1
    LocalDate closingDate = fromDate.minusDays(1);
    trainPeriodRepository.closeOpenPeriod(trainId, closingDate);

    // d. If tillDate provided (temporary deactivation): create future reactivation period
    if (tillDate != null) {
      TrainPeriodEntity futurePeriod = TrainPeriodEntity.builder()
        .train(entity)
        .effectiveFrom(tillDate.plusDays(1))
        .effectiveTill(null)
        .reason("Auto-reactivation after temporary deactivation")
        .createdBy(adminId)
        .build();
      trainPeriodRepository.save(futurePeriod);
    }

    // e. Delete any future periods that would overlap
    trainPeriodRepository.deleteFuturePeriods(trainId, fromDate);

    // ── CASCADE ──────────────────────────────────────────────────────────

    List<String> warnings = new ArrayList<>();

    // f. Close TrainCoaches: set effectiveTo = fromDate - 1 on coaches with open effectiveTo
    List<TrainCoachEntity> openCoaches = trainCoachRepository.findActiveByTrainIdOnDate(trainId, fromDate);
    int affectedCoaches = 0;
    for (TrainCoachEntity coach : openCoaches) {
      if (coach.getEffectiveTo() == null || coach.getEffectiveTo().isAfter(closingDate)) {
        coach.setEffectiveTo(closingDate);
        coach.setUpdatedBy(adminId);
        affectedCoaches++;
      }
    }
    if (!openCoaches.isEmpty()) {
      trainCoachRepository.saveAll(openCoaches);
    }

    // g. Close TrainSchedules: set endDate = fromDate - 1 on running schedules
    int affectedSchedules = 0;
    // Close running schedule
    Optional<TrainScheduleEntity> runningOpt = trainScheduleRepository.findRunning(trainId, fromDate);
    if (runningOpt.isPresent()) {
      TrainScheduleEntity running = runningOpt.get();
      if (running.getEndDate() == null || running.getEndDate().isAfter(closingDate)) {
        running.setEndDate(closingDate);
        running.setUpdatedBy(adminId);
        trainScheduleRepository.save(running);
        affectedSchedules++;
      }
    }
    // Close upcoming schedules by setting endDate to their startDate (effectively making them zero-length / cancelled)
    List<TrainScheduleEntity> upcomingSchedules = trainScheduleRepository.findUpcoming(trainId, closingDate);
    for (TrainScheduleEntity upcoming : upcomingSchedules) {
      if (tillDate == null || upcoming.getStartDate().isBefore(tillDate) || upcoming.getStartDate().isEqual(tillDate)) {
        upcoming.setEndDate(closingDate.isBefore(upcoming.getStartDate()) ? upcoming.getStartDate() : closingDate);
        upcoming.setUpdatedBy(adminId);
        affectedSchedules++;
      }
    }
    if (!upcomingSchedules.isEmpty()) {
      trainScheduleRepository.saveAll(upcomingSchedules);
    }

    // h. Cancel future Journeys from fromDate to tillDate (or all future if tillDate is null)
    LocalDate cancelFrom = fromDate;
    LocalDate cancelTo   = tillDate != null ? tillDate : fromDate.plusYears(1); // practical upper bound
    List<JourneyEntity> journeysToCancel = journeyRepository.findByTrainAndDateRange(trainId, cancelFrom, cancelTo);

    int cancelledJourneys   = 0;
    int journeysWithBookings = 0;
    String cancelReason = request.getReason() != null
      ? "Train deactivated: " + request.getReason()
      : "Train deactivated by admin";

    for (JourneyEntity journey : journeysToCancel) {
      if (!Boolean.TRUE.equals(journey.getIsCancelled())) {
        journey.setIsCancelled(true);
        journey.setCancelReason(cancelReason);
        cancelledJourneys++;
      }
    }
    if (!journeysToCancel.isEmpty()) {
      journeyRepository.saveAll(journeysToCancel);
    }

    // i. Delete unbooked inventory for cancelled journeys
    // j. Count journeys that have bookings — add to warnings
    int deletedInventoryRows = 0;
    for (JourneyEntity journey : journeysToCancel) {
      List<JourneySeatInventoryEntity> inventoryRows =
        journeySeatInventoryRepository.findByJourneyId(journey.getJourneyId());

      boolean hasBookings = false;
      List<JourneySeatInventoryEntity> toDelete = new ArrayList<>();

      for (JourneySeatInventoryEntity inv : inventoryRows) {
        boolean booked = (inv.getBookedConfirmed() != null && inv.getBookedConfirmed() > 0)
          || (inv.getBookedRac() != null && inv.getBookedRac() > 0)
          || (inv.getBookedWaitlist() != null && inv.getBookedWaitlist() > 0);

        if (booked) {
          hasBookings = true;
        } else {
          toDelete.add(inv);
        }
      }

      if (hasBookings) {
        journeysWithBookings++;
        warnings.add("Journey on " + journey.getJourneyDate() + " has existing bookings — passengers must be notified.");
      }

      if (!toDelete.isEmpty()) {
        journeySeatInventoryRepository.deleteAll(toDelete);
        deletedInventoryRows += toDelete.size();
      }
    }

    String message;
    if (tillDate != null) {
      message = "Train " + entity.getTrainNumber() + " deactivated from " + fromDate + " to " + tillDate
        + ". Will auto-reactivate on " + tillDate.plusDays(1) + ". "
        + cancelledJourneys + " journeys cancelled.";
    } else {
      message = "Train " + entity.getTrainNumber() + " deactivated from " + fromDate + " indefinitely. "
        + cancelledJourneys + " journeys cancelled.";
    }

    return DeactivationResponse.builder()
      .entityType("TRAIN")
      .entityCode(entity.getTrainNumber())
      .entityName(entity.getTrainName())
      .action("DEACTIVATED")
      .period(toPeriodResponse(
        trainPeriodRepository.findActivePeriod(trainId, closingDate).orElse(null),
        closingDate))
      .affectedFareRules(0)
      .affectedCoaches(affectedCoaches)
      .affectedSchedules(affectedSchedules)
      .cancelledJourneys(cancelledJourneys)
      .deletedInventoryRows(deletedInventoryRows)
      .warnings(warnings.isEmpty() ? null : warnings)
      .message(message)
      .build();
  }

  // ── Activate ──────────────────────────────────────────────────────────────
  @Override
  @Transactional
  public DeactivationResponse activate(String trainNumber, ActivateRequest request) {

    TrainEntity entity = findByNumber(trainNumber);
    Long trainId       = entity.getTrainId();
    Long adminId       = SecurityUtils.getCurrentAdminId();
    LocalDate today    = LocalDate.now();
    LocalDate fromDate = request.getFromDate();
    LocalDate tillDate = request.getTillDate();

    // a. Validate fromDate >= today
    if (fromDate.isBefore(today))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FROM_DATE",
        "fromDate cannot be in the past.");

    // b. Check no overlap with existing periods
    LocalDate overlapTill = tillDate != null ? tillDate : fromDate.plusYears(100);
    if (trainPeriodRepository.hasOverlap(trainId, fromDate, overlapTill, null))
      throw new BaseException(HttpStatus.CONFLICT, "PERIOD_OVERLAP",
        "The requested activation period overlaps with an existing period.");

    // c. Create new period
    TrainPeriodEntity newPeriod = TrainPeriodEntity.builder()
      .train(entity)
      .effectiveFrom(fromDate)
      .effectiveTill(tillDate)
      .reason(request.getReason() != null ? request.getReason() : "Train activated by admin")
      .createdBy(adminId)
      .build();
    TrainPeriodEntity savedPeriod = trainPeriodRepository.save(newPeriod);

    // d. Return DeactivationResponse with action=ACTIVATED
    String message = "Train " + entity.getTrainNumber() + " activated from " + fromDate
      + (tillDate != null ? " to " + tillDate : " indefinitely")
      + ". You must manually: (1) add/reactivate coaches, (2) create schedule, (3) generate journeys.";

    return DeactivationResponse.builder()
      .entityType("TRAIN")
      .entityCode(entity.getTrainNumber())
      .entityName(entity.getTrainName())
      .action("ACTIVATED")
      .period(toPeriodResponse(savedPeriod, today))
      .affectedFareRules(0)
      .affectedCoaches(0)
      .affectedSchedules(0)
      .cancelledJourneys(0)
      .deletedInventoryRows(0)
      .warnings(null)
      .message(message)
      .build();
  }

  // ── Get Periods ───────────────────────────────────────────────────────────
  @Override
  public List<PeriodResponse> getPeriods(String trainNumber) {
    TrainEntity entity = findByNumber(trainNumber);
    LocalDate today = LocalDate.now();
    List<TrainPeriodEntity> periods = trainPeriodRepository.findAllByTrainId(entity.getTrainId());
    return periods.stream()
      .map(p -> toPeriodResponse(p, today))
      .toList();
  }

  // ── Cascade Info ──────────────────────────────────────────────────────────
  @Override
  public CascadeInfoResponse getCascadeInfo(String trainNumber) {
    TrainEntity entity = findByNumber(trainNumber);
    Long trainId = entity.getTrainId();
    LocalDate today = LocalDate.now();

    boolean isActive = trainPeriodRepository.isActiveOnDate(trainId, today);
    int coachCount = trainCoachRepository.countActiveByTrainIdOnDate(trainId, today);

    // Count running + upcoming schedules
    int scheduleCount = 0;
    if (trainScheduleRepository.findRunning(trainId, today).isPresent()) {
      scheduleCount++;
    }
    scheduleCount += trainScheduleRepository.findUpcoming(trainId, today).size();

    // Count future non-cancelled journeys
    List<JourneyEntity> futureJourneys = journeyRepository.findByTrainAndDateRange(
      trainId, today, today.plusYears(1));
    long journeyCount = futureJourneys.stream()
      .filter(j -> !Boolean.TRUE.equals(j.getIsCancelled()))
      .count();

    StringBuilder msg = new StringBuilder();
    if (coachCount > 0) msg.append(coachCount).append(" active coach(es). ");
    if (scheduleCount > 0) msg.append(scheduleCount).append(" active/upcoming schedule(s). ");
    if (journeyCount > 0) msg.append(journeyCount).append(" future journey(s). ");
    if (msg.isEmpty()) msg.append("No linked records.");

    return CascadeInfoResponse.builder()
      .entityType("TRAIN")
      .entityCode(entity.getTrainNumber())
      .entityName(entity.getTrainName())
      .currentlyActive(isActive)
      .activeFareRulesCount(0)
      .message(msg.toString().trim())
      .build();
  }

  // ── Paginated Admin Query ─────────────────────────────────────────────────
  @Override
  public PageResponse<TrainResponse> getAllForAdmin(
    String search,
    String trainTypeCode,
    String zoneCode,
    Boolean isActive,
    int page,
    int size,
    String sortBy,
    String sortDir
  ) {
    // Sanitise filters
    String s  = blankToNull(search);
    String tc = blankToNull(trainTypeCode) != null ? trainTypeCode.trim().toUpperCase() : null;
    String zc = blankToNull(zoneCode)      != null ? zoneCode.trim().toUpperCase()      : null;

    // Whitelist sort fields
    String safeSortBy = switch (sortBy != null ? sortBy.trim() : "") {
      case "trainName" -> "trainName";
      case "pantrycar" -> "pantrycar";
      default          -> "trainNumber";
    };

    Sort sort = "desc".equalsIgnoreCase(sortDir)
      ? Sort.by(safeSortBy).descending()
      : Sort.by(safeSortBy).ascending();

    // API is 1-based, Spring Data is 0-based
    int pageIndex = Math.max(0, page - 1);
    int pageSize  = Math.max(1, Math.min(size, 100));

    Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

    LocalDate today = LocalDate.now();
    Page<TrainEntity> result = trainRepository.findAllForAdminPaged(tc, zc, isActive, s, today, pageable);

    return PageResponse.<TrainResponse>builder()
      .content(result.getContent().stream().map(e -> toResponse(e, null)).toList())
      .totalElements(result.getTotalElements())
      .totalPages(result.getTotalPages())
      .currentPage(page)
      .pageSize(pageSize)
      .first(result.isFirst())
      .last(result.isLast())
      .build();
  }

  // ── Dropdown ──────────────────────────────────────────────────────────────
  @Override
  public List<TrainResponse> getAllForDropdown(String search) {
    LocalDate today = LocalDate.now();
    return trainRepository.findActiveForDropdown(blankToNull(search), today)
      .stream().map(e -> toResponse(e, null)).toList();
  }

  // ── Return Train Info ─────────────────────────────────────────────────────
  @Override
  public ReturnTrainResponse getReturnTrainInfo(String trainNumber) {

    String trimmed = trainNumber.trim();

    if (!trimmed.matches("^[0-9]{5}$"))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_TRAIN_NUMBER",
        "Train number must be exactly 5 digits.");

    int number = Integer.parseInt(trimmed);

    // Indian Railways convention: trains run in pairs
    //   odd  number -> return is number + 1  (e.g. 12951 -> 12952)
    //   even number -> return is number - 1  (e.g. 12952 -> 12951)
    int returnNumber = (number % 2 != 0) ? number + 1 : number - 1;

    // Guard: must stay in valid 5-digit range (10000-99999)
    if (returnNumber < 10000 || returnNumber > 99999) {
      return ReturnTrainResponse.builder()
        .returnTrainNumber(null)
        .exists(false)
        .existingTrain(null)
        .message("Return train number could not be determined for " + trimmed + ".")
        .build();
    }

    String returnTrainNumber = String.valueOf(returnNumber);

    return trainRepository.findByTrainNumber(returnTrainNumber)
      .map(existing -> ReturnTrainResponse.builder()
        .returnTrainNumber(returnTrainNumber)
        .exists(true)
        .existingTrain(toResponse(existing, null))
        .message("Return train " + returnTrainNumber + " (" + existing.getTrainName() +
          ") is already registered.")
        .build())
      .orElseGet(() -> ReturnTrainResponse.builder()
        .returnTrainNumber(returnTrainNumber)
        .exists(false)
        .existingTrain(null)
        .message("Return train " + returnTrainNumber + " has not been added yet.")
        .build());
  }

  // ── Excel Upload ──────────────────────────────────────────────────────────
  // Expected columns (row 1 = header, data from row 2 onwards):
  //   A: Train Number | B: Train Name | C: Train Type Code | D: Zone Code | E: Pantry Car (YES/NO)
  @Override
  @Transactional
  public BulkUploadResponse uploadFromExcel(MultipartFile file) {

    validateFile(file);

    List<BulkUploadResponse.RowError> errors = new ArrayList<>();
    int successCount   = 0;
    int duplicateCount = 0;
    LocalDate today    = LocalDate.now();
    Long adminId       = SecurityUtils.getCurrentAdminId();

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

      Sheet sheet = workbook.getSheetAt(0);
      if (sheet == null)
        throw new BaseException(HttpStatus.BAD_REQUEST, "EMPTY_SHEET",
          "The uploaded file has no sheets.");

      int lastRow = sheet.getLastRowNum();

      // Row 0 = header -- skip it. Data starts at row 1 (0-based) = row 2 in Excel (1-based display)
      for (int i = 1; i <= lastRow; i++) {
        Row row = sheet.getRow(i);
        if (row == null || isRowBlank(row)) continue;

        int displayRow = i + 1;

        String trainNumber   = getCellString(row, 0).trim();
        String trainName     = getCellString(row, 1).trim();
        String trainTypeCode = getCellString(row, 2).trim().toUpperCase();
        String zoneCode      = getCellString(row, 3).trim().toUpperCase();
        String pantryRaw     = getCellString(row, 4).trim().toUpperCase();

        // ── Row-level validation ──────────────────────────
        String rowError = validateRow(trainNumber, trainName, trainTypeCode, zoneCode);
        if (rowError != null) {
          errors.add(BulkUploadResponse.RowError.builder()
            .rowNumber(displayRow).trainNumber(trainNumber)
            .trainName(trainName).reason(rowError).build());
          continue;
        }

        // ── Duplicate check ───────────────────────────────
        if (trainRepository.existsByTrainNumber(trainNumber)) {
          duplicateCount++;
          errors.add(BulkUploadResponse.RowError.builder()
            .rowNumber(displayRow).trainNumber(trainNumber)
            .trainName(trainName)
            .reason("Train number '" + trainNumber + "' already exists. Skipped.").build());
          continue;
        }

        // ── Reference data validation ─────────────────────
        Optional<TrainTypeEntity> typeOpt = trainTypeRepository.findByTypeCode(trainTypeCode);
        if (typeOpt.isEmpty()) {
          errors.add(BulkUploadResponse.RowError.builder()
            .rowNumber(displayRow).trainNumber(trainNumber).trainName(trainName)
            .reason("Train type '" + trainTypeCode + "' not found.").build());
          continue;
        }
        // Period-based active check for train type
        if (!trainTypePeriodRepository.isActiveOnDate(typeOpt.get().getTypeId(), today)) {
          errors.add(BulkUploadResponse.RowError.builder()
            .rowNumber(displayRow).trainNumber(trainNumber).trainName(trainName)
            .reason("Train type '" + trainTypeCode + "' is inactive.").build());
          continue;
        }

        Optional<ZoneEntity> zoneOpt = zoneRepository.findByCode(zoneCode);
        if (zoneOpt.isEmpty()) {
          errors.add(BulkUploadResponse.RowError.builder()
            .rowNumber(displayRow).trainNumber(trainNumber).trainName(trainName)
            .reason("Zone '" + zoneCode + "' not found.").build());
          continue;
        }
        if (!zoneOpt.get().getIsActive()) {
          errors.add(BulkUploadResponse.RowError.builder()
            .rowNumber(displayRow).trainNumber(trainNumber).trainName(trainName)
            .reason("Zone '" + zoneCode + "' is inactive.").build());
          continue;
        }

        // ── Save ──────────────────────────────────────────
        boolean pantrycar = "YES".equals(pantryRaw) || "TRUE".equals(pantryRaw) || "1".equals(pantryRaw);

        TrainEntity entity = TrainEntity.builder()
          .trainNumber(trainNumber)
          .trainName(trainName)
          .trainType(typeOpt.get())
          .zone(zoneOpt.get())
          .pantrycar(pantrycar)
          .createdBy(adminId)
          .build();

        TrainEntity saved = trainRepository.save(entity);

        // Create initial active period for the new train
        TrainPeriodEntity initialPeriod = TrainPeriodEntity.builder()
          .train(saved)
          .effectiveFrom(today)
          .effectiveTill(null)
          .reason("Train created via bulk upload")
          .createdBy(adminId)
          .build();
        trainPeriodRepository.save(initialPeriod);

        successCount++;
      }

    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.error("Excel upload failed", e);
      throw new BaseException(HttpStatus.BAD_REQUEST, "PARSE_ERROR",
        "Could not parse the uploaded file. Ensure it matches the template format.");
    }

    return BulkUploadResponse.builder()
      .successCount(successCount)
      .failureCount(errors.size() - duplicateCount)
      .duplicateCount(duplicateCount)
      .errors(errors.isEmpty() ? null : errors)
      .build();
  }

  @Override
  public TrainResponse getTrainDetails(String trainNumber) {
    TrainEntity train = trainRepository
      .findByTrainNumberWithDetails(trainNumber.trim())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
    return toResponse(train, null);
  }

  // ── Excel Template ────────────────────────────────────────────────────────
  @Override
  public byte[] getExcelTemplate() {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      XSSFSheet sheet = workbook.createSheet("Trains");

      // ── Header style ──────────────────────────────────
      XSSFCellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 30, (byte) 64, (byte) 175}, null));
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      XSSFFont headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
      headerFont.setFontHeightInPoints((short) 11);
      headerStyle.setFont(headerFont);

      // ── Example row style ─────────────────────────────
      XSSFCellStyle exampleStyle = workbook.createCellStyle();
      exampleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 239, (byte) 246, (byte) 255}, null));
      exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      // ── Headers ───────────────────────────────────────
      String[] headers = {
        "Train Number *",
        "Train Name *",
        "Train Type Code *",
        "Zone Code *",
        "Pantry Car (YES/NO)"
      };
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
        sheet.setColumnWidth(i, 6000);
      }

      // ── Example row ───────────────────────────────────
      Row example = sheet.createRow(1);
      String[] exampleData = {"12951", "Mumbai Central Rajdhani Express", "RAJDHANI", "WR", "YES"};
      for (int i = 0; i < exampleData.length; i++) {
        Cell cell = example.createCell(i);
        cell.setCellValue(exampleData[i]);
        cell.setCellStyle(exampleStyle);
      }

      // ── Instructions sheet ────────────────────────────
      XSSFSheet instructions = workbook.createSheet("Instructions");
      String[] lines = {
        "INSTRUCTIONS FOR BULK TRAIN UPLOAD",
        "",
        "1. Fill data in the 'Trains' sheet starting from row 2.",
        "2. Row 1 is the header -- do not modify it.",
        "3. Train Number: exactly 5 digits (e.g. 12951). Must be unique.",
        "4. Train Name: 3-150 characters. Must be unique.",
        "5. Train Type Code: must match an existing active train type (e.g. RAJDHANI, EXPRESS).",
        "6. Zone Code: must match an existing active zone (e.g. NR, WR, SR, CR).",
        "7. Pantry Car: enter YES or NO (case-insensitive). Leave blank for NO.",
        "8. Duplicate train numbers or names will be skipped with a reason in the upload report.",
        "9. Save as .xlsx before uploading."
      };
      for (int i = 0; i < lines.length; i++) {
        Row r = instructions.createRow(i);
        r.createCell(0).setCellValue(lines[i]);
      }
      instructions.setColumnWidth(0, 20000);

      workbook.write(out);
      return out.toByteArray();

    } catch (IOException e) {
      log.error("Failed to generate Excel template", e);
      throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "TEMPLATE_ERROR",
        "Failed to generate Excel template.");
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private TrainEntity findByNumber(String trainNumber) {
    return trainRepository.findByTrainNumber(trainNumber.trim())
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "TRAIN_NOT_FOUND",
        "Train not found: " + trainNumber));
  }

  private ZoneEntity findActiveZone(String zoneCode) {
    ZoneEntity zone = zoneRepository.findByCode(zoneCode)
      .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND",
        "Zone not found: " + zoneCode));
    if (!zone.getIsActive())
      throw new BaseException(HttpStatus.BAD_REQUEST, "ZONE_INACTIVE",
        "Zone '" + zoneCode + "' is inactive.");
    return zone;
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty())
      throw new BaseException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Uploaded file is empty.");
    String name = file.getOriginalFilename();
    if (name == null || (!name.toLowerCase().endsWith(".xlsx") && !name.toLowerCase().endsWith(".xls")))
      throw new BaseException(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE",
        "Only .xlsx and .xls files are accepted.");
  }

  private String validateRow(String trainNumber, String trainName, String typeCode, String zoneCode) {
    if (trainNumber.isBlank())   return "Train number is required.";
    if (!trainNumber.matches("^[0-9]{5}$")) return "Train number must be exactly 5 digits. Got: '" + trainNumber + "'.";
    if (trainName.isBlank())     return "Train name is required.";
    if (trainName.length() < 3 || trainName.length() > 150) return "Train name must be 3-150 characters.";
    if (typeCode.isBlank())      return "Train type code is required.";
    if (zoneCode.isBlank())      return "Zone code is required.";
    return null;
  }

  private boolean isRowBlank(Row row) {
    for (int i = 0; i < 5; i++) {
      String val = getCellString(row, i);
      if (!val.isBlank()) return false;
    }
    return true;
  }

  private String getCellString(Row row, int colIndex) {
    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case STRING  -> cell.getStringCellValue();
      case NUMERIC -> {
        double val = cell.getNumericCellValue();
        yield String.valueOf((long) val);
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> {
        try { yield cell.getStringCellValue(); }
        catch (Exception e) { yield String.valueOf((long) cell.getNumericCellValue()); }
      }
      default -> "";
    };
  }

  private String blankToNull(String s) {
    return (s != null && !s.isBlank()) ? s : null;
  }

  private TrainResponse toResponse(TrainEntity e, String message) {
    LocalDate today = LocalDate.now();
    boolean isActive = trainPeriodRepository.isActiveOnDate(e.getTrainId(), today);

    // Get current active period for effectiveFrom/effectiveTill display
    Optional<TrainPeriodEntity> activePeriodOpt = trainPeriodRepository.findActivePeriod(e.getTrainId(), today);

    return TrainResponse.builder()
      .trainId(e.getTrainId())
      .trainNumber(e.getTrainNumber())
      .trainName(e.getTrainName())
      .trainTypeCode(e.getTrainType().getTypeCode())
      .trainTypeName(e.getTrainType().getTypeName())
      .isSuperfast(e.getTrainType().getIsSuperfast())
      .zoneCode(e.getZone().getCode())
      .zoneName(e.getZone().getName())
      .pantrycar(e.getPantrycar())
      .isActive(isActive)
      .effectiveFrom(activePeriodOpt.map(TrainPeriodEntity::getEffectiveFrom).orElse(null))
      .effectiveTill(activePeriodOpt.map(TrainPeriodEntity::getEffectiveTill).orElse(null))
      .createdBy(e.getCreatedBy())
      .updatedBy(e.getUpdatedBy())
      .createdAt(e.getCreatedAt())
      .updatedAt(e.getUpdatedAt())
      .message(message)
      .build();
  }

  private PeriodResponse toPeriodResponse(TrainPeriodEntity p, LocalDate today) {
    if (p == null) return null;

    String status;
    if (p.getEffectiveFrom().isAfter(today)) {
      status = "UPCOMING";
    } else if (p.getEffectiveTill() != null && p.getEffectiveTill().isBefore(today)) {
      status = "PAST";
    } else if (p.coversDate(today)) {
      status = "ACTIVE";
    } else {
      status = "INACTIVE_GAP";
    }

    return PeriodResponse.builder()
      .periodId(p.getPeriodId())
      .effectiveFrom(p.getEffectiveFrom())
      .effectiveTill(p.getEffectiveTill())
      .status(status)
      .reason(p.getReason())
      .createdBy(p.getCreatedBy())
      .createdAt(p.getCreatedAt())
      .build();
  }
}
