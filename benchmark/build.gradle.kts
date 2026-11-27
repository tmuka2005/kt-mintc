plugins {
  id("convention.benchmark")
  id("convention.detekt")
}

benchmark {
  targets {
    register("jvm")
  }
  configurations {
    named("main") {
      warmups = 5
      iterations = 5
      iterationTime = 1
      iterationTimeUnit = "s"
    }
  }
}
