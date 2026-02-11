package com.railway.main_service.utility.excel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {
  String value();  // Column name in Excel
  int order() default 0;  // Column order
  boolean required() default false;
}
