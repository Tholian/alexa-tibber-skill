plugins {
    java
    id("org.springframework.boot") version "4.0.5" // Aktualisiert auf Spring Boot 4.x
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.tholian"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25) // Java 25
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot 4 Starter
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Alexa SDK (ask-sdk-servlet-support wird ausgeschlossen: javax.servlet kollidiert mit Jakarta EE 11)
    implementation("com.amazon.alexa:ask-sdk:2.86.0") {
        exclude(group = "com.amazon.alexa", module = "ask-sdk-servlet-support")
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
