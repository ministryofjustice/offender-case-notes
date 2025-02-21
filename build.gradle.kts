import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "7.1.2"
  kotlin("plugin.spring") version "2.1.10"
  kotlin("plugin.jpa") version "2.1.10"
  id("io.gatling.gradle") version "3.13.4.1"
  jacoco
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

dependencies {
  annotationProcessor("org.projectlombok:lombok:1.18.36")

  compileOnly("org.projectlombok:lombok:1.18.36")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.3.1")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.2.0")

  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
  implementation("net.sf.ehcache:ehcache:2.10.9.2")
  implementation("org.apache.commons:commons-text:1.13.0")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")
  implementation("com.google.guava:guava:33.4.0-jre")

  implementation(platform("com.amazonaws:aws-java-sdk-bom:1.12.782"))
  implementation("software.amazon.awssdk:sns:2.30.25")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
  testCompileOnly("org.projectlombok:lombok:1.18.36")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.0")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
  testImplementation("org.wiremock:wiremock-standalone:3.12.0")
  testImplementation("org.testcontainers:postgresql:1.20.5")
  testImplementation("org.testcontainers:localstack:1.20.5")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.awaitility:awaitility:4.2.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")

  testImplementation("org.springframework.security.oauth:spring-security-oauth2:2.5.2.RELEASE")
  testImplementation("org.springframework.security:spring-security-jwt:1.1.1.RELEASE")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.25")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
      freeCompilerArgs.add("-Xwhen-guards")
    }
  }

  register("initialiseDatabase", Test::class) {
    include("**/SchemaSpyIntTest.class")
  }

  test {
    exclude("**/SchemaSpyIntTest.class")
  }

  getByName("initialiseDatabase") {
    onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
  }
}

// Jacoco code coverage
tasks.named("test") {
  finalizedBy("jacocoTestReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}
