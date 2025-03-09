plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "craftybroker"
include("src:test:NetTest")
findProject(":src:test:NetTest")?.name = "NetTest"
