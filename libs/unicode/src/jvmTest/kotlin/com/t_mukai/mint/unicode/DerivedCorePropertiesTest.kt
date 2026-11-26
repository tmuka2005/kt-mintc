package com.t_mukai.mint.unicode

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

@Suppress("unused")
class DerivedCorePropertiesTest :
  FunSpec({
    val properties = loadProperties("DerivedCoreProperties.txt")

    test("isXidStart matches XID_Start") {
      mismatches(properties.getValue("XID_Start")) {
        it.isXidStart()
      }.shouldBeEmpty()
    }

    test("isXidContinue matches XID_Continue") {
      mismatches(properties.getValue("XID_Continue")) {
        it.isXidContinue()
      }.shouldBeEmpty()
    }

    test("isDefaultIgnorable matches Default_Ignorable_Code_Point") {
      mismatches(properties.getValue("Default_Ignorable_Code_Point")) {
        it.isDefaultIgnorable()
      }.shouldBeEmpty()
    }

    test("conjunctBreakOf matches InCB") {
      for (conjunct in ConjunctBreak.entries - ConjunctBreak.None) {
        mismatches(properties.getValue("InCB=${conjunct.name}")) {
          conjunctBreakOf(it) == conjunct
        }.shouldBeEmpty()
      }
    }
  })
