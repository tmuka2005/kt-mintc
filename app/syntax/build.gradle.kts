plugins {
  id("convention.kotlin-multiplatform")
  id("convention.kotest")
  id("convention.detekt")
}

kotlin {
  jvm()

  sourceSets {
    commonMain {
      dependencies {
        api(project(":libs:parser"))
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotest.assertions.arrow)
      }
    }
  }
}
