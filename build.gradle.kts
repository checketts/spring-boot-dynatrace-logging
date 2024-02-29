import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.2.3"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("jvm") version "1.9.22"
	kotlin("plugin.spring") version "1.9.22"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}



repositories {
	mavenCentral()
}

configurations {
	this["implementation"].exclude(group="org.springframework.boot", module="spring-boot-starter-logging") //Exclude logging starter to remove logback
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("org.springframework.boot:spring-boot-starter-log4j2")
	runtimeOnly("org.apache.logging.log4j:log4j-spring-boot")
	runtimeOnly("org.apache.logging.log4j:log4j-layout-template-json")
	runtimeOnly("com.lmax:disruptor:3.3.4") { because("Required for log4j2 async appenders")}

	implementation("com.dynatrace.oneagent.sdk.java:oneagent-sdk:1.9.0")

	runtimeOnly("io.micrometer:micrometer-registry-dynatrace")



	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
