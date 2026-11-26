plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
  explicitApi()
  compilerOptions.allWarningsAsErrors = true
  compilerOptions.freeCompilerArgs.addAll(
    "-Xexplicit-context-arguments",
    "-Xreturn-value-checker=full",
    "-Xcollection-literals",
    "-Xname-based-destructuring=complete",
    "-Xcompanion-blocks-and-extensions",
    "-Xconsistent-data-class-copy-visibility",
    "-Xexpect-actual-classes",
    "-Xcontext-sensitive-resolution",
    "-Xfull-value-classes",
  )
  compilerOptions.optIn.addAll(
    "kotlin.contracts.ExperimentalContracts",
    "kotlin.text.unicode.ExperimentalUnicodeApi",
  )
  jvm()
}

repositories {
  mavenCentral()
}

dependencyLocking {
  lockAllConfigurations()
}
