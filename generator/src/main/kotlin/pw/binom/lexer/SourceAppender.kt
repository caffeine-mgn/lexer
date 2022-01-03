package pw.binom.lexer

interface SourceAppender {
    fun ln(text: String): SourceAppender
    fun ln(text: String, func: SourceAppender.() -> Unit): SourceAppender
    fun t(text: String): SourceAppender
    fun padding(count: Int = 1, func: SourceAppender.() -> Unit): SourceAppender
    operator fun invoke(func: SourceAppender.() -> Unit): SourceAppender
    fun pad(count: Int): SourceAppender
}

class SourceAppender2(val o: Appendable) : SourceAppender {
    private var count = 0
    private fun <T : Appendable> T.pad(count: Int): T {
        repeat(count) {
            append("  ")
        }
        return this
    }

    override fun ln(text: String): SourceAppender {
        o.pad(count)
        o.append(text).append('\n')
        return this
    }

    override fun ln(text: String, func: SourceAppender.() -> Unit): SourceAppender {
        o.pad(count)
        o.append(text).append('\n')
        val oldLevel = count
        try {
            count++
            func()
        } finally {
            count = oldLevel
        }
        return this
    }

    override fun t(text: String): SourceAppender {
        o.append(text)
        return this
    }

    override fun padding(count: Int, func: (SourceAppender) -> Unit): SourceAppender {
        val oldLevel = this.count
        try {
            this.count += count
            func(this)
        } finally {
            this.count = oldLevel
        }
        return this
    }

    override fun invoke(func: (SourceAppender) -> Unit): SourceAppender =
        padding(count = 1, func = func)

    override fun pad(count: Int): SourceAppender {
        o.pad(count)
        return this
    }

}