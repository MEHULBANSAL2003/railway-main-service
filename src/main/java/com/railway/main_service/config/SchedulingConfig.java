package com.railway.main_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled annotation processing.
 * Without this, JourneyGeneratorJob won't run automatically.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
