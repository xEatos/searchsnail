plugins {
	kotlin("jvm") version "2.0.21"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.serialization") version "1.9.25"
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.borgnetzwerk"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")


	implementation("org.springframework.boot:spring-boot-starter-graphql")

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework:spring-webflux")
	testImplementation("org.springframework.graphql:spring-graphql-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")


	implementation("io.arrow-kt:arrow-core:1.2.4")

	// kotest test framework
	testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
	testImplementation("io.kotest:kotest-assertions-core:5.9.1")
	testImplementation("io.kotest:kotest-property:5.9.1")

	// json parser
	implementation("com.google.code.gson:gson:2.11.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

	//  --- youtube api ---
	implementation("com.google.api-client:google-api-client:2.4.0")
	implementation("com.google.oauth-client:google-oauth-client-jetty:2.4.0")
	implementation("com.google.apis:google-api-services-youtube")

	// https://mvnrepository.com/artifact/com.google.api-client/google-api-client-java6
	implementation("com.google.api-client:google-api-client-java6:2.1.4")

	// https://mvnrepository.com/artifact/com.google.oauth-client/google-oauth-client-jetty
	implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")

	// https://mvnrepository.com/artifact/com.google.apis/google-api-services-youtube
	implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")



}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

