package com.example.attendancesystem.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Attendance Service Application
 * Microservice for Attendance Management
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EntityScan(basePackages = {
    "com.example.attendancesystem.attendance.model"
})
@EnableJpaRepositories(basePackages = {
    "com.example.attendancesystem.attendance.repository"
})
public class AttendanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendanceServiceApplication.class, args);
    }
}
