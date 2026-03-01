# Dependency Manifest — Personal Finance Tracker
**Author**: Tech Lead
**Date**: 2026-02-28
**Module**: application/build.gradle.kts (the only module that builds the runnable app)

---

## Complete Replacement `application/build.gradle.kts`

```kotlin
plugins {
    id("java")
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.shan.cyber.tech"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    // ---- Core Web & JPA ----
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // ---- Database ----
    runtimeOnly("org.postgresql:postgresql")          // version managed by Spring Boot BOM (42.7.x)
    implementation("org.liquibase:liquibase-core")   // version managed by Spring Boot BOM

    // ---- Security / Password Hashing ----
    implementation("org.springframework.security:spring-security-crypto")
    // Provides BCryptPasswordEncoder. We pull ONLY spring-security-crypto, not the full
    // spring-boot-starter-security, to avoid Spring Security auto-configuration that would
    // override our custom filter setup.

    // ---- Validation ----
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Provides @Valid, @NotBlank, @Pattern, @Size etc via Hibernate Validator (Jakarta Validation 3.0)

    // ---- Jackson / JSON ----
    // jackson-databind and jackson-datatype-jsr310 are already pulled in by spring-boot-starter-web.
    // No additional Jackson dependencies needed.

    // ---- Code Generation ----
    compileOnly("org.projectlombok:lombok")           // version managed by Spring Boot BOM (1.18.x)
    annotationProcessor("org.projectlombok:lombok")

    // ---- API Documentation ----
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    // Serves Swagger UI at /swagger-ui.html and OpenAPI JSON at /v3/api-docs
    // Compatible with Spring Boot 3.2.x (requires springdoc 2.x for Spring Boot 3.x)

    // ---- Testing ----
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Includes: JUnit 5, Mockito, MockMvc, AssertJ, Hamcrest — all version-managed

    testImplementation("org.testcontainers:postgresql:1.19.6")
    // PostgreSQL Testcontainer for @DataJpaTest repository tests.
    // Version 1.19.6 is compatible with Spring Boot 3.2.x and Postgres 15.

    testImplementation("org.testcontainers:junit-jupiter:1.19.6")
    // JUnit 5 extension for Testcontainers lifecycle management

    testImplementation("io.rest-assured:rest-assured:5.4.0")
    // For integration tests in the acceptance module. Version 5.x compatible with Java 17.
}

tasks.test {
    useJUnitPlatform()
    // Run unit tests + repository slice tests
    exclude("**/*IntegrationTest*")
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform()
    include("**/*IntegrationTest*")
    group = "verification"
    description = "Runs full-stack integration tests (requires Docker)"
}
```

---

## Dependency Decision Log

### KEEP

| Dependency | Why Kept | Notes |
|---|---|---|
| `spring-boot-starter-web` | Core MVC framework, Jackson, Tomcat | Essential |
| `spring-boot-starter-data-jpa` | JPA/Hibernate + Spring Data repositories | Essential |
| `org.postgresql:postgresql` | JDBC driver for Postgres 15 | Changed to `runtimeOnly`, version de-pinned to let BOM manage |
| `org.liquibase:liquibase-core` | DB migrations | Already correctly wired |
| `org.projectlombok:lombok` | Reduces boilerplate (@Getter, @Setter, @Builder) | Keep — useful even with records for entities |
| `spring-boot-starter-validation` | Bean Validation for request DTOs | NEW addition |
| `spring-security-crypto` | BCryptPasswordEncoder for password hashing | NEW addition — not full Spring Security |

### ADD

| Dependency | GAV | Why Added |
|---|---|---|
| spring-boot-starter-validation | `org.springframework.boot:spring-boot-starter-validation` (BOM-managed) | Required for `@Valid` on controller params to trigger Bean Validation |
| spring-security-crypto | `org.springframework.security:spring-security-crypto` (BOM-managed) | BCrypt password hashing without pulling in full Spring Security |
| springdoc-openapi | `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0` | Swagger UI for API exploration; replaces the misused swagger-codegen |
| testcontainers-postgresql | `org.testcontainers:postgresql:1.19.6` | Realistic DB in tests matching production Postgres 15 |
| testcontainers-junit-jupiter | `org.testcontainers:junit-jupiter:1.19.6` | JUnit 5 integration for Testcontainers |
| rest-assured | `io.rest-assured:rest-assured:5.4.0` | HTTP-level integration testing |
| spring-boot-starter-test | `org.springframework.boot:spring-boot-starter-test` (BOM-managed) | JUnit 5 + Mockito + MockMvc |

### REMOVE

| Dependency | Why Removed |
|---|---|
| `io.swagger:swagger-codegen-maven-plugin:2.2.3` | This is a Maven plugin, not a runtime library. Wrong artifact entirely. Replaced by springdoc-openapi. |
| `org.springframework.boot:spring-boot-starter-log4j2` | Conflicts with spring-boot-starter-logging (Logback). Only one logging backend allowed. Keep Logback (default). |
| `org.springframework.boot:spring-boot-starter-logging` | Redundant explicit declaration — it is already pulled in transitively by spring-boot-starter-web. Remove the explicit line to keep the build clean, but the effect is the same. |
| `org.postgresql:postgresql:42.1.4` (version pin) | Version 42.1.4 is from 2017 with known CVEs. Remove version pin; let Spring Boot BOM provide 42.7.x. |
| `testImplementation(platform("org.junit:junit-bom:5.9.1"))` | Redundant — spring-boot-starter-test already manages JUnit 5 at the correct version. Having two BOMs for the same library causes version conflicts. |
| `testImplementation("org.junit.jupiter:junit-jupiter")` | Redundant — included transitively via spring-boot-starter-test. |

---

## Version Compatibility Matrix

| Dependency | Version | Spring Boot 3.2.2 Compatible |
|---|---|---|
| Spring Boot | 3.2.2 | — |
| Java | 17 | YES |
| PostgreSQL driver | 42.7.2 (BOM-managed) | YES |
| Liquibase Core | 4.25.1 (BOM-managed) | YES |
| Lombok | 1.18.30 (BOM-managed) | YES |
| Hibernate Validator | 8.0.x (BOM-managed) | YES |
| SpringDoc OpenAPI | 2.3.0 | YES (requires 2.x for Spring Boot 3.x) |
| Testcontainers | 1.19.6 | YES |
| Rest Assured | 5.4.0 | YES (Java 17 compatible) |

---

## Notes on `database/` module

The `database/build.gradle.kts` currently has:
```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-web")
developmentOnly("org.springframework.boot:spring-boot-docker-compose")
```

This module has no `@SpringBootApplication`, no `application.yaml`, no datasource config. The Spring Boot JPA dependency here serves no purpose currently. Leave as-is for now — the module is a placeholder. Do NOT attempt to run it as a Spring Boot app.

The `developmentOnly("org.springframework.boot:spring-boot-docker-compose")` should move to the `application/build.gradle.kts` if auto-Docker-Compose startup is desired (it was missing from the main module's build).
