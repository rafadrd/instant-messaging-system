import java.util.Properties

plugins {
    // Kotlin plugins
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.spring) apply false

    // Spring plugins
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false

    // Linting plugin
    alias(libs.plugins.ktlint) apply false

    // Test logging plugin
    alias(libs.plugins.test.logger) apply false
}

val dockerComposePath: String = rootProject.file("docker-compose.yml").absolutePath

val dbTestsUp by tasks.registering(Exec::class) {
    commandLine("docker", "compose", "-f", dockerComposePath, "up", "-d", "postgres")
}

val dbTestsWait by tasks.registering(Exec::class) {
    dependsOn(dbTestsUp)
    commandLine("docker", "exec", "ims-postgres", "sh", "-c", "until pg_isready -U dbuser -d db; do sleep 1; done")
}

val dbTestsDown by tasks.registering(Exec::class) {
    commandLine("docker", "compose", "-f", dockerComposePath, "stop", "postgres")
}

dbTestsDown.configure {
    mustRunAfter(subprojects.map { it.tasks.withType<Test>() })
}

fun loadEnv(project: Project): Map<String, String> {
    val envFile =
        project.rootProject.layout.projectDirectory
            .file(".env")
    val content =
        project.providers
            .fileContents(envFile)
            .asText.orNull ?: return emptyMap()

    val props = Properties()
    props.load(content.byteInputStream())
    return props.entries.associate { it.key.toString() to it.value.toString() }
}

val junitApi = libs.junit.jupiter.api
val junitParams = libs.junit.jupiter.params
val junitEngine = libs.junit.jupiter.engine

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    group = "pt.isel"
    version = "0.0.1-SNAPSHOT"

    repositories { mavenCentral() }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> { jvmToolchain(21) }

    dependencies {
        "implementation"(kotlin("test"))
        "testImplementation"(junitApi)
        "testImplementation"(junitParams)
        "testRuntimeOnly"(junitEngine)
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        val envFile =
            project.rootProject.layout.projectDirectory
                .file(".env")
        inputs.file(envFile).optional().withPropertyName("envFile")

        val envVars = loadEnv(project)
        environment(envVars)

        if (!envVars.containsKey("DB_URL")) {
            environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=dbuser&password=isel")
        }

        dependsOn(dbTestsWait)
        finalizedBy(dbTestsDown)
    }
}
