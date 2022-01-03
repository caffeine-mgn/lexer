package pw.binom.lexer

internal class KotlinGenerator(val lexer: Lexer, val appendable: SourceAppender) {

    private var temparal = 0
    fun generateReadExp2(value: Expression, vars: HashMap<Expression, String>, mainExp: Boolean = false): String {
        val varName: String
        val isNew: Boolean
        val tt = vars[value]
        if (tt != null) {
            varName = tt
            isNew = false
        } else {
            isNew = true
            varName = vars[value] ?: (value.name ?: "t${++temparal}")
        }
        val prefix = if (isNew) {
            "var "
        } else {
            ""
        }
        if (!mainExp && value.propertyName != null) {
            if (value.count == ExpCount.MANY) {
                appendable.ln("$prefix$varName = readList{read${value.propertyName}()}")
            } else {
                appendable.ln("$prefix$varName = read${value.propertyName}()")
            }
            vars[value] = varName
            return varName
        }
        when (value) {
            is Expression.Token -> {
                if (value.count == ExpCount.MANY) {
//                    val tmpVarName = "t${++temparal}"
                    appendable.ln("$prefix$varName = readList{read${value.rule.propertyName}()}")
//                    appendable.ln("$prefix$varName = ArrayList<${value.rule.propertyName}>()")
//                        .ln("while (true) {") {
//                            ln("$varName += read${value.rule.propertyName}()?:break")
//                        }.ln("}")
                } else {
                    appendable.ln("$prefix $varName = read${value.rule.propertyName}() //$value")
                }
            }
            is Expression.Or -> {
                if (isNew) {
                    appendable.ln("$prefix$varName: ${lexer.tokenTypeName}?")
                }
                val left = generateReadExp2(value.left, vars = vars)
                appendable.ln("$varName=$left")
                appendable.ln("if ($varName==null) {") {
                    val right = generateReadExp2(value.right, vars = vars)
                    ln("$varName=$right")
                }.ln("}")
//                appendable.ln("val $varName = xor($left, $right) //$value")
            }
            is Expression.And -> {
                appendable.ln("push()")
                if (isNew) {
                    appendable.ln("$prefix$varName:${lexer.tokenTypeName}? = null")
                }
                val left = generateReadExp2(value.left, vars = vars)
                if (value.left.count != ExpCount.OPTION) {
                    appendable.ln("if ($left!=null) {") {
                        val right = generateReadExp2(value.right, vars = vars)
                        ln("$varName=$right")
                    }.ln("}")
                } else {
                    appendable.ln("$varName=$left")
                    val right = generateReadExp2(value.right, vars = vars)
                    appendable.ln("$varName=$right")
                }
                appendable.ln("if ($varName == null) pop() else skip()")
            }
            is Expression.Root -> generateReadExp2(value = value.exp, mainExp = true, vars = vars)
            else -> TODO()
        }
//        if (value.count == ExpCount.ONE) {
//            appendable.ln("if ($varName == null) return null")
//        }
        vars[value] = varName
        return varName
    }

