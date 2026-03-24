package com.railway.main_service.service.stateService;


import com.railway.common.logging.Loggable;
import com.railway.main_service.dto.response.state.StateResponse;
import com.railway.main_service.entity.StateEntity;
import com.railway.main_service.repository.StateRepository;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Loggable
@Slf4j
@RequiredArgsConstructor
public class StateServiceImpl implements StateService{

  private final StateRepository stateRepository;
  private final StateExcelProcessor stateExcelProcessor;

  @Override
  public ExcelUploadResult<StateEntity> uploadStatesExcel(MultipartFile file) {
    log.info("Starting Excel upload for states. File: {}, Size: {} bytes",
      file.getOriginalFilename(), file.getSize());

    ExcelUploadResult<StateEntity> result = stateExcelProcessor.processExcelFile(file);

    log.info("Excel upload completed. Success: {}, Failed: {}, Total: {}",
      result.getSuccessCount(), result.getFailureCount(), result.getTotalRows());
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<StateResponse> getRequiredStates(String searchTerm) {
    List<StateEntity> states;

    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      states = stateRepository.findAllActive();
    } else {
      states = stateRepository.searchActiveStates(searchTerm.trim());
    }

    return states.stream()
      .map(this::mapToResponse)
      .toList();
  }

  private StateResponse mapToResponse(StateEntity state) {
    return StateResponse.builder()
      .id(state.getId())
      .code(state.getCode())
      .name(state.getName())
      .isActive(state.isCurrentlyActive())
      .effectiveFrom(state.getEffectiveFrom())
      .effectiveTill(state.getEffectiveTill())
      .reason(state.getReason())
      .build();
  }
}
