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
    annotationProcessor("org.projectlombok:lombok")

    // Alexa SDK (ask-sdk-servlet-support wird ausgeschlossen: javax.servlet kollidiert mit Jakarta EE 11)
    compileOnly("org.jetbrains:annotations:24.1.0")

    // KI-generiert: JSON-Logging für CloudWatch via Logstash-Format
    compileOnly("org.projectlombok:lombok")

    // KI-generiert: IntelliJ @Language-Annotation für GraphQL-String-Highlighting (compile-only, kein Runtime-Einfluss)
    implementation("com.amazon.alexa:ask-sdk:2.86.0") {
        exclude(group = "com.amazon.alexa", module = "ask-sdk-servlet-support")
    }

    // KI-generiert: Lombok für @Slf4j-Annotation (eliminiert Logger-Boilerplate)
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("org.springframework.boot:spring-boot-starter-web")
    testAnnotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
