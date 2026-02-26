package com.railway.main_service.controller;

import com.railway.common.exceptions.ApiResponse;
import com.railway.common.logging.Loggable;
import com.railway.main_service.constants.ApiConstants;
import com.railway.main_service.dto.response.state.StateResponse;
import com.railway.main_service.service.stateService.StateService;
import com.railway.main_service.service.stateService.StateServiceImpl;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.STATES)
@RequiredArgsConstructor
@Loggable
public class StateController {

  private final StateService stateService;

  @PostMapping(ApiConstants.UPLOAD_STATES_DATA_BY_EXCEL)
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<ExcelUploadResult>> uploadStatesExcel(
    @RequestParam("file") MultipartFile file) {

    ExcelUploadResult result = stateService.uploadStatesExcel(file);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @GetMapping(ApiConstants.GET_STATES)
  public ResponseEntity<ApiResponse<List<StateResponse>>> getAllStates(@RequestParam(value = "searchTerm", required = false) String searchTerm) {
    List<StateResponse> response = stateService.getRequiredStates(searchTerm);
    return ResponseEntity.ok(ApiResponse.success(response));
  }



}
