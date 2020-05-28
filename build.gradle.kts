plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "0.4.0"
  kotlin("plugin.spring") version "1.3.72"
}

configurations {
  implementation { exclude(mapOf("module" to "tomcat-jdbc")) }
}

extra["spring-security.version"] = "5.3.2.RELEASE"

dependencies {

   // Lombok version >= 1.18.10 is incompatible with jackson-databind <= 2.11.0 due to a change on Builder.Default's internal property naming.  This breaks CaseNoteResourceTest.
  annotationProcessor("org.projectlombok:lombok:1.18.8")

  compileOnly("org.projectlombok:lombok:1.18.8")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.flywaydb:flyway-core:6.4.3")
  runtimeOnly("org.postgresql:postgresql:42.2.12")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("org.springframework:spring-jms")
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")

  implementation("javax.annotation:javax.annotation-api:1.3.2")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:2.3.3")
  implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("javax.activation:activation:1.1.1")
  implementation("javax.transaction:javax.transaction-api:1.3")

  implementation("io.springfox:springfox-swagger2:2.9.2")
  implementation("io.springfox:springfox-swagger-ui:2.9.2")
  implementation("io.swagger:swagger-core:1.6.1")

  implementation("net.sf.ehcache:ehcache:2.10.6")
  implementation("org.apache.commons:commons-text:1.8")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
  implementation("com.pauldijou:jwt-core_2.11:4.3.0")
  implementation("com.google.code.gson:gson:2.8.6")

  implementation("software.amazon.awssdk:sns:2.13.25")

  testAnnotationProcessor("org.projectlombok:lombok:1.18.12")
  testCompileOnly("org.projectlombok:lombok:1.18.12")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.tngtech.java:junit-dataprovider:1.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.17.0")
  testImplementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.4")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.26.3")
  testImplementation("org.testcontainers:localstack:1.13.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.0.3")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")

  testImplementation("org.springframework.security.oauth:spring-security-oauth2:2.4.1.RELEASE")
  testImplementation("org.springframework.security:spring-security-jwt:1.1.0.RELEASE")
}

