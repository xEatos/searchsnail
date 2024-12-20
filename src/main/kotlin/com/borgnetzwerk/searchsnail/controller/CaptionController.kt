package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.CaptionGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.TranscriptTextGraphQL
import com.borgnetzwerk.searchsnail.domain.service.WebService
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Controller

@Controller
class CaptionController (
    @Autowired
    val webService: WebService,
){
    val textService = webService.fetchTextService()
    private final val json = Json() { ignoreUnknownKeys = true }

    /*
    @QueryMapping
    fun transcriptChapters(
        @Argument captionId: String
    ): CaptionGraphQL? =
        textService.fetchText(captionId)?.let { response ->
            try {
                val parsed = json.decodeFromString<TranscriptObject>(response).parse
                CaptionGraphQL(
                    id = parsed.pageid.toString(),
                    text = parsed.wikitext.text
                )
            } catch (ex: Exception) {
                null
            }
        }

     */

}

