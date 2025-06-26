@echo off
REM Script to create configuration files for all microservices

echo Creating configuration files for all microservices...

REM Define services and their ports
set SERVICES=attendance-service:8082:9092 menu-service:8083:9093 order-service:8084:9094 table-service:8085:9095

echo.
echo Creating pom.xml files...

REM Create pom.xml for each service
for %%s in (%SERVICES%) do (
    for /f "tokens=1,2,3 delims=:" %%a in ("%%s") do (
        echo Creating pom.xml for %%a...
        
        (
        echo ^<?xml version="1.0" encoding="UTF-8"?^>
        echo ^<project xmlns="http://maven.apache.org/POM/4.0.0"
        echo          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        echo          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
        echo          http://maven.apache.org/xsd/maven-4.0.0.xsd"^>
        echo     ^<modelVersion^>4.0.0^</modelVersion^>
        echo.
        echo     ^<groupId^>com.example.attendancesystem^</groupId^>
        echo     ^<artifactId^>%%a^</artifactId^>
        echo     ^<version^>1.0.0^</version^>
        echo     ^<packaging^>jar^</packaging^>
        echo.
        echo     ^<name^>Attendance Management System - %%a^</name^>
        echo     ^<description^>%%a Microservice^</description^>
        echo.
        echo     ^<properties^>
        echo         ^<maven.compiler.source^>21^</maven.compiler.source^>
        echo         ^<maven.compiler.target^>21^</maven.compiler.target^>
        echo         ^<project.build.sourceEncoding^>UTF-8^</project.build.sourceEncoding^>
        echo         ^<spring-boot.version^>3.2.1^</spring-boot.version^>
        echo         ^<grpc-spring-boot-starter.version^>2.15.0.RELEASE^</grpc-spring-boot-starter.version^>
        echo         ^<postgresql.version^>42.7.1^</postgresql.version^>
        echo         ^<micrometer.version^>1.12.1^</micrometer.version^>
        echo     ^</properties^>
        echo.
        echo     ^<dependencyManagement^>
        echo         ^<dependencies^>
        echo             ^<dependency^>
        echo                 ^<groupId^>org.springframework.boot^</groupId^>
        echo                 ^<artifactId^>spring-boot-dependencies^</artifactId^>
        echo                 ^<version^>${spring-boot.version}^</version^>
        echo                 ^<type^>pom^</type^>
        echo                 ^<scope^>import^</scope^>
        echo             ^</dependency^>
        echo         ^</dependencies^>
        echo     ^</dependencyManagement^>
        echo.
        echo     ^<dependencies^>
        echo         ^<!-- Shared Library --^>
        echo         ^<dependency^>
        echo             ^<groupId^>com.example.attendancesystem^</groupId^>
        echo             ^<artifactId^>shared-lib^</artifactId^>
        echo             ^<version^>1.0.0^</version^>
        echo         ^</dependency^>
        echo.
        echo         ^<!-- Spring Boot Starters --^>
        echo         ^<dependency^>
        echo             ^<groupId^>org.springframework.boot^</groupId^>
        echo             ^<artifactId^>spring-boot-starter^</artifactId^>
        echo         ^</dependency^>
        echo         ^<dependency^>
        echo             ^<groupId^>org.springframework.boot^</groupId^>
        echo             ^<artifactId^>spring-boot-starter-web^</artifactId^>
        echo         ^</dependency^>
        echo         ^<dependency^>
        echo             ^<groupId^>org.springframework.boot^</groupId^>
        echo             ^<artifactId^>spring-boot-starter-data-jpa^</artifactId^>
        echo         ^</dependency^>
        echo         ^<dependency^>
        echo             ^<groupId^>org.springframework.boot^</groupId^>
        echo             ^<artifactId^>spring-boot-starter-validation^</artifactId^>
        echo         ^</dependency^>
        echo         ^<dependency^>
        echo             ^<groupId^>org.springframework.boot^</groupId^>
        echo             ^<artifactId^>spring-boot-starter-actuator^</artifactId^>
        echo         ^</dependency^>
        echo.
        echo         ^<!-- gRPC --^>
        echo         ^<dependency^>
        echo             ^<groupId^>net.devh^</groupId^>
        echo             ^<artifactId^>grpc-spring-boot-starter^</artifactId^>
        echo             ^<version^>${grpc-spring-boot-starter.version}^</version^>
        echo         ^</dependency^>
        echo.
        echo         ^<!-- Database --^>
        echo         ^<dependency^>
        echo             ^<groupId^>org.postgresql^</groupId^>
        echo             ^<artifactId^>postgresql^</artifactId^>
        echo             ^<version^>${postgresql.version}^</version^>
        echo         ^</dependency^>
        echo.
        echo         ^<!-- Service Discovery --^>
        echo         ^<dependency^>
        echo             ^<groupId^>javax.jmdns^</groupId^>
        echo             ^<artifactId^>jmdns^</artifactId^>
        echo             ^<version^>3.5.8^</version^>
        echo         ^</dependency^>
        echo.
        echo         ^<!-- Observability --^>
        echo         ^<dependency^>
        echo             ^<groupId^>io.micrometer^</groupId^>
        echo             ^<artifactId^>micrometer-registry-prometheus^</artifactId^>
        echo             ^<version^>${micrometer.version}^</version^>
        echo         ^</dependency^>
        echo         ^<dependency^>
        echo             ^<groupId^>io.micrometer^</groupId^>
        echo             ^<artifactId^>micrometer-tracing-bridge-brave^</artifactId^>
        echo         ^</dependency^>
        echo         ^<dependency^>
        echo             ^<groupId^>io.zipkin.reporter2^</groupId^>
        echo             ^<artifactId^>zipkin-reporter-brave^</artifactId^>
        echo         ^</dependency^>
        echo.
        echo         ^<!-- Testing --^>
        echo         ^<dependency^>
        echo             ^<groupId^>org.springframework.boot^</groupId^>
        echo             ^<artifactId^>spring-boot-starter-test^</artifactId^>
        echo             ^<scope^>test^</scope^>
        echo         ^</dependency^>
        echo     ^</dependencies^>
        echo.
        echo     ^<build^>
        echo         ^<plugins^>
        echo             ^<plugin^>
        echo                 ^<groupId^>org.springframework.boot^</groupId^>
        echo                 ^<artifactId^>spring-boot-maven-plugin^</artifactId^>
        echo                 ^<version^>${spring-boot.version}^</version^>
        echo             ^</plugin^>
        echo             ^<plugin^>
        echo                 ^<groupId^>org.apache.maven.plugins^</groupId^>
        echo                 ^<artifactId^>maven-compiler-plugin^</artifactId^>
        echo                 ^<version^>3.12.1^</version^>
        echo                 ^<configuration^>
        echo                     ^<source^>21^</source^>
        echo                     ^<target^>21^</target^>
        echo                 ^</configuration^>
        echo             ^</plugin^>
        echo         ^</plugins^>
        echo     ^</build^>
        echo ^</project^>
        ) > "microservices\%%a\pom.xml"
    )
)

