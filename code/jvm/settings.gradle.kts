plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "instant-messaging"

// Subprojects inclusion
include(":domain")
include(":repository")
include(":service")
include(":repository-jdbi")
include(":infrastructure")
include(":http-api")
include(":http-pipeline")
include(":host")
