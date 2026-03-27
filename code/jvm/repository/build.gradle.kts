plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":domain"))
    testFixturesImplementation(rootProject.libs.spring.boot.starter.test)
}
