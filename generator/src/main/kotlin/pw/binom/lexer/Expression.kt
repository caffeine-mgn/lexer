package pw.binom.lexer

sealed interface Expression {
    var name: String?
    var parent: Expression?
    var root: Boolean
    var count: ExpCount
    var propertyName: String?
    var onGone: String?
    fun copy(name: String? = null, count: ExpCount? = null, throwOnGone: String? = null): Expression

    interface Named : Expression
    class Root(private var body: () -> Expression, private var body2: Expression?) : Expression, Named {
        override var name: String? = null
        override var parent: Expression? = null
        override var root: Boolean = true
        override var count: ExpCount = ExpCount.ONE
        override var propertyName: String? = null
        override var onGone: String? = null
        override fun toString(): String = propertyName?:exp.toString()
        fun reset(f: () -> Expression) {
            body = f
            body2 = null
        }

        val exp: Expression
            get() {
                if (body2 == null) {
                    body2 = body()
                }
                return body2!!
            }

        override fun copy(name: String?, count: ExpCount?, throwOnGone: String?): Expression =
            Root(
                body = body,
                body2 = body2,
            ).also {
                it.name = name ?: this.name
                it.parent = parent
                it.root = root
                it.count = count ?: this.count
                it.propertyName = propertyName
                it.onGone=throwOnGone?:this.onGone
            }

    }

    sealed interface Two : Expression {
        val left: Expression
        val right: Expression

    }

    class Token(val rule: Rule<*>) : Expression, Named {
        override var name: String? = null
        override var parent: Expression? = null
        override var root: Boolean = false
        override var count: ExpCount = ExpCount.ONE
        override var propertyName: String? = null
        override var onGone: String? = null
        override fun toString(): String = rule.toString()
        override fun copy(name: String?, count: ExpCount?, throwOnGone: String?) =
            Token(
                rule = rule
            ).also {
                it.name = name ?: this.name
                it.parent = parent
                it.root = root
                it.count = count ?: this.count
                it.propertyName = propertyName
                it.onGone=throwOnGone?:this.onGone
            }
    }

    class And(override val left: Expression, override val right: Expression) : Expression, Two {
        override var name: String? = null
        override var parent: Expression? = null
        override var root: Boolean = false
        override var count: ExpCount = ExpCount.ONE
        override var propertyName: String? = null
        override var onGone: String? = null
        override fun toString(): String = "($left and $right)"

        override fun copy(name: String?, count: ExpCount?, throwOnGone: String?) =
            And(
                left = left,
                right = right,
            ).also {
                it.name = name ?: this.name
                it.parent = parent
                it.root = root
                it.count = count ?: this.count
                it.propertyName = propertyName
                it.onGone=throwOnGone?:this.onGone
            }
    }

    class Or(override val left: Expression, override val right: Expression) : Expression, Two {
        override var name: String? = null
        override var parent: Expression? = null
        override var root: Boolean = false
        override var count: ExpCount = ExpCount.ONE
        override var propertyName: String? = null
        override var onGone: String? = null
        override fun toString(): String = "($left or $right)"

        override fun copy(name: String?, count: ExpCount?, onGone: String?) =
            Or(
                left = left,
                right = right,
            ).also {
                it.name = name ?: this.name
                it.parent = parent
                it.root = root
                it.count = count ?: this.count
                it.propertyName = propertyName
                it.onGone=onGone?:this.onGone
            }
    }
}