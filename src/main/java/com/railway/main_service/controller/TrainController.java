package com.railway.main_service.controller;


import com.railway.common.logging.Loggable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/train/search")
@Loggable
public class TrainController {

  @GetMapping("/test")
  public String test() {
    return "Hello World!";
  }
}
