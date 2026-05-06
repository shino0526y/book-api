buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.flywaydb:flyway-database-postgresql:11.18.0")
	}
}

plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	war
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.flywaydb.flyway") version "11.18.0"
}

group = "io.github.shino0526y"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

val postgresHost = providers.environmentVariable("POSTGRES_HOST").orElse("localhost")
val postgresPort = providers.environmentVariable("POSTGRES_PORT").orElse("5432")
val postgresDb = providers.environmentVariable("POSTGRES_DB").orElse("book_api")
val postgresUser = providers.environmentVariable("POSTGRES_USER").orElse("book_api")
val postgresPassword = providers.environmentVariable("POSTGRES_PASSWORD").orElse("book_api")

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("org.postgresql:postgresql")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat-runtime")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

flyway {
	url = "jdbc:postgresql://${postgresHost.get()}:${postgresPort.get()}/${postgresDb.get()}"
	user = postgresUser.get()
	password = postgresPassword.get()
	locations = arrayOf("filesystem:${projectDir}/src/main/resources/db/migration")
	baselineOnMigrate = true
	cleanDisabled = false
}

tasks.withType<Test> {
	useJUnitPlatform()
}
