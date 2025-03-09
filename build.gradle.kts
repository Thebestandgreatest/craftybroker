plugins {
	kotlin("jvm") version "2.1.10"
	kotlin("plugin.serialization") version "2.1.10"
}

group = "io.github.thebestandgreatest"
version = "0.1.0"

repositories {
	mavenCentral()
	maven {
		name = "Impulse"
		url = uri("https://maven.pkg.github.com/Arson-Club/Impulse")
		credentials {
			username = project.findProperty("gpr.user").toString()
			password = project.findProperty("gpr.token").toString()
		}
	}
}

val ktorVersion: String by project
val logbackVersion: String by project

dependencies {
	implementation("club.arson.impulse:api:0.3.1-SNAPSHOT")
	implementation("org.slf4j:slf4j-api:2.0.7")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
	implementation("ch.qos.logback:logback-classic:$logbackVersion")
	implementation("io.ktor:ktor-client-core:$ktorVersion")
	implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
	implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
	implementation("io.ktor:ktor-client-cio:$ktorVersion")
	implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
	implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}