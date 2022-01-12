package pw.binom.lexer

internal class KotlinGenerator(val lexer: Lexer, val appendable: SourceAppender) {
    interface GeneratedExp {
        val text: String

        class Exp(override val text: String) : GeneratedExp
        class CodeAndExp(val code: String, override val text: String) : GeneratedExp
        companion object {
            fun make(sb: StringBuilder, exp: String): GeneratedExp =
                when {
                    sb.isEmpty() -> Exp(exp)
                    else -> CodeAndExp(code = sb.toString(), text = exp)
                }
        }
    }

    private fun <T : Appendable> T.apply(exp: GeneratedExp): T {
        if (exp is GeneratedExp.CodeAndExp) {
            appendLine(exp.code.trimEnd())
        }
        return this
    }

    private var temparal = 0
    fun generateReadExp2(
        value: Expression,
        req: Set<Expression>,
        vars: HashMap<Expression, String>,
        mainExp: Boolean = false
    ): GeneratedExp {
        if (!mainExp && value.propertyName != null) {
            val sb = StringBuilder()
            val exp = when {
                value.count == ExpCount.MANY -> "readList{read${value.propertyName}()}"
//                value in req -> "read${value.propertyName}()?:r(s0){return null}"
                else -> "read${value.propertyName}()"
            }
            return if (value.name != null) {
                sb.appendLine("${value.name} = $exp")
                GeneratedExp.make(sb, value.name!!)
            } else {
//                val varName = "t${++temparal}"
//                sb.append("val $varName = $exp")
                GeneratedExp.make(sb, exp)
            }
        }
        when (value) {
            is Expression.Token -> {
                val sb = StringBuilder()
                val exp = when {
                    value.count == ExpCount.MANY -> "readList{read${value.rule.propertyName}()}"
//                    value in req -> "read${value.rule.propertyName}()?:r(s0){return null}"
                    else -> "read${value.rule.propertyName}()"
                }
                if (value.name != null) {
                    sb.appendLine("${value.name} = $exp")
                    return GeneratedExp.make(sb, value.name!!)
                }
//                val varName = "t${++temparal}"
//                sb.append("val $varName = $exp")
                return GeneratedExp.make(sb, exp)
            }
            is Expression.Or -> {
                val left = generateReadExp2(value.left, req = req, vars = vars)
                val right = generateReadExp2(value.right, req = req, vars = vars)
                val sb = StringBuilder()
                sb.appendLine("or({ //left: ${value.left}")
                sb.apply(left)
                sb.append(left.text)
                if (value.left.onGone != null) {
                    sb.append(value.left.onGone)
                }
                sb.appendLine("},{ //right: ${value.right}")
                sb.apply(right)
                sb.append(right.text)
                if (value.right.onGone != null) {
                    sb.append(value.right.onGone)
                }
                sb.append("})")
                return GeneratedExp.Exp(sb.toString())


                val varName = "t${++temparal}"

                val funcName = "or${++temparal}"
                sb.append("fun $funcName():Any?{")
                sb.apply(left)
                sb.appendLine("val l = ${left.text}")
                sb.appendLine("if (l!=null){return l}")
                sb.apply(right)
                sb.appendLine("return ${right.text}")
                sb.appendLine("}")
                return GeneratedExp.make(sb, "$funcName()")

                if (left is GeneratedExp.CodeAndExp) {
                    sb.appendLine(left.code)
                }
                sb.appendLine("var $varName : Any? = ${left.text}")
                    .appendLine("if ($varName==null){")
                if (right is GeneratedExp.CodeAndExp) {
                    sb.appendLine(right.code)
                }
                sb.appendLine("$varName = ${right.text}")
                sb.appendLine("}")
                return GeneratedExp.make(sb, varName)
            }
            is Expression.And -> {
                val left = generateReadExp2(value.left, req = req, vars = vars)
                val right = generateReadExp2(value.right, req = req, vars = vars)
                val sb = StringBuilder()
                sb.appendLine("and({ //left: ${value.left}")
                sb.apply(left)
                sb.append(left.text)
                if (value.left.onGone != null) {
                    sb.append(value.left.onGone)
                } else {
                    if (value.left.count == ExpCount.OPTION) {
                        sb.append("?:${lexer.tokenTypeName}.Exist")
                    }
                }
                sb.appendLine("},{ //right: ${value.right}")
                sb.apply(right)
                sb.append(right.text)
                if (value.right.onGone != null) {
                    sb.append(value.right.onGone)
                } else {
                    if (value.right.count == ExpCount.OPTION) {
                        sb.append("?:${lexer.tokenTypeName}.Exist")
                    }
                }
                sb.append("})")
                return GeneratedExp.Exp(sb.toString())


                val funcName = "and${++temparal}"
                sb.append("fun $funcName():Any?{")
                sb.appendLine("val s1 = makeState()")
                sb.apply(left)
                sb.append("val l = ${left.text}")
                if (value.left.count != ExpCount.OPTION) {
                    sb.append("?:return null")
                }
                sb.appendLine()
                sb.apply(right)
                sb.append("val r = ${right.text}")
                if (value.right.count != ExpCount.OPTION) {
                    sb.append("?:r(s1){return null}")
                }
                sb.appendLine()
                sb.appendLine("return l?:r")
                sb.appendLine("}")
                return GeneratedExp.make(sb, "$funcName()")


                sb.apply(left)
                sb.appendLine(left.text)
                sb.apply(right)
                val varName = "t${++temparal}"
                sb.appendLine("val $varName=${right.text}?:${left.text}")
                val e = if (value.count != ExpCount.OPTION) {
                    "?:r(s0){return null}"
                } else ""
                return GeneratedExp.make(sb, varName + e)


                sb.apply(left)
                if (value.left in req) {
                    if (value.left.name != null) {
                        sb.append("${value.left.name}=")
                    }
                    sb.appendLine("${left.text}?:r(s0){return null}")
                    if (value.right in req) {
                        sb.apply(right)
                        if (value.right.name != null) {
                            sb.append("${value.right.name}=")
                        }
                        sb.appendLine("${right.text}?:r(s0){return null}")
                        sb.append("//---------------4")
                        return GeneratedExp.make(sb, "${lexer.tokenTypeName}.Exist")
                    } else {
                        sb.apply(right)
                        if (value.right.name != null) {
                            sb.append("${value.right.name}=")
                        }
                        sb.appendLine(right.text)
                        sb.append("//---------------3")
                        return GeneratedExp.make(sb, "${left.text}?:${right.text}")
                    }
                } else {
                    sb.appendLine(left.text)
                    if (value.right in req) {
                        sb.apply(right)
                        if (value.right.name != null) {
                            sb.append("${value.right.name}=${right.text}")
                        }
                        sb.appendLine("${right.text}?:r(s0){return null}")
                        sb.append("//---------------2")
                        return GeneratedExp.make(sb, right.text)
                    } else {
                        sb.apply(right)
                        sb.appendLine(right.text)
                        sb.append("//---------------1")
                        return GeneratedExp.make(sb, "${left.text}?:${right.text}")
                    }
                }
            }
            is Expression.Root -> return generateReadExp2(value = value.exp, req = req, mainExp = false, vars = vars)
            else -> TODO()
        }
        TODO()
    }

    fun generateReadExp3(value: Expression.Root, afterRead: List<String>) {
        appendable
            .ln("val s0 = makeState()")
        val vars = HashMap<Expression, String>()
        val named = value.getNamed(excludeRoots = true)

        val nonOptionals = HashSet<Expression>()
        nonOptionals += value.exp
        nonOptionals += value.getNamed(excludeRoots = true)
        val nonOptionals2 = HashSet<Expression>()
        value.exp.forEachNonOptional(excludeRoots = true) {
            nonOptionals2 += it
        }
        nonOptionals.forEach {
            val name = it.name ?: "t${++temparal}"
            val baseType =
                if (it.propertyName == null) "Any" else "${lexer.tokenTypeName}.${it.propertyName}"
            when (it.count) {
                ExpCount.ONE,
                ExpCount.OPTION -> appendable.ln("var $name:$baseType? = null")
                ExpCount.MANY -> appendable.ln("var $name:List<$baseType>? = null")
            }
            vars[it] = name
        }
        val expRoot = generateReadExp2(value.exp, mainExp = false, vars = vars, req = nonOptionals2)
        if (expRoot is GeneratedExp.CodeAndExp) {
            appendable.ln(expRoot.code)
        }
        if (value.exp.onGone != null) {
            appendable.ln("${expRoot.text}${value.exp.onGone}?:r(s0){return null}")
        } else {
            appendable.ln("${expRoot.text}?:r(s0){return null}")
        }
//        appendable.ln("if (${expRoot.text} == null) {") {
//            ln("setState(s0)")
//            ln("return null")
//        }.ln("}")

        afterRead.forEach {
            appendable.t(it)
        }
        appendable.ln("return ${lexer.tokenTypeName}.${value.propertyName}(") {
            ln("position = s0.position,")
            ln("column = s0.column,")
            ln("line = s0.line,")
            ln("length = this.position - s0.position,")
            ln("source = source,")
            named.forEach {
                val e = if (it in nonOptionals2) "" else ""
                ln("${it.name} = ${it.name}$e,")
            }
        }
        appendable.ln(")")
    }

    fun generateReadExp(value: Rule.Exp, level: Int = 0) {
        val exp = value.exp

        appendable
            .padding(level) {
                ln("fun read${value.propertyName}():${lexer.tokenTypeName}.${value.propertyName}? {")
                value.preRead.forEach {
                    appendable.t(it)
                }
                appendable.t("\n")
                generateReadExp3(exp, afterRead = value.afterRead)
                ln("}")
            }
    }

    fun generateRead(value: Rule<*>, level: Int = 0) {
        when (value) {
            is Rule.Regexp -> appendable
                .padding(level) {
                    ln("fun read${value.propertyName}():${lexer.tokenTypeName}.${value.propertyName}? {") {
                        value.preRead.forEach {
                            appendable.t(it)
                        }
                        ln("val result = __${value.propertyName}.matchAt(source, position)?:return null")
                        ln("val ret = ${lexer.tokenTypeName}.${value.propertyName}(position = position, column = column, line = line, body = result.value)")
                        ln("move(result.value)")
                        value.afterRead.forEach {
                            appendable.t(it)
                        }
                        ln("return ret")
                    }
                    ln("}")
                }
            is Rule.StrExp -> appendable
                .padding(level) {
                    ln("fun read${value.propertyName}():${lexer.tokenTypeName}.${value.propertyName}? {") {
                        value.preRead.forEach {
                            appendable.t(it)
                        }
                        ln("return if (assertString(__${value.propertyName})) {") {
                            ln("val ret = ${lexer.tokenTypeName}.${value.propertyName}(position = position, column = column, line = line)")
                            ln("move(__${value.propertyName})")
                            value.afterRead.forEach {
                                appendable.t(it)
                            }
                            ln("ret")
                        }
                        ln("} else {") {
                            ln("null")
                        }
                        ln("}")
                    }
                    ln("}")
                }
            is Rule.Exp -> generateReadExp(level = level, value = value)
        }
    }

    fun generateUtils() {
        appendable.ln("private inline fun and(l: () -> Any?, r: () -> Any?): Any? {") {
            ln("val m = makeState()")
            ln("val left = l()")
            ln("if (left != null) {") {
                ln("if (r() == null) {") {
                    ln("setState(m)")
                    ln("return null")
                }.ln("}")
                ln("return Token.Exist")
            }.ln("}")
            ln("return null")
        }.ln("}")
        appendable.ln("private inline fun or(l: () -> Any?, r: () -> Any?): Any? = l() ?: r()")
    }

    fun generateClass(rule: Rule<*>, level: Int = 0) {
        val extends = rule.extends.joinToString(separator = ", ").let { if (it.isNotEmpty()) ",$it" else it }
        when (rule) {
            is Rule.Regexp -> appendable.padding(level) {
                ln("class ${rule.propertyName}(") {
                    ln("override val position: Int,")
                    ln("override val column: Int,")
                    ln("override val line: Int,")
                    ln("override val body: String,")
                }
                ln(") : ${lexer.tokenTypeName}, RegexpToken$extends {") {
                    ln("override val regexp get() = __${rule.propertyName}")
                    rule.code.forEach {
                        ln(it.trimEnd())
                    }
                }
                ln("}")
            }
            is Rule.StrExp -> appendable.padding(level) {
                ln("class ${rule.propertyName}(") {
                    ln("override val position: Int,")
                    ln("override val column: Int,")
                    ln("override val line: Int,")
                }
                ln(") : ${lexer.tokenTypeName}, StringToken$extends {") {
                    ln("override val string get() = __${rule.propertyName}")
                    ln("override val body get() = __${rule.propertyName}")
                    ln("override val length get() = ${rule.string.length}")
                    rule.code.forEach {
                        ln(it.trimEnd())
                    }
                }
                ln("}")
            }
            is Rule.Exp -> {
                val named = rule.exp.getNamed(excludeRoots = true)

                val nonOptionals2 = HashSet<Expression>()
                rule.exp.forEachNonOptional(excludeRoots = true) {
                    nonOptionals2 += it
                }

                appendable.padding(level) {
                    ln("class ${rule.propertyName}(") {
                        ln("override val position: Int,")
                        ln("override val column: Int,")
                        ln("override val line: Int,")
                        ln("override val length: Int,")
                        ln("private val source: String,")

                        named.forEach {
                            val e = if (it in nonOptionals2) {
                                ""
                            } else {
                                "?"
                            }
                            val text = when {
                                it.count == ExpCount.MANY -> "List<${it.propertyName ?: lexer.tokenTypeName}>"
                                else -> it.propertyName ?: lexer.tokenTypeName
                            }
                            ln("val ${it.name}: $text$e,")
                        }
                    }.ln(") : ${lexer.tokenTypeName}$extends {") {
                        ln("override val body get() = source.substring(position, position + length)")
                        rule.code.forEach {
                            ln(it.trimEnd())
                        }
                    }.ln("}")
                }
            }
        }
    }

    fun generate() {

    }
}