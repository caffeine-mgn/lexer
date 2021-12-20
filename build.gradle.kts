allprojects {
    group = "pw.binom.lexer"
    version = pw.binom.Versions.LIB_VERSION

    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://repo.binom.pw")
    }
}