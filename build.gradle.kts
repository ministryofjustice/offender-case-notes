plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.0.0-BETA"
  kotlin("plugin.spring") version "1.4.21"
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

dependencies {
  annotationProcessor("org.projectlombok:lombok:1.18.16")

  compileOnly("org.projectlombok:lombok:1.18.16")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.flywaydb:flyway-core:7.4.0")
  runtimeOnly("org.postgresql:postgresql:42.2.18")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("org.springframework:spring-jms")
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")

  implementation("com.google.code.gson:gson:2.8.6")
  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("io.springfox:springfox-boot-starter:3.0.0")

  implementation("net.sf.ehcache:ehcache:2.10.6")
  implementation("org.apache.commons:commons-text:1.9")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
  implementation("com.pauldijou:jwt-core_2.11:4.3.0")
  implementation("com.google.code.gson:gson:2.8.6")

  implementation("software.amazon.awssdk:sns:2.15.58")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.16")
  testCompileOnly("org.projectlombok:lombok:1.18.16")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.22.0")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")

  testImplementation("org.springframework.security.oauth:spring-security-oauth2:2.5.0.RELEASE")
  testImplementation("org.springframework.security:spring-security-jwt:1.1.1.RELEASE")

  agentDeps("com.microsoft.azure:applicationinsights-agent:3.0.1-BETA.2")
}
