import com.t_mukai.mint.buildlogic.GenerateUnicodeRanges

plugins {
  id("convention.kotlin-multiplatform")
  id("convention.kotest")
  id("convention.detekt")
}

val ucdDirectory = layout.projectDirectory.dir("ucd")

val generateUnicodeRanges by tasks.registering(GenerateUnicodeRanges::class) {
  sources.from(
    ucdDirectory.file("DerivedCoreProperties.txt"),
  )
  ranges =
    mapOf(
      "xidStartRanges" to "XID_Start",
      "xidContinueRanges" to "XID_Continue",
      "defaultIgnorableRanges" to "Default_Ignorable_Code_Point",
    )
  packageName = "com.t_mukai.mint.unicode"
  outputDirectory = layout.buildDirectory.dir("generated/unicode")
}

kotlin {
  jvm()

  sourceSets {
    commonMain {
      kotlin.srcDir(generateUnicodeRanges)
    }
  }
}

tasks.named<Test>("jvmTest") {
  inputs.dir(ucdDirectory).withPathSensitivity(PathSensitivity.RELATIVE)
  systemProperty("ucd.dir", ucdDirectory.asFile.absolutePath)
}
