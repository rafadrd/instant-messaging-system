plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(libs.jakarta.inject.api)
    testFixturesImplementation(rootProject.libs.spring.boot.starter.test)
}
