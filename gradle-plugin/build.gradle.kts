plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

apply<pw.binom.plugins.BinomPublishPlugin>()
kotlin.sourceSets["main"].kotlin.srcDir(project.buildDir.resolve("gen"))
dependencies {
    api(kotlin("stdlib"))
    api(kotlin("gradle-plugin"))
    api(gradleApi())
}

tasks {
    val compileKotlin by getting
    val generateVersion = create("generateVersion") {
        val sourceDir = project.buildDir.resolve("gen/pw/binom/lexer")
        sourceDir.mkdirs()
        val versionSource = sourceDir.resolve("version.kt")
        outputs.files(versionSource)
        inputs.property("version", project.version)

        versionSource.writeText(
            """package pw.binom.lexer
            
const val LEXER_VERSION = "${project.version}"
const val LEXER_GROUP = "${project.group}"
"""
        )
    }
    compileKotlin.dependsOn(generateVersion)
}

gradlePlugin {
    plugins {
        create("lexer") {
            id = "pw.binom.static-lexer"
            implementationClass = "pw.binom.lexer.LexerPlugin"
            description = "Binom Static Lexer Generator"
        }
    }
}
apply<pw.binom.plugins.DocsPlugin>()
