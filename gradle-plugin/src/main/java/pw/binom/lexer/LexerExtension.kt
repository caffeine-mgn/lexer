package pw.binom.lexer

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile

abstract class LexerExtension {
    @get:Input
    abstract val lexers: ListProperty<String>
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    fun lexer(lexer: String) {
        lexers.add(lexer)
    }
}