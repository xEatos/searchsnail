package com.borgnetzwerk.searchsnail.controller.domain

import com.borgnetzwerk.searchsnail.domain.model.*
import java.time.LocalDate

data class MediumGraphQL(
    val id: String,
    val type: String?,
    val title: String?,
    val caption: CaptionGraphQL?,
    val publication: String?,
    val channel: WikiDataGraphQL?,
    val thumbnail: String?,
    val categories: List<WikiDataGraphQL>?,
    val transcripts: List<TranscriptGraphQL>?,
    val languages: List<String>?,
    val subtitleLanguages: List<String>?,
    val duration: Int?,
)

data class LeanMediumGraphQL(
    val id: String,
    val type: String,
    val title: String?,
    val publication: String?,
    val channel: String?,
    val thumbnail: String?,
    val duration: Int?,
)