    fun generateReadExp3(value: Expression.Root, afterRead: List<String>) {
        appendable
            .ln("push()")
            .ln("val position = position")
            .ln("val column = column")
            .ln("val line = line")
        val vars = HashMap<Expression, String>()
        val named = value.getNamed(excludeRoots = true)

        val nonOptionals = HashSet<Expression>()
        nonOptionals += value.exp
        nonOptionals += value.getNamed(excludeRoots = true)
//        value.exp.forEachNonOptional(excludeRoots = true) {
//            nonOptionals += it
//        }
        nonOptionals.forEach {
            val name = it.name ?: "t${++temparal}"
            val baseType =
                if (it.propertyName == null) "Any" else "${lexer.tokenTypeName}.${it.propertyName}"
            when (it.count) {
                ExpCount.ONE,
                ExpCount.OPTION -> appendable.ln("var $name:$baseType? = null")
                ExpCount.MANY -> appendable.ln("var $name:List<$baseType>? = null")
            }

//            appendable.ln("var $name:$type?=null")
            vars[it] = name
        }
        val expRoot = generateReadExp2(value.exp, mainExp = true, vars = vars)
        appendable.ln("if ($expRoot==null){") {
            ln("pop()")
            ln("return null")
        }.ln("}")
//        if (nonOptionals.isNotEmpty()) {
//            appendable.ln("if (${nonOptionals.joinToString(separator = "||") { vars[it]!! + "==null" }}){") {
//                ln("pop()")
//                ln("return null")
//            }.ln("}")
//        }

//        nonOptionals.forEach {
//            val varName = vars[it]!!
//            appendable.ln("if ($varName == null) {pop();return null;}")
//        }

//        named.forEach {
//            appendable.ln("//${it.name} = $it")
//        }
//        named.forEach {
//            if (it.count != ExpCount.OPTION) {
//                appendable.ln("if (${it.name} == null) {pop();return null}")
//            }
//        }

        appendable
            .ln("skip()")
        afterRead.forEach {
            appendable.t(it)
        }
        appendable.ln("return ${lexer.tokenTypeName}.${value.propertyName}(") {
            ln("position = position,")
            ln("column = column,")
            ln("line = line,")
            ln("length = this.position - position,")
            ln("source = source,")
            named.forEach {
                ln("${it.name} = ${it.name},")
            }
        }
        appendable.ln(")")
    }

    fun generateReadExp(value: Rule.Exp, level: Int = 0) {
        val exp = value.exp

        appendable
            .padding(level) {
                ln("private fun read${value.propertyName}():${lexer.tokenTypeName}.${value.propertyName}? {")
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
                    ln("private fun read${value.propertyName}():${lexer.tokenTypeName}.${value.propertyName}? {") {
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
                    ln("private fun read${value.propertyName}():${lexer.tokenTypeName}.${value.propertyName}? {") {
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

    fun generateClass(rule: Rule<*>, level: Int = 0) {
        when (rule) {
            is Rule.Regexp -> appendable.padding(level) {
                ln("class ${rule.propertyName}(") {
                    ln("override val position: Int,")
                    ln("override val column: Int,")
                    ln("override val line: Int,")
                    ln("override val body: String,")
                }
                ln(") : ${lexer.tokenTypeName}, RegexpToken {") {
                    ln("override val regexp get() = __${rule.propertyName}")
                }
                ln("}")
            }
            is Rule.StrExp -> appendable.padding(level) {
                ln("class ${rule.propertyName}(") {
                    ln("override val position: Int,")
                    ln("override val column: Int,")
                    ln("override val line: Int,")
                }
                ln(") : ${lexer.tokenTypeName}, StringToken {") {
                    ln("override val string get() = __${rule.propertyName}")
                    ln("override val body get() = __${rule.propertyName}")
                    ln("override val length get() = ${rule.string.length}")
                }
                ln("}")
            }
            is Rule.Exp -> {
                val named = rule.exp.getNamed(excludeRoots = true)
                appendable.padding(level) {
                    ln("class ${rule.propertyName}(") {
                        ln("override val position: Int,")
                        ln("override val column: Int,")
                        ln("override val line: Int,")
                        ln("override val length: Int,")
                        ln("private val source: String,")

                        named.forEach {
                            val text = when (it.count) {
                                ExpCount.MANY -> "List<${it.propertyName ?: lexer.tokenTypeName}>"
                                ExpCount.OPTION -> "${it.propertyName ?: lexer.tokenTypeName}?"
                                ExpCount.ONE -> it.propertyName ?: lexer.tokenTypeName
                            }
                            ln("val ${it.name}: $text,")
                        }
                    }.ln(") : ${lexer.tokenTypeName} {") {
                        ln("override val body get() = source.substring(position, position + length)")
                    }.ln("}")
                }
            }
        }
    }

    fun generate() {

    }
}