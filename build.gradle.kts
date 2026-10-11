// KI-generiert: Build der AWS-Lambda-Variante des Alexa Tibber Skills – bewusst ohne Framework.
// Artefakt: build/distributions/alexa-tibber-skill.zip (Klassen im Root, Abhängigkeiten unter lib/)
plugins {
    java
}

group = "org.tholian"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// KI-generiert: Mockito als Java-Agent laden (statt dynamischem Agent-Attach, der ab JDK 21 Warnungen erzeugt)
val mockitoAgent: Configuration = configurations.create("mockitoAgent")

dependencies {
    // Alexa Skills Kit – ask-sdk-core (Skill-Kern, CustomSkillBuilder) und ask-sdk-lambda-support (SkillStreamHandler für Lambda);
    // bewusst nicht das Aggregat "ask-sdk"
    implementation("com.amazon.alexa:ask-sdk-core:2.86.0")
    implementation("com.amazon.alexa:ask-sdk-lambda-support:2.86.0")
    // KI-generiert: Sicherheits-Pin – ask-sdk-lambda-support bringt transitiv commons-io 2.7 mit (CVE-2021-29425);
    // die Constraint hebt nur die transitive Version an, ohne commons-io direkt einzubinden
    constraints {
        implementation("commons-io:commons-io:2.22.0") {
            because("ask-sdk-lambda-support bringt transitiv commons-io 2.7 mit (CVE-2021-29425)")
        }
    }
    // KI-generiert: Jackson wird direkt importiert (TibberGraphQlClient, Modell) und daher explizit deklariert;
    // die Version entspricht der bereits aufgelösten (ask-sdk-model-runtime bringt ältere mit)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.3")
    // Lambda-Laufzeit-Interfaces (RequestStreamHandler, Context)
    implementation("com.amazonaws:aws-lambda-java-core:1.4.0")
    // SLF4J-API explizit auf 2.x – ask-sdk-core bringt transitiv 1.7.x mit, Logback 1.5 benötigt 2.x
    implementation("org.slf4j:slf4j-api:2.0.20")

    // JSON-Logging nach CloudWatch
    runtimeOnly("ch.qos.logback:logback-classic:1.6.5")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:9.0")

    // Nur zur Compile-Zeit (nicht im Lambda-Paket): @Language, @NullMarked/@Nullable, @Slf4j
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("org.jspecify:jspecify:1.0.1")
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.mockito:mockito-junit-jupiter:5.24.0")
    // KI-generiert: Logback-API im Test, um zu prüfen, dass der Tibber-Token nie geloggt wird
    testImplementation("ch.qos.logback:logback-classic:1.6.5")
    // KI-generiert: WebSocket-Server nur für Tests (FakeTibberWebSocketServer) – nicht im Lambda-Paket;
    // der Skill selbst nutzt java.net.http.WebSocket aus dem JDK
    testImplementation("org.java-websocket:Java-WebSocket:1.6.0")
    mockitoAgent("org.mockito:mockito-core:5.24.0") {
        isTransitive = false
    }
}

// KI-generiert: ask-sdk-model-runtime bringt log4j-api/log4j-slf4j-impl 2.8.2 transitiv mit. Wir loggen über
// SLF4J + Logback, daher sind sie toter Ballast (und Scanner-Rauschen) und werden aus allen Classpaths (inkl. Test) entfernt.
configurations.configureEach {
    exclude(group = "org.apache.logging.log4j")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}

// KI-generiert: Lambda-Deployment-Paket im von AWS empfohlenen Layout (kein Fat-JAR/Shadow-Plugin nötig)
val buildZip by tasks.registering(Zip::class) {
    archiveFileName = "alexa-tibber-skill.zip"
    // KI-generiert: Nur Klassen und Ressourcen (sourceSets.main.output) – nicht den kompletten compileJava-Output,
    // der interne Gradle-Dateien wie previous-compilation-data.bin enthält
    from(sourceSets.main.map { it.output })
    into("lib") {
        from(configurations.runtimeClasspath)
    }
}

tasks.build {
    dependsOn(buildZip)
}
