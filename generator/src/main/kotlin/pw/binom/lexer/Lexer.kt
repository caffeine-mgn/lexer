package pw.binom.lexer

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Lexer {
    abstract val packageName: String
    abstract val lexerClass: String
    abstract val tokenTypeName: String
    internal val rules = ArrayList<Pair<String, Rule<out Rule<*>>>>()
    protected fun regexp(regexp: Regex) = regexp(regexp = regexp.pattern)
    protected fun regexp(regexp: String): RuleProvider<Rule.Regexp> = RuleProvider(this, Rule.Regexp(regexp = regexp))

    protected fun string(string: String) = RuleProvider(this, Rule.StrExp(string = string))
}

sealed interface Rule<T> : ReadOnlyProperty<Lexer, Rule<T>> {
    class Regexp(val regexp: String) : Rule<Regexp> {
        override fun getValue(thisRef: Lexer, property: KProperty<*>) = this
    }

    class StrExp(val string: String) : Rule<StrExp> {
        override fun getValue(thisRef: Lexer, property: KProperty<*>) = this
    }
}

class RuleProvider<T : Rule<T>>(val lexer: Lexer, val value: T) : PropertyDelegateProvider<Lexer, T> {
    override fun provideDelegate(thisRef: Lexer, property: KProperty<*>): T {
        lexer.rules += property.name to value
        return value
    }
}

