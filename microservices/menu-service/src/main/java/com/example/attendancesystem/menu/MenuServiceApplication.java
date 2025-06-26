package com.example.attendancesystem.menu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Menu Service Application
 * Microservice for Menu Management
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EntityScan(basePackages = {
    "com.example.attendancesystem.menu.model",
    "com.example.attendancesystem.shared.model"
})
@EnableJpaRepositories(basePackages = {
    "com.example.attendancesystem.menu.repository"
})
public class MenuServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MenuServiceApplication.class, args);
    }
}
