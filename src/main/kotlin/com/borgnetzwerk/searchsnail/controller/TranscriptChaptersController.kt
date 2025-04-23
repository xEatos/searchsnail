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
    val textService = webService.fetchTextService()
    private final val json = Json() { ignoreUnknownKeys = true }

    @QueryMapping
    fun transcriptChapters(
        @Argument transcriptIds: List<String>
    ): List<TranscriptTextGraphQL> = transcriptIds.mapNotNull { it ->
        textService.fetchText(it)?.let { text -> TranscriptTextGraphQL(text.text, text.id) }
    }
}

