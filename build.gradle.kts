import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.2.0"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("plugin.jpa") version "2.1.21"
  id("io.gatling.gradle") version "3.14.3"
  jacoco
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

dependencies {
  annotationProcessor("org.projectlombok:lombok:1.18.38")

  compileOnly("org.projectlombok:lombok:1.18.38")

  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.5")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.13.2")

  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
  implementation("org.apache.commons:commons-text:1.13.1")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")
  implementation("com.google.guava:guava:33.4.8-jre")

  implementation(platform("com.amazonaws:aws-java-sdk-bom:1.12.783"))
  implementation("software.amazon.awssdk:sns:2.31.51")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
  testCompileOnly("org.projectlombok:lombok:1.18.38")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
  testImplementation("org.wiremock:wiremock-standalone:3.13.0")
  testImplementation("org.testcontainers:postgresql:1.21.0")
  testImplementation("org.testcontainers:localstack:1.21.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.awaitility:awaitility:4.3.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")

  testImplementation("org.springframework.security.oauth:spring-security-oauth2:2.5.2.RELEASE")
  testImplementation("org.springframework.security:spring-security-jwt:1.1.1.RELEASE")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.29")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencyCheck {
  suppressionFiles.add("dependencyCheck/suppression.xml")
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
