package com.railway.main_service.service.stateService;

import com.railway.main_service.dto.response.state.StateResponse;
import com.railway.main_service.utility.excel.ExcelUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface StateService {

  ExcelUploadResult uploadStatesExcel(MultipartFile file);

  List<StateResponse> getRequiredStates(String searchTerm);
}
