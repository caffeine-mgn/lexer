plugins {
    kotlin("jvm")
    `maven-publish`
}
apply<pw.binom.plugins.BinomPublishPlugin>()
dependencies {
    api(kotlin("stdlib"))
    testApi(kotlin("test"))
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
        testLogging.showStandardStreams = true
        testLogging.showCauses = true
        testLogging.showExceptions = true
        testLogging.showStackTraces = true
        testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

apply<pw.binom.plugins.DocsPlugin>()

publishing {
    publications {
        create<MavenPublication>("BinomRepository") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
        }
    }
}
