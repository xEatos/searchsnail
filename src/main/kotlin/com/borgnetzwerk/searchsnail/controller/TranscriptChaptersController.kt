package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.TranscriptChapterGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.TranscriptTextGraphQL
import com.borgnetzwerk.searchsnail.domain.service.WebService
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMap

@Controller
class TranscriptChaptersController(
    webService: WebService,
) {
    val apiRequest = webService.restFetchService()
    private final val json = Json() { ignoreUnknownKeys = true }

    @QueryMapping
    fun transcriptChapters(
        @Argument transcriptIds: List<String>
    ): List<TranscriptTextGraphQL> = transcriptIds.mapNotNull { it ->
        apiRequest.fetch(
            "https://borgnetzwerk.miraheze.org/w/api.php?",
            mapOf(
                "action" to "parse",
                "prop" to "wikitext",
                "pageid" to it,
                "format" to "json"
            ),
            HttpEntity<String>(HttpHeaders().apply {
                set(HttpHeaders.ACCEPT, "application/json")
            })
        )?.let { response ->
            try {
                val parsed = json.decodeFromString<TranscriptObject>(response).parse
                TranscriptTextGraphQL(
                    id = parsed.pageid.toString(),
                    text = parsed.wikitext.text
                )
            } catch (ex: Exception) {
                null
            }
        }
    }
}

@Serializable
data class TranscriptObject(
    val parse: TranscriptParse
)

@Serializable
data class Wikitext(
    val text: String,
)

@Serializable
data class TranscriptParse(
    val pageid: Int,
    val title: String,
    @Serializable(with = WikitextTransformer::class)
    val wikitext: Wikitext
)

object WikitextTransformer : JsonTransformingSerializer<Wikitext>(Wikitext.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> buildJsonObject {
                element.forEach { (key, value) ->
                    when (key) {
                        "*" -> put("text", value)
                        else -> put(key, value)
                    }
                }
            }
            else -> element
        }
    }
}