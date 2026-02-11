package com.railway.main_service.utility.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

  public static final String TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final String TYPE_XLS = "application/vnd.ms-excel";

  /**
   * Check if file is Excel format
   */
  public static boolean hasExcelFormat(MultipartFile file) {
    String contentType = file.getContentType();
    return TYPE_XLSX.equals(contentType) || TYPE_XLS.equals(contentType);
  }

  /**
   * Get cell value as string
   */
  public static String getCellValue(Cell cell) {
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue().trim();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getDateCellValue().toString();
        }
        return String.valueOf((long) cell.getNumericCellValue());
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      default:
        return "";
    }
  }

  /**
   * Get cell value as Integer
   */
  public static Integer getCellValueAsInteger(Cell cell) {
    String value = getCellValue(cell);
    if (value.isEmpty()) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Get cell value as Boolean
   */
  public static Boolean getCellValueAsBoolean(Cell cell) {
    String value = getCellValue(cell).toLowerCase();
    if (value.isEmpty()) {
      return false;
    }
    return "true".equals(value) || "yes".equals(value) || "1".equals(value);
  }

  /**
   * Create workbook from file
   */
  public static Workbook createWorkbook(MultipartFile file) throws IOException {
    InputStream inputStream = file.getInputStream();
    return WorkbookFactory.create(inputStream);
  }

  /**
   * Get header row as list of strings
   */
  public static List<String> getHeaders(Sheet sheet) {
    List<String> headers = new ArrayList<>();
    Row headerRow = sheet.getRow(0);

    if (headerRow != null) {
      Iterator<Cell> cellIterator = headerRow.cellIterator();
      while (cellIterator.hasNext()) {
        Cell cell = cellIterator.next();
        headers.add(getCellValue(cell));
      }
    }

    return headers;
  }

  /**
   * Get cell value as Double
   */
  public static Double getCellValueAsDouble(Cell cell) {
    if (cell == null) {
      return null;
    }

    try {
      switch (cell.getCellType()) {
        case NUMERIC:
          return cell.getNumericCellValue();
        case STRING:
          String value = cell.getStringCellValue().trim();
          return value.isEmpty() ? null : Double.parseDouble(value);
        default:
          return null;
      }
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
