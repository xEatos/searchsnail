package com.borgnetzwerk.searchsnail.repository.serialization

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.platform.commons.annotation.Testable

@Serializable
data class Container (
    @Serializable(with = WikidataObjectTransformer::class)
    val channel: WikidataObject
)

@Testable
class WikidataObjectTest : DescribeSpec({
    describe("WikidataObjectTest") {
        it("channel wiki data literal"){
            val jsonStr = ("""
            {
                "channel": {
                    "xml:lang": "en",
                    "type": "literal",
                    "value": "Doktor Whatson"
                }
            }
            """).trimIndent()

            val json = Json { ignoreUnknownKeys = true }

            val container = json.decodeFromString<Container>(jsonStr)
            println(container)
        }
    }
})