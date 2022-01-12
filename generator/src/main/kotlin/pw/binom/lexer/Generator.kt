package pw.binom.lexer

object Generator {
    fun generate(path: String, lexer: Lexer, appendable: SourceAppender) {
        val rules = lexer.sortRooRules()
        val kg = KotlinGenerator(lexer, appendable)
        if (lexer.packageName.isNotEmpty()) {
            if (lexer.packageName.isBlank()) {
                throw IllegalArgumentException("Lexer $path has blank packageName property")
            }
            appendable.t("package ").t(lexer.packageName).t("\n\n")
        }
        if (lexer.tokenTypeName.isBlank()) {
            throw IllegalArgumentException("Lexer $path has blank tokenTypeName property")
        }
        appendable.ln("@OptIn(ExperimentalStdlibApi::class)")
            .ln("open class ${lexer.lexerClass}(val source: String) : Sequence<${lexer.lexerClass}.${lexer.tokenTypeName}?> {") {
                ln("sealed interface SimpleToken : ${lexer.tokenTypeName} {") {}
                ln("}")

                ln("sealed interface StringToken: SimpleToken {") {
                    ln("val string: String")
                    ln("override val body: String") {
                        ln("get() = string")
                    }
                }.ln("}")
                ln("sealed interface RegexpToken: SimpleToken {") {
                    ln("val regexp: Regex")
                }.ln("}")
            }



        appendable.ln("sealed interface ${lexer.tokenTypeName} {") {
            ln("val body: String")
            ln("val length: Int get() = body.length")
            ln("val position: Int")
            ln("val column: Int")
            ln("val line: Int")
            ln("object Exist:${lexer.tokenTypeName} {") {
                ln("override val body get()=\"\"")
                ln("override val position get()=0")
                ln("override val column get()=0")
                ln("override val line get()=0")
            }.ln("}")
        }
        lexer.rules.forEach {
            val value = it.value
            kg.generateClass(rule = value, level = 2)
        }
        appendable.ln("\t}")


        appendable.ln("\tfun parse() = TokenIterator(source)")
        appendable.ln("\toverride fun iterator() = parse()")
        appendable.ln("")
        appendable.ln("\tclass TokenIterator(val source: String) : Iterator<${lexer.tokenTypeName}?> {") {
            ln("var position = 0"){
                ln("private set")
            }
            ln("var column = 0"){
                ln("private set")
            }
            ln("var line = 0"){
                ln("private set")
            }
            ln("private var value: String = \"\"")
            ln("private val stack = ArrayList<StackItem>()")
            lexer.inits.forEach {
                ln(it)
            }
        }
        appendable.pad(2).ln("private class StackItem(val position: Int, val column: Int, val line: Int)")
        appendable.padding(2) {
            ln("private fun makeState() = StackItem(") {
                ln("position = position,")
                ln("column = column,")
                ln("line = line,")
            }.ln(")")
            ln("private fun setState(state:StackItem) {") {
                ln("position = state.position")
                ln("column = state.column")
                ln("line = state.line")
            }.ln("}")
            appendable.ln("private inline fun <T> r(state: StackItem, func: ()->Unit) : T?{"){
                ln("setState(state)")
                ln("func()")
                ln("return null")
            }.ln("}")
            ln("fun push() {") {
                ln("stack += makeState()")
            }
        }.ln("}").ln("fun pop() : Boolean{") {
            ln("val item = stack.removeLastOrNull() ?: return false")
            ln("setState(item)")
            ln("return true")
        }.ln("}").ln("fun skip() : Boolean{") {
            ln("stack.removeLastOrNull() ?: return false")
            ln("return true")
        }.ln("}")
        kg.generateUtils()


        appendable.t("\t\toverride fun hasNext(): Boolean = position < source.length\n")

            .t("\t\tprivate fun move(result:String){\n").t("\t\t\tposition += result.length\n").t("\t\t}\n")


            .ln("\t\tprivate fun assertString(str:String):Boolean{")
            .ln("\t\t\tif (str.length + position > source.length) {").ln("\t\t\t\treturn false").ln("\t\t\t}")
            .ln("\t\t\treturn source.substring(position, position + str.length)==str").ln("\t\t}")
        appendable.ln("private inline fun <T> readList(f: () -> T?): List<T> {") {
            ln("val list = ArrayList<T>()")
            ln("while (true) {") {
                ln("list += f() ?: break")
                ln("}")
            }
            ln("return list")
        }.ln("}")

//        appendable.ln("private inline fun <T : Expression> and(f1: () -> T?, f2: () -> T?): T? {") {
//            ln("push()")
//            ln("val left = f1()")
//            ln("if (left==null){") {
//                ln("pop()")
//                ln("return null")
//            }.ln("}")
//            ln("val right = f2()")
//            ln("if (right == null) {") {
//                ln("pop()")
//            }.ln("}")
//            ln("return right")
//        }.ln("}")





        lexer.rules.forEach {
            val value = it.value
            kg.generateRead(value, 2)
        }

        appendable.ln("\t\toverride fun next(): ${lexer.tokenTypeName}? {").ln("\t\t\tif (!hasNext()) {")
            .ln("\t\t\t\tthrow NoSuchElementException()").ln("\t\t\t}")
        if (lexer.rules.isEmpty()) {
            appendable.t("\t\t\treturn null\n")
        } else {
            appendable.t("\t\t\treturn ")
            var first = true
            rules.forEach {
                if (!first) {
                    appendable.t("\t\t\t\t?: ")
                }
                first = false
                appendable.ln("read${it.first}()")
            }
            if (!first) {
                appendable.t("\t\t\t\t")
            }
            appendable.ln("")
        }
        appendable.t("\t\t}\n")
        appendable.t("\t}\n")


        appendable.t("}\n")
        lexer.rules.forEach {
            val value = it.value
            when (value) {
                is Rule.Regexp -> appendable.t(
                    "private val __${it.key} = \"${
                        value.regexp
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\r", "\\r")
                            .replace("\n", "\\n")
                            .replace("\t", "\\t")
                    }\".toRegex()\n"
                )
                is Rule.StrExp -> appendable.t(
                    "private const val __${it.key} = \"${
                        value.string
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\r", "\\r")
                            .replace("\n", "\\n")
                            .replace("\t", "\\t")
                    }\"\n"
                )
            }
        }

    }
}

val Appendable.padding
    get() = when (this) {
        is PaddingAppendable -> count
        else -> 0
    }

fun <T : Appendable> T.pad(count: Int): T {
    repeat(count) {
        append('\t')
    }
    return this
}

fun <T : Appendable> T.level(func: Appendable.() -> Unit): T {
    withPadding(padding + 1).func()
    return this
}

operator fun <T : Appendable> T.invoke(func: Appendable.() -> Unit): T = level(func)

fun <T : Appendable> T.ln(text: String): T {
    append(text).append('\n')
    return this
}

fun <T : Appendable> T.ln(text: String, func: Appendable.() -> Unit): T {
    ln(text).level(func)
    return this
}

fun <T : Appendable> T.t(text: String): T {
    append(text)
    return this
}

fun Appendable.addPadding(count: Int): Appendable = withPadding(this.padding + count)

fun Appendable.withPadding(count: Int): Appendable = when (this) {
    is PaddingAppendable -> appendable.withPadding(count)
    else -> PaddingAppendable(this, count)
}

private class PaddingAppendable(val appendable: Appendable, val count: Int) : Appendable {
    private fun padding() {
        appendable.pad(count)
    }

    override fun append(csq: CharSequence?): java.lang.Appendable {
        padding()
        appendable.append(csq)
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable {
        padding()
        appendable.append(csq, start, end)
        return this
    }

    override fun append(c: Char): java.lang.Appendable {
        padding()
        appendable.append(c)
        return this
    }
}