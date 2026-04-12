package com.railway.main_service.controller;


import com.railway.main_service.constant.ApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_BASE)
@RequiredArgsConstructor
@Slf4j
public class TestController {

  @GetMapping
  public String hello(){
    return "hello";
  }

  @GetMapping("/train")
  public String train(){
    return  "train";
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public String admin(){
    return "admin";
  }
}
