package pw.binom.lexer

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Lexer {
    open val packageName: String
        get() = "pw.binom.lexer"
    open val lexerClass: String
        get() = "Lexer"
    open val tokenTypeName: String
        get() = "Token"
    internal val inits = ArrayList<String>()
    internal val rules = HashMap<String, Rule<out Rule<*>>>()

    private val order = ArrayList<Rule<out Rule<*>>>()
    protected var rootRules: List<Rule<*>> = order

    internal fun sortRooRules(): List<Pair<String, Rule<*>>> {

        val rules = rules.entries.associate { it.value to it.key }
        return rootRules.map {
            rules[it]!! to it
        }

        if (rootRules is List<Rule<*>>) {

        }

        val out = ArrayList<Pair<String, Rule<*>>>()
        order.forEach {
            out += rules[it]!! to it
        }
        return out
    }

    protected fun regexp(regexp: Regex) = regexp(regexp = regexp.pattern)
    protected fun regexp(regexp: String): RuleProvider<Rule.Regexp> {
        val r = Rule.Regexp(regexp = regexp)
        order += r
        return RuleProvider(this, r)
    }

    protected fun exp(func: Rule.Exp.() -> Expression): RuleProvider<Rule.Exp> {
        val r = exp()
        r.value.exp.reset {
            r.value.func()
        }
        return r
    }

    protected fun string(string: String): RuleProvider<Rule.StrExp> {
        val r = Rule.StrExp(string = string)
        order += r
        return RuleProvider(this, r)
    }

    protected fun exp(exp: Expression): RuleProvider<Rule.Exp> {
        exp.root = true
        val r = Rule.Exp(exp = Expression.Root({ exp }, exp))
        order += r
        return RuleProvider(this, r)
    }

    protected fun exp(): RuleProvider<Rule.Exp> {
        val r = Rule.Exp(exp = Expression.Root({ throw IllegalStateException("Expression not set") }, null))
        order += r
        return RuleProvider(
            this,
            r
        )
    }


}

operator fun Rule.Exp.invoke(func: () -> Expression) {
    this.exp.reset(func)
}

enum class ExpCount {
    OPTION,
    ONE,
    MANY,
}


fun <T : Rule<*>> T.named(name: String): Expression.Named = this.token.named(name)
fun <T : Expression.Named> T.named(name: String) =
    copy(name = name, count = count) as Expression.Named


val Rule<*>.token: Expression.Named
    get() = when (this) {
        is Rule.Exp -> exp
        else -> Expression.Token(this).also { it.propertyName = propertyName }
    }

sealed interface Rule<T> : ReadOnlyProperty<Lexer, Rule<T>> {
    var propertyName: String
    val preRead: ArrayList<String>
    val afterRead: ArrayList<String>
    val extends: ArrayList<String>
    val code: ArrayList<String>

    class Regexp internal constructor(val regexp: String) : Rule<Regexp> {
        override fun getValue(thisRef: Lexer, property: KProperty<*>) = this
        override lateinit var propertyName: String
        private val c = regexp
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t")

        override fun toString(): String = propertyName.ifEmpty { "Regexp(\"$c\")" }
        override val preRead = ArrayList<String>()
        override val afterRead = ArrayList<String>()
        override val extends = ArrayList<String>()
        override val code = ArrayList<String>()
    }

    class StrExp internal constructor(val string: String) : Rule<StrExp> {
        override lateinit var propertyName: String
        override val preRead = ArrayList<String>()
        override fun getValue(thisRef: Lexer, property: KProperty<*>) = this
        override fun toString(): String = propertyName.ifEmpty {"StrExp($string)"}
        override val afterRead = ArrayList<String>()
        override val extends = ArrayList<String>()
        override val code = ArrayList<String>()
    }

    class Exp internal constructor(val exp: Expression.Root) : Rule<Exp> {
        override lateinit var propertyName: String
        override val preRead = ArrayList<String>()
        override fun getValue(thisRef: Lexer, property: KProperty<*>) = this
        override fun toString(): String = propertyName.ifEmpty {"Exp($exp)"}
        override val afterRead = ArrayList<String>()
        override val extends = ArrayList<String>()
        override val code = ArrayList<String>()
    }
}

fun <T : Rule<*>> T.beforeRead(func: () -> String) = beforeRead(func())
fun <T : Rule<*>> T.beforeRead(code: String): T {
    preRead += code
    return this
}

fun <T : Rule<*>> T.afterRead(func: () -> String) = beforeRead(func())
fun <T : Rule<*>> T.afterRead(code: String): T {
    afterRead += code
    return this
}

fun <T : Lexer> T.init(code: String): T {
    inits += code
    return this
}

class RuleProvider<T : Rule<T>>(val lexer: Lexer, val value: T) : PropertyDelegateProvider<Lexer, T> {
    override fun provideDelegate(thisRef: Lexer, property: KProperty<*>): T {

        value.propertyName = property.name
        if (value is Rule.Exp) {
            value.exp.propertyName = property.name
            lexer.rules[property.name] = value
        } else {
            lexer.rules += property.name to value
        }
        return value
    }
}

fun <T : RuleProvider<*>> T.beforeRead(code: String): T {
    value.beforeRead(code)
    return this
}

fun <T : RuleProvider<*>> T.beforeRead(code: () -> String): T {
    value.beforeRead(code)
    return this
}

fun <T : RuleProvider<*>> T.afterRead(code: String): T {
    value.afterRead(code)
    return this
}

fun <T : RuleProvider<*>> T.afterRead(code: () -> String): T {
    value.afterRead(code)
    return this
}