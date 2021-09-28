plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.9-beta"
  kotlin("plugin.spring") version "1.5.31"
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

dependencies {
  annotationProcessor("org.projectlombok:lombok:1.18.20")

  compileOnly("org.projectlombok:lombok:1.18.20")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.flywaydb:flyway-core:7.15.0")
  runtimeOnly("org.postgresql:postgresql:42.2.23")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.0.2")

  implementation("com.google.code.gson:gson:2.8.8")
  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("io.springfox:springfox-boot-starter:3.0.0")

  implementation("net.sf.ehcache:ehcache:2.10.9.2")
  implementation("org.apache.commons:commons-text:1.9")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.5")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")
  implementation("com.google.code.gson:gson:2.8.8")

  implementation("software.amazon.awssdk:sns:2.17.34")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.20")
  testCompileOnly("org.projectlombok:lombok:1.18.20")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.awaitility:awaitility:4.1.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.1.0")

  testImplementation("org.springframework.security.oauth:spring-security-oauth2:2.5.1.RELEASE")
  testImplementation("org.springframework.security:spring-security-jwt:1.1.1.RELEASE")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
