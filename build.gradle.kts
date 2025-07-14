import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    application
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "io.github.thebestandgreatest"
version = "0.2.0"

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
    implementation("club.arson.impulse:api:v0.3.2")
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

application {
    val name = "io.github.thebestandgreatest"
    mainClass.set(name)
}

tasks.withType<ShadowJar> {
    description = "Crafty Controller broker for Impulse."
}