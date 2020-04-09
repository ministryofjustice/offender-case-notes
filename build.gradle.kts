import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

plugins {
  kotlin("jvm") version "1.3.71"
  kotlin("plugin.spring") version "1.3.71"
  id("org.springframework.boot") version "2.2.6.RELEASE"
  id("io.spring.dependency-management") version "1.0.9.RELEASE"
  id("org.owasp.dependencycheck") version "5.3.2.1"
  id("com.github.ben-manes.versions") version "0.28.0"
  id("se.patrikerdes.use-latest-versions") version "0.2.13"
}

repositories {
  mavenLocal()
  mavenCentral()
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "11"
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

dependencyCheck {
  failBuildOnCVSS = 5f
  suppressionFiles = listOf("dependency-check-suppress-spring.xml")
  format = ALL
  analyzers.assemblyEnabled = false
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    isNonStable(candidate.version) && !isNonStable(currentVersion)
  }
}

group = "uk.gov.justice.digital.hmpps"

val todaysDate: String = LocalDate.now().format(ISO_DATE)
val today: Instant = Instant.now()
version = if (System.getenv().contains("CI")) "${todaysDate}.${System.getenv("CIRCLE_BUILD_NUM")}" else todaysDate

springBoot {
  buildInfo {
    properties {
      time = today
      additional = mapOf(
          "by" to System.getProperty("user.name"),
          "operatingSystem" to "${System.getProperty("os.name")} (${System.getProperty("os.version")})",
          "continuousIntegration" to System.getenv().containsKey("CI"),
          "machine" to InetAddress.getLocalHost().hostName
      )
    }
  }
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

dependencyManagement {
  imports { mavenBom(SpringBootPlugin.BOM_COORDINATES) }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  annotationProcessor("org.projectlombok:lombok:1.18.8")

  compileOnly("org.projectlombok:lombok:1.18.8")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.flywaydb:flyway-core:6.3.3")
  runtimeOnly("org.postgresql:postgresql:42.2.12")

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.hibernate:hibernate-java8")

  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.security.oauth:spring-security-oauth2:2.4.0.RELEASE")
  implementation("org.springframework.security:spring-security-jwt:1.1.0.RELEASE")

  implementation("org.springframework:spring-jms")
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")

  implementation("net.logstash.logback:logstash-logback-encoder:6.3")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.0")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.0")
  implementation("com.github.timpeeters:spring-boot-graceful-shutdown:2.2.1")

  implementation("javax.annotation:javax.annotation-api:1.3.2")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:2.3.2")
  implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("io.springfox:springfox-swagger2:2.9.2")
  implementation("io.springfox:springfox-swagger-ui:2.9.2")

  implementation("io.jsonwebtoken:jjwt:0.9.1")

  implementation("net.sf.ehcache:ehcache:2.10.6")
  implementation("org.apache.commons:commons-lang3:3.10")
  implementation("org.apache.commons:commons-text:1.8")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.3")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
  implementation("com.pauldijou:jwt-core_2.11:4.3.0")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("com.google.guava:guava:28.2-jre")

  implementation("software.amazon.awssdk:sns:2.11.12")

  testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.8")
  testCompileOnly("org.projectlombok:lombok:1.18.8")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux")
  testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.17.0")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.26.3")
  testImplementation("com.nhaarman:mockito-kotlin-kt1.1:1.6.0")
  testImplementation("org.testcontainers:localstack:1.13.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.0.2")
}

tasks {
  test { useJUnitPlatform() }

  val agentDeps by configurations.register("agentDeps") {
    dependencies {
      "agentDeps"("com.microsoft.azure:applicationinsights-agent:2.6.0") {
        isTransitive = false
      }
    }
  }

  val copyAgent by registering(Copy::class) {
    from(agentDeps)
    into("$buildDir/libs")
  }

  assemble { dependsOn(copyAgent) }
}
