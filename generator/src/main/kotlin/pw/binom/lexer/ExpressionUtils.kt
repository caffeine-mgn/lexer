package pw.binom.lexer

internal fun Expression.forEach(excludeRoots: Boolean, func: (Expression) -> Unit) {
    val viseted = HashSet<Expression>()
    fun r(e: Expression) {
        if (e in viseted) {
            return
        }
        func(e)
        viseted += e
        when (e) {
            is Expression.Two -> {
                r(e.left)
                r(e.right)
            }
            is Expression.Root -> if (!excludeRoots) r(e.exp)
        }
    }
    r(this)
}

internal fun Expression.forEachNonOptional(excludeRoots: Boolean, func: (Expression) -> Unit) {
    val viseted = HashSet<Expression>()
    fun r(e: Expression) {
        if (e in viseted) {
            return
        }
        if (e.count == ExpCount.OPTION) {
            return
        }

        func(e)
        viseted += e
//        if (excludeRoots && e.propertyName != null) {
//            return
//        }
        when (e) {
            is Expression.Two -> {
                r(e.left)
                r(e.right)
            }
            is Expression.Root -> if (!excludeRoots) r(e.exp)
        }
    }
    r(this)
}

internal fun Expression.Root.getNamed(excludeRoots: Boolean): ArrayList<Expression> {
    val expressions = ArrayList<Expression>()
    exp.forEach(excludeRoots = excludeRoots) {
        if (it.name != null) {
            expressions += it
        }
    }
    return expressions
}

operator fun Rule.Regexp.plus(other: String): String = this.regexp + other
operator fun Rule<*>.plus(other: Expression): Expression = this.token + other
operator fun Expression.plus(other: Expression): Expression.And = Expression.And(this, other)

infix fun Rule<*>.and(other: Expression): Expression.And = this.token and other
infix fun Expression.and(space: Rule<*>): Expression.And = this and space.token
infix fun Rule<*>.and(rule: Rule<*>): Expression.And = this.token and rule.token
infix fun Expression.and(token: Expression): Expression.And = Expression.And(left = this, right = token)

infix fun Rule<*>.or(rule: Rule<*>): Expression = this.token or rule.token
infix fun Rule<*>.or(other: Expression): Expression = token or other
infix fun Expression.or(other: Rule<*>) = this or other.token

infix fun Expression.or(other: Expression): Expression {
    return Expression.Or(left = this, right = other)
}

operator fun Expression.plus(token: Rule<*>): Expression.And =
    Expression.And(left = this, right = token.token)

operator fun Rule<*>.plus(token: Rule<*>): Expression = this.token + token.token

val Rule<*>.optional
    get() = this.token.optional as Expression.Named

val Rule<*>.many
    get() = this.token.copy(count = ExpCount.MANY) as Expression.Named

val <T : Expression>T.optional
    get() = copy(count = ExpCount.OPTION)