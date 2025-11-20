import java.io.FileInputStream
import java.util.Properties

dependencies {
    // Module dependencies
    implementation(project(":repository"))
    implementation(project(":domain"))

    // JDBI and Postgres dependencies
    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.kotlin)
    implementation(libs.jdbi3.postgres)
    implementation(libs.postgresql)

    // Kotlin dependencies
    implementation(libs.kotlin.reflect)
}

fun loadEnv(): Map<String, String> {
    val envFile =
        project.projectDir.parentFile
            ?.parentFile
            ?.parentFile
            ?.resolve(".env")

    if (envFile == null || !envFile.exists()) {
        return emptyMap()
    }

    val props = Properties()
    props.load(FileInputStream(envFile))
    return props.entries.associate { it.key.toString() to it.value.toString() }
}

tasks.withType<Test> {
    val envVars = loadEnv()
    environment(envVars)

    if (!envVars.containsKey("DB_URL")) {
        environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=dbuser&password=isel")
    }

    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}

// DB related tasks
val composeFileDir = rootProject.layout.projectDirectory.dir("../../")
val dockerComposePath = composeFileDir.file("docker-compose.yml").toString()

task<Exec>("dbTestsUp") {
    commandLine(
        "docker",
        "compose",
        "-f",
        dockerComposePath,
        "up",
        "-d",
        "postgres",
    )
}

task<Exec>("dbTestsWait") {
    commandLine("docker", "exec", "ims-postgres", "/app/bin/wait-for-postgres.sh", "localhost")
    dependsOn("dbTestsUp")
}

task<Exec>("dbTestsDown") {
    commandLine("docker", "compose", "-f", dockerComposePath, "stop", "postgres")
}
