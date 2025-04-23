package com.borgnetzwerk.searchsnail.utils.sparqlqb

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.platform.commons.annotation.Testable

@Testable
class BasicGraphPatternTest : DescribeSpec({
    describe("Basic graph pattern") {
        it("Set 1") {
            val WD = Namespace("wd", "http://www.example.org/entity")
            val WDT = Namespace("wdt", "http://www.example.org/entity/direct")
            val country = Var("country")

            BasicGraphPattern(WD("Q458"), WDT("P150"), country).toString() shouldBe
                    """
                wd:Q458 wdt:P150 ?country .
            """.trimIndent()
        }

        it("Set 2") {
            val WD = Namespace("wd", "http://www.example.org/entity")
            val WDT = Namespace("wdt", "http://www.example.org/entity/direct")
            val country = Var("country")
            val city = Var("city")

            BasicGraphPattern(city, WDT("P17"), country)
                .add(WDT("P31"), WD("Q1637706"))
                .toString() shouldBe
                    """
                ?city wdt:P17 ?country ;
                    wdt:P31 wd:Q1637706 .
            """.trimIndent()
        }

        it("Set 3") {
            val WD = Namespace("wd", "http://www.example.org/entity")
            val WDT = Namespace("wdt", "http://www.example.org/entity/direct")
            val country = Var("country")
            val city = Var("city")

            BasicGraphPattern(city, WDT("P17"), country)
                .add(WDT("P31"), WD("Q1637706"))
                .add(WD("Q1637707"))
                .toString() shouldBe
                    """
                ?city wdt:P17 ?country ;
                    wdt:P31 wd:Q1637706, wd:Q1637707 .
            """.trimIndent()
        }
    }
})