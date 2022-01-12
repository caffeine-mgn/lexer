package pw.binom.lexer

import org.junit.jupiter.api.Test

class KotlinGeneratorTest {

    object B1 : Lexer() {
        val PRIMARY_EXP by exp {
            NUMBER.token.named("number")
        }
        val O2 by exp {
            PRIMARY_EXP.many.named("exp").onGone("?:TODO()")// or NUMBER.named("number")
        }

        //        val EXP by exp {
//            PRIMARY_EXP.named("left") + OP_PLUS.named("operator")// + PRIMARY_EXP.named("right")
//        }
//
//        val OP_PLUS by string("+")
        val NUMBER by regexp("\\d+")
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