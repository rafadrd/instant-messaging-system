plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" }

rootProject.name = "instant-messaging"

include("domain")
include("repository")
include("service")
include("http-api")
include("repository-jdbi")
include("host")
include("http-pipeline")
include("infrastructure")
