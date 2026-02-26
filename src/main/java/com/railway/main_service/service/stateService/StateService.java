package com.railway.main_service.service.stateService;

import com.railway.main_service.utility.excel.ExcelUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface StateService {

  ExcelUploadResult uploadStatesExcel(MultipartFile file);
}
