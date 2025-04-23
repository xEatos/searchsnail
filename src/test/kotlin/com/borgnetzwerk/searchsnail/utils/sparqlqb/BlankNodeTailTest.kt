package com.borgnetzwerk.searchsnail.utils.sparqlqb

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.platform.commons.annotation.Testable

@Testable
class BlankNodeTailTest : DescribeSpec({
    describe("BlankNodeTailTest") {
        val EX = Namespace("ex", "http://www.example.org/")
        val XSD = Namespace.XSD

        it("with one property and one iri ") {
            BlankNodeTail(
                EX("prop"), EX("attr1")
            ).toString() shouldBe
                    """
            [
                ex:prop ex:attr1
            ]
            """.trimIndent()
        }


        it("with one property and two iri ") {
            BlankNodeTail(
                EX("prop"), EX("attr1")
            ).with(EX("prop"), EX("attr2"))
                .toString() shouldBe
                    """
            [
                ex:prop ex:attr1, ex:attr2
            ]
            """.trimIndent()
        }

        it("with two property and each one an iri ") {
            BlankNodeTail(
                EX("prop1"), EX("attr1")
            ).with(EX("prop2"), EX("attr2"))
                .toString() shouldBe
                    """
            [
                ex:prop1 ex:attr1 ;
                ex:prop2 ex:attr2
            ]
            """.trimIndent()
        }

        it("with two property and one iri, literal ") {
            BlankNodeTail(
                EX("prop1"), EX("attr1")
            ).with(EX("prop2"), Literal(42, XSD("integer")))
                .toString() shouldBe
                    """
            [
                ex:prop1 ex:attr1 ;
                ex:prop2 "42"^^xsd:integer
            ]
            """.trimIndent()
        }

        it("with one property path and one iri ") {
            BlankNodeTail(
                listOf(EX("prop1"), EX("prop2")), EX("attr1")
            )
                .toString() shouldBe
                    """
            [
                ex:prop1 / ex:prop2 ex:attr1
            ]
            """.trimIndent()
        }

        it("with one property, one property path and one iri ") {
            BlankNodeTail(
                listOf(EX("prop1"), EX("prop2")), EX("attr1")
            ).with(EX("prop1"), Literal(42, XSD("integer")))
                .toString() shouldBe
                    """
            [
                ex:prop1 / ex:prop2 ex:attr1 ;
                ex:prop1 "42"^^xsd:integer
            ]
            """.trimIndent()
        }

        it("complex") {
            BlankNodeTail(
                listOf(EX("prop1"), EX("prop2")), EX("attr1")
            ).with(
                EX("prop1"),
                BlankNodeTail(EX("prop3"), Literal(42, XSD("integer"))).with(
                    EX("prop1"),
                    BlankNodeTail(EX("prop3"), Literal(42, XSD("integer")))
                )
            ).with(
                EX("prop2"),
                BlankNodeTail(
                    EX("prop1"),
                    BlankNodeTail(EX("prop3"), Literal(42, XSD("integer")))
                ).with(
                    EX("prop3"), Literal(42, XSD("integer"))
                )
            )
                .toString() shouldBe
                    """
            [
                ex:prop1 / ex:prop2 ex:attr1 ;
                ex:prop1 [
                    ex:prop3 "42"^^xsd:integer ;
                    ex:prop1 [
                        ex:prop3 "42"^^xsd:integer
                    ]
                ] ;
                ex:prop2 [
                    ex:prop1 [
                        ex:prop3 "42"^^xsd:integer
                    ] ;
                    ex:prop3 "42"^^xsd:integer
                ]
            ]
            """.trimIndent()
        }
    }
})