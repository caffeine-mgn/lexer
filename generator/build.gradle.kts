plugins {
    kotlin("jvm")
}
apply<pw.binom.plugins.BinomPublishPlugin>()
dependencies {
    api(kotlin("stdlib"))
    testApi(kotlin("test"))
}

tasks{
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