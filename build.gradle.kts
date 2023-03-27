plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.8.5-beta-2"
  kotlin("plugin.spring") version "1.8.10"
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

dependencies {
  annotationProcessor("org.projectlombok:lombok:1.18.26")

  compileOnly("org.projectlombok:lombok:1.18.26")

  runtimeOnly("com.h2database:h2:2.1.214")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.6.0")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.2.0")

  implementation("com.google.code.gson:gson:2.10.1")
  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.15")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.15")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.15")

  implementation("net.sf.ehcache:ehcache:2.10.9.2")
  implementation("org.apache.commons:commons-text:1.10.0")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")
  implementation("com.google.code.gson:gson:2.10.1")

  implementation("software.amazon.awssdk:sns:2.20.32")

  implementation("io.swagger:swagger-core:1.6.10")
  implementation("io.swagger.core.v3:swagger-core:2.2.9")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.26")
  testCompileOnly("org.projectlombok:lombok:1.18.26")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.37.0")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.awaitility:awaitility:4.2.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

  testImplementation("org.springframework.security.oauth:spring-security-oauth2:2.5.2.RELEASE")
  testImplementation("org.springframework.security:spring-security-jwt:1.1.1.RELEASE")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.13")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}
