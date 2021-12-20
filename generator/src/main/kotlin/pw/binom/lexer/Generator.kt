package pw.binom.lexer

object Generator {
    fun generate(path: String, lexer: Lexer, appendable: Appendable) {
        if (lexer.packageName.isNotEmpty()) {
            if (lexer.packageName.isBlank()) {
                throw IllegalArgumentException("Lexer $path has blank packageName property")
            }
            appendable.append("package ").append(lexer.packageName).append("\n\n")
        }
        if (lexer.tokenTypeName.isBlank()) {
            throw IllegalArgumentException("Lexer $path has blank tokenTypeName property")
        }

        appendable
            .append("@OptIn(ExperimentalStdlibApi::class)\n")
            .append("open class ${lexer.lexerClass}(val source: String) : Sequence<${lexer.lexerClass}.${lexer.tokenTypeName}> {\n")
        appendable.append("\tsealed interface SimpleToken {\n")
            .append("\t\tval string: String\n")
            .append("\t\tval length: Int get() = string.length\n")
            .append("\t}\n")
        appendable.append("\tsealed interface StringToken: SimpleToken\n")
        appendable.append("\tsealed interface RegexpToken: SimpleToken {\n")
            .append("\t\tval regexp: Regex\n")
            .append("\t}\n")
        appendable.append("\tsealed interface ${lexer.tokenTypeName} {\n")
            .append("\t\tval position: Int\n")
            .append("\t\tval column: Int\n")
            .append("\t\tval line: Int\n")
        lexer.rules.forEach {
            val value = it.second
            when (value) {
                is Rule.Regexp -> appendable.append("\t\tclass ${it.first}(override val position: Int, override val column: Int, override val line: Int, override val string: String")
                    .append(") : ${lexer.tokenTypeName}, RegexpToken {\n")
                    .append("\t\t\toverride val regexp get() = __${it.first}\n")
                    .append("\t\t}\n")
                is Rule.StrExp -> appendable.append("\t\tclass ${it.first}(override val position: Int, override val column: Int, override val line: Int) : ${lexer.tokenTypeName}, StringToken {\n")
                    .append("\t\t\toverride val string get() = __${it.first}\n")
                    .append("\t\t}\n")
            }
        }
        appendable.append("\t}\n")


        appendable.append("\tfun parse() = TokenIterator(source)\n")
        appendable.append("\toverride fun iterator() = parse()\n")
        appendable.append("\n")
        appendable.append("\tclass TokenIterator(val source: String) : Iterator<${lexer.tokenTypeName}> {\n")


        appendable
            .append("\t\tprivate var position = 0\n")
            .append("\t\tprivate var column = 0\n")
            .append("\t\tprivate var line = 0\n")
            .append("\t\tprivate var value: String = \"\"\n")
            .append("\t\toverride fun hasNext(): Boolean = position < source.length\n")

            .append("\t\tprivate fun move(result:String){\n")
            .append("\t\t\tposition += result.length\n")
            .append("\t\t}\n")


            .append("\t\tprivate fun assertString(str:String):Boolean{\n")
            .append("\t\t\tif (str.length + position > source.length) {\n")
            .append("\t\t\t\treturn false\n")
            .append("\t\t\t}\n")
            .append("\t\t\treturn source.substring(position, position + str.length)==str\n")
            .append("\t\t}\n")


        lexer.rules.forEach {
            val value = it.second
            when (value) {
                is Rule.Regexp -> appendable.append("\t\tprivate fun read${it.first}():${lexer.tokenTypeName}.${it.first}? {\n")
                    .append("\t\t\tval result = __${it.first}.matchAt(source, position)?:return null\n")
                    .append("\t\t\tval ret = ${lexer.tokenTypeName}.${it.first}(position = position, column = column, line = line, string = result.value)\n")
                    .append("\t\t\tmove(result.value)\n")
                    .append("\t\t\treturn ret\n")
                    .append("\t\t}\n")
                is Rule.StrExp -> appendable.append("\t\tprivate fun read${it.first}():${lexer.tokenTypeName}.${it.first}? {\n")
                    .append("\t\t\treturn if (assertString(__${it.first}))\n {")
                    .append("\t\t\t\tval ret = ${lexer.tokenTypeName}.${it.first}(position = position, column = column, line = line)\n")
                    .append("\t\t\t\tmove(__${it.first})\n")
                    .append("\t\t\t\tret\n")
                    .append("\t\t\t} else {\n")
                    .append("\t\t\t\tnull\n")
                    .append("\t\t\t}\n")
                    .append("\t\t}\n")
            }

        }

        appendable.append("\t\toverride fun next(): ${lexer.tokenTypeName} {\n")
            .append("\t\t\tif (!hasNext()) {\n")
            .append("\t\t\t\tthrow NoSuchElementException()\n")
            .append("\t\t\t}\n")
        if (lexer.rules.isEmpty()) {
            appendable.append("\t\t\treturn null\n")
        } else {
            appendable.append("\t\t\treturn ")
            var first = true
            lexer.rules.forEach {
                if (!first) {
                    appendable.append("\t\t\t?: ")
                }
                first = false
                appendable.append("read").append(it.first).append("()\n")
            }
            if (!first) {
                appendable.append("\t\t\t?:")
            }
            appendable.append(" TODO()\n")
        }
        appendable.append("\t\t}\n")
//        appendable.append("\t\t\treturn byRegexp(FUN, Lex.FUN)\n")
//            .append("?: byRegexp(SPACE, Lex.SPACE)\n")
//            .append("?: byRegexp(ID, Lex.ID)\n")
//            .append("?: byString(BS, Lex.BS)\n")
//            .append("?: byString(BE, Lex.BE)\n")
//            .append("?: TODO()\n")
//            .append("}\n")

//        appendable.append("\t\tprivate fun byRegexp(regexp: Regex, m: Lex): Lex? {\n")
//            .append("\t\t\tval result = regexp.matchAt(source, position) ?: return null\n")
//            .append("\t\t\tstart = result.range.first\n")
//            .append("\t\t\tend = result.range.last\n")
//            .append("\t\t\tvalue = result.value\n")
//            .append("\t\t\tposition = result.range.last + 1\n")
//            .append("\t\t\treturn m\n")
//            .append("\t\t}\n")
//        appendable.append("\t\tprivate fun byString(str: String, m: ${lexer.tokenTypeName}): ${lexer.tokenTypeName}? {\n")
//            .append("\t\t\tif (str.length + position > source.length) {\n")
//            .append("\t\t\t\treturn null\n")
//            .append("\t\t\t}\n")
//            .append("\t\t\tif (source.substring(position, position + str.length) != str) {\n")
//            .append("\t\t\t\treturn null\n")
//            .append("\t\t\t}\n")
//            .append("\t\t\tmove(str)\n")
//            .append("\t\t\treturn m\n")
//            .append("\t\t}\n")


        appendable.append("\t}\n")


        appendable.append("}\n")
        lexer.rules.forEach {
            val value = it.second
            when (value) {
                is Rule.Regexp -> appendable.append(
                    "private val __${it.first} = \"${
                        value.regexp.replace(
                            "\\",
                            "\\\\"
                        )
                    }\".toRegex()\n"
                )
                is Rule.StrExp -> appendable.append("private const val __${it.first} = \"${value.string}\"\n")
            }
        }

    }
}