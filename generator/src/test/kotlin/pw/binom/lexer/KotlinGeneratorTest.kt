package pw.binom.lexer

import org.junit.jupiter.api.Test

class KotlinGeneratorTest {

    object B1 : Lexer() {
        val PRIMARY_EXP by exp()
        val EXP by exp()
        val OP_PLUS by string("+")
        val NUMBER by regexp("\\d+")
        init {
            PRIMARY_EXP {
                NUMBER.token.named("number")
            }
            EXP {
                PRIMARY_EXP.named("left") + OP_PLUS.named("operator") + PRIMARY_EXP.named("right")
            }
        }
    }

//    @Test
//    fun genClassExp() {
//        KotlinGenerator(B1, SourceAppender2(System.out))
//            .generateClass(B1.VARIABLE)
//    }

//    @Test
//    fun genReadExp() {
//        val kg = KotlinGenerator(B1, SourceAppender2(System.out))
//        kg.generateRead(B1.ID)
//        kg.generateRead(B1.VARIABLE)
//    }

    @Test
    fun genTest() {
        Generator.generate("", B1, SourceAppender2(System.out))
//        val kg = KotlinGenerator(B1, System.out)
//        kg.generateRead(B1.ID)
//        kg.generateRead(B1.VARIABLE)
    }
}