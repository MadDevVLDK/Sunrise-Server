
plugins {
    id("java")
    id("org.springframework.boot") version("3.5.0")
    id("io.spring.dependency-management") version("1.1.7")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "com.sunrise"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("com.github.ben-manes.caffeine:caffeine")

    runtimeOnly("org.postgresql:postgresql")
}

// tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
//     // Эта строка гарантирует, что профиль "local" будет активирован в процессе приложения, запущенным задачей bootRun
//     systemProperty("spring.profiles.active", "local")
//     doFirst {
//         // Создаем Map, куда будем загружать переменные
//         val envMap = mutableMapOf<String, String>()
//         File(".env").readLines().forEach { line ->
//             if (line.isNotEmpty() && !line.startsWith("#")) {
//                 val parts = line.split("=", limit = 2)
//                 if (parts.size == 2) {
//                     val key = parts[0].trim()
//                     envMap[key] = parts[1].trim().trim('"', '\'')
//                 }
//             }
//         }
//         // Добавляем загруженные переменные в окружение процесса bootRun
//         environment(envMap)
//     }
// }

// Отключение ошибок
tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}