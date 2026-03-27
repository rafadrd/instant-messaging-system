plugins {
    java
    alias(libs.plugins.spring.boot) apply false
}

allprojects {
    group = "pt.isel"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// Subprojects configuration
subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        add("api", platform(rootProject.libs.spring.boot.dependencies))
        add("testImplementation", rootProject.libs.spring.boot.starter.test)
        add("testRuntimeOnly", rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
