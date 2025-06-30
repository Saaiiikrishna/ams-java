package com.example.attendancesystem.table;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Table Service Application
 * Microservice for Table Management
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EntityScan(basePackages = {
    "com.example.attendancesystem.table.model",
    "com.example.attendancesystem.shared.model"
})
@EnableJpaRepositories(basePackages = {
    "com.example.attendancesystem.table.repository",
    "com.example.attendancesystem.shared.repository"
})
public class TableServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TableServiceApplication.class, args);
    }
}
