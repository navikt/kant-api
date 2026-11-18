import dev.detekt.gradle.plugin.getSupportedKotlinVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.springframework.boot.gradle.tasks.bundling.BootJar

val ktlintVersion = "1.8.0"
val klageAuthVersion = "0.4.0"
val klageKodeverkVersion = "3.4.9"

plugins {
    val kotlinVersion = "2.4.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.6"
    idea
    kotlin("kapt") version kotlinVersion
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("no.nav.klage:klage-auth:$klageAuthVersion")
    implementation("no.nav.klage:klage-kodeverk:$klageKodeverkVersion")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("no.nav.klage:klage-auth-test:$klageAuthVersion")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.2.7")
}

ktlint {
    version.set(ktlintVersion)
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        exclude { it.file.path.contains("${File.separator}build${File.separator}") }
    }
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(false)
}

// io.spring.dependency-management applies the Kotlin BOM to every configuration,
// which would force the linters onto the project's Kotlin compiler. Both tools are
// built against a specific compiler version and break when it changes.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(getSupportedKotlinVersion())
        }
    }
}

// ktlint-gradle has no equivalent of getSupportedKotlinVersion(), so keep the
// version that ktlint itself declares instead of the one forced by the BOM.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(requested.version!!)
        }
    }
}

// NamedArguments implements RequiresAnalysisApi, so it only reports when detekt
// runs with a compile classpath. The plain `detekt` task has no classpath and
// would silently pass, hence the analysis aware tasks are wired into `check`
// and the plain one is disabled.
tasks.named("detekt") {
    enabled = false
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget.set(JvmTarget.JVM_25.target)
    reports {
        html.required.set(true)
        checkstyle.required.set(true)
        sarif.required.set(false)
        markdown.required.set(false)
    }
}

tasks.named("check") {
    dependsOn("detektMain", "detektTest")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("kant-api.jar")
}
