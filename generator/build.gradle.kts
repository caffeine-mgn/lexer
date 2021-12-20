plugins {
    kotlin("jvm")
}
apply<pw.binom.plugins.BinomPublishPlugin>()
dependencies {
    api(kotlin("stdlib"))
}

apply<pw.binom.plugins.DocsPlugin>()