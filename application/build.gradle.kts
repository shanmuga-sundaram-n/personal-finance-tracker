plugins {
    id("java")
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.liquibase.gradle") version "3.1.0"
}

group = "com.shan.cyber.tech"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    // Testcontainers for persistence adapter tests
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.6"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

// Domain unit tests + Architecture tests (fast, no Docker)
tasks.test {
    useJUnitPlatform {
        excludeTags("integration", "adapter")
    }
}

// Persistence adapter tests (Docker required)
tasks.register<Test>("adapterTest") {
    useJUnitPlatform {
        includeTags("adapter")
    }
    group = "verification"
    description = "Persistence adapter tests using Testcontainers"
    shouldRunAfter("test")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}