echo pom.xml files created.

echo.
echo Creating Application classes...

REM Create Application classes for each service
for %%s in (%SERVICES%) do (
    for /f "tokens=1,2,3 delims=:" %%a in ("%%s") do (
        set SERVICE_NAME=%%a
        set SERVICE_CLASS=!SERVICE_NAME:-service=!
        
        REM Convert to proper case
        for /f "delims=-" %%x in ("!SERVICE_CLASS!") do (
            set FIRST_PART=%%x
            set FIRST_CHAR=!FIRST_PART:~0,1!
            set REST_CHARS=!FIRST_PART:~1!
            call :UpperCase FIRST_CHAR
            set PROPER_NAME=!FIRST_CHAR!!REST_CHARS!
        )
        
        echo Creating Application class for %%a...
        
        (
        echo package com.example.attendancesystem.!SERVICE_CLASS!;
        echo.
        echo import org.springframework.boot.SpringApplication;
        echo import org.springframework.boot.autoconfigure.SpringBootApplication;
        echo import org.springframework.boot.autoconfigure.domain.EntityScan;
        echo import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
        echo import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
        echo import org.springframework.scheduling.annotation.EnableScheduling;
        echo.
        echo /**
        echo  * !PROPER_NAME! Service Application
        echo  * Microservice for !PROPER_NAME! Management
        echo  */
        echo @SpringBootApplication
        echo @EnableJpaAuditing
        echo @EnableScheduling
        echo @EntityScan^(basePackages = {
        echo     "com.example.attendancesystem.!SERVICE_CLASS!.model",
        echo     "com.example.attendancesystem.shared.model"
        echo }^)
        echo @EnableJpaRepositories^(basePackages = {
        echo     "com.example.attendancesystem.!SERVICE_CLASS!.repository"
        echo }^)
        echo public class !PROPER_NAME!ServiceApplication {
        echo.
        echo     public static void main^(String[] args^) {
        echo         SpringApplication.run^(!PROPER_NAME!ServiceApplication.class, args^);
        echo     }
        echo }
        ) > "microservices\%%a\src\main\java\com\example\attendancesystem\!SERVICE_CLASS!\!PROPER_NAME!ServiceApplication.java"
    )
)

echo Application classes created.

echo.
echo All configuration files created successfully!
echo.
echo Services configured:
for %%s in (%SERVICES%) do (
    for /f "tokens=1,2,3 delims=:" %%a in ("%%s") do (
        echo - %%a ^(HTTP: %%b, gRPC: %%c^)
    )
)

goto :eof

:UpperCase
for %%i in (a b c d e f g h i j k l m n o p q r s t u v w x y z) do call set %1=%%%1:%%i=%%i%%
goto :eof
