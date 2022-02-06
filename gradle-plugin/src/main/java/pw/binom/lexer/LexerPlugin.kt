package pw.binom.lexer

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class LexerPlugin : Plugin<Project> {
    fun findRuntimeClasspath(target: Project) =
        when {
            "runtime" in target.configurations.names -> target.configurations.getAt("runtime")
            "runtimeClasspath" in target.configurations.names -> target.configurations.getAt("runtimeClasspath")
            else -> throw GradleException("Can't find runtime configuration")
        }

    override fun apply(target: Project) {
        val extension = target.extensions.create("lexer", LexerExtension::class.java)
        target.pluginManager.apply("kotlin-platform-jvm")
        target.gradle.addListener(object : DependencyResolutionListener {
            override fun beforeResolve(dependencies: ResolvableDependencies) {
                val config = findRuntimeClasspath(target)
                config.dependencies.add(target.dependencies.create("org.jetbrains.kotlin:kotlin-stdlib:${KotlinVersion.CURRENT}"))
//                config.dependencies.add(target.dependencies.create("pw.binom.static-css:generator:0.1.30"))
//                config.dependencies.add(target.dependencies.create("pw.binom.lexer:generator:0.1.30"))
                target.dependencies.add("api", target.dependencies.create("$LEXER_GROUP:generator:$LEXER_VERSION"))
                target.gradle.removeListener(this)
            }

            override fun afterResolve(dependencies: ResolvableDependencies) {
            }
        })
        extension.outputFile.set(target.buildDir.resolve("lexer/${target.name}.kt"))
        val generateMainTask = target.tasks.register("generateLexerMainSource", GenerateMain::class.java)
        val compileKotlin = target.tasks.findByName("compileKotlin") as KotlinCompile
        compileKotlin.dependsOn(generateMainTask.get())
        val mainFileDir = target.buildDir.resolve("lexer-lexer-main")
        generateMainTask.get().mainFile.set(mainFileDir.resolve("GeneratedMain.kt"))
        val kotlin = target.extensions.getByName("kotlin") as KotlinJvmProjectExtension
        kotlin.sourceSets.findByName("main")!!.kotlin.srcDir(mainFileDir)
        val generateLexer = target.tasks.register("buildLexer", GenerateLexer::class.java)
        generateLexer.get().dependsOn(compileKotlin)
        val runtimeClasspath = findRuntimeClasspath(target)
        generateLexer.get().classpath = compileKotlin.outputs.files + runtimeClasspath
    }
}

abstract class GenerateLexer : JavaExec() {
    init {
        group = "build"
        mainClass.set("pw.binom.lexer.GeneratedMain")
        val extension = project.extensions.getByType(LexerExtension::class.java)
        argumentProviders += CommandLineArgumentProvider { arrayListOf(extension.outputFile.asFile.get().absolutePath) }
    }

    override fun exec() {
        classpath += project.configurations.getByName("compileClasspath")
        super.exec()
    }
}

abstract class GenerateMain : DefaultTask() {
    @OutputFile
    val mainFile = project.objects.fileProperty()

    init {
        group = "build"
        doFirst {
            mainFile.get().asFile.parentFile!!.mkdirs()
        }
    }

    @TaskAction
    fun execute() {
        mainFile.asFile.get().parentFile.mkdirs()
        mainFile.asFile.get().outputStream().bufferedWriter().use {
            it.append("// ---===GENERATED===--- //\n")
            it.append("package pw.binom.lexer\n\n")
                .append("import java.io.File\n\n")
                .append("object GeneratedMain{\n")
                .append("\t@JvmStatic\n")
                .append("\tfun main(args:Array<String>){\n")
                .append("val file = File(args[0])\n")
                .append("file.parentFile!!.mkdirs()\n")
                .append("\t\tfile.outputStream().bufferedWriter().use {\n")
            val extension = project.extensions.getByType(LexerExtension::class.java)
            extension.lexers.get().forEach { path ->
                it.append("\t\t\tGenerator.generate(path = \"$path\", lexer = $path, appendable = SourceAppender2(it))\n")
            }
            it.append("\t\t}\n")
                .append("\t}\n")
                .append("}")
        }
    }
}
