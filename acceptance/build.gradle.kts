plugins {
    id("java")
}

group = "com.shan.cyber.tech"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Spring Boot Test (full context)
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.2"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Application under test — all source + compiled classes on test classpath
    testImplementation(project(":application"))

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // RestAssured
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:json-path:5.4.0")

    // Testcontainers
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.6"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")

    // PostgreSQL driver (needed at runtime for Testcontainers)
    testRuntimeOnly("org.postgresql:postgresql")
}

// Acceptance tests (Docker required)
tasks.register<Test>("acceptanceTest") {
    useJUnitPlatform {
        includeTags("acceptance")
    }
    group = "verification"
    description = "Full HTTP round-trip acceptance tests using RestAssured + Testcontainers"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    // Show test output in CI
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Alias used by the CI pipeline command: ./gradlew integrationTest
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("acceptance")
    }
    group = "verification"
    description = "Alias for acceptanceTest — full HTTP round-trip tests"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Exclude acceptance tests from the default 'test' task so they don't run without Docker
tasks.test {
    useJUnitPlatform {
        excludeTags("acceptance")
    }
}
