package com.borgnetzwerk.searchsnail.utils.sparqlqb.prefixes

import com.borgnetzwerk.searchsnail.utils.sparqlqb.Namespace
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.platform.commons.annotation.Testable

@Testable
class PrefixTest : DescribeSpec({
    describe("prefixes") {
        it("prefix 1") {
            Namespace("wd", "http://www.example.org/entity")("P31").toString() shouldBe
                    "wd:P31"
        }

        it("prefix 2") {
            Namespace("wd", "http://www.example.org/entity")("P31").toString() shouldBe
                    "wd:P31"
        }
    }
})