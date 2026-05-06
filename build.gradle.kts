import nu.studer.gradle.jooq.JooqEdition

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
	id("nu.studer.jooq") version "10.2.1"
}

group = "io.github.shino0526y"
version = "0.0.1-SNAPSHOT"

val jooqVersion = "3.19.32"
val postgresDriverVersion = "42.7.10"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["jooq.version"] = jooqVersion

val postgresHost = providers.environmentVariable("POSTGRES_HOST").orElse("localhost")
val postgresPort = providers.environmentVariable("POSTGRES_PORT").orElse("5432")
val postgresDb = providers.environmentVariable("POSTGRES_DB").orElse("book_api")
val postgresUser = providers.environmentVariable("POSTGRES_USER").orElse("book_api")
val postgresPassword = providers.environmentVariable("POSTGRES_PASSWORD").orElse("book_api")
val jooqOutputDir = layout.buildDirectory.dir("generated-src/jooq/main")

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	jooqGenerator("org.postgresql:postgresql:$postgresDriverVersion")
	runtimeOnly("org.postgresql:postgresql")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat-runtime")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}

	sourceSets.named("main") {
		kotlin.srcDir(jooqOutputDir)
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

jooq {
	version.set(jooqVersion)
	edition.set(JooqEdition.OSS)

	configurations {
		create("main") {
			generateSchemaSourceOnCompilation.set(false)

			jooqConfiguration.apply {
				logging = org.jooq.meta.jaxb.Logging.WARN

				jdbc.apply {
					driver = "org.postgresql.Driver"
					url = "jdbc:postgresql://${postgresHost.get()}:${postgresPort.get()}/${postgresDb.get()}"
					user = postgresUser.get()
					password = postgresPassword.get()
				}

				generator.apply {
					name = "org.jooq.codegen.KotlinGenerator"

					database.apply {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "public"
						includes = ".*"
						excludes = "flyway_schema_history"
					}

					target.apply {
						packageName = "io.github.shino0526y.book_api.generated.jooq"
						directory = jooqOutputDir.get().asFile.path
					}
				}
			}
		}
	}
}

tasks.named("generateJooq") {
	dependsOn(tasks.named("flywayMigrate"))
}

tasks.named("compileKotlin") {
	dependsOn(tasks.named("generateJooq"))
}

tasks.withType<Test> {
	useJUnitPlatform()
}
