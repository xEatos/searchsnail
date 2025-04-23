package com.borgnetzwerk.searchsnail.controller.domain

data class TranscriptGraphQL(
    val language: String?,
    val chapters: List<TranscriptChapterGraphQL>
)

data class TranscriptChapterGraphQL(
    val id: String?,
    val heading: String,
    val startTimestamp : Int?,
    val endTimestamp : Int?
)

data class TranscriptTextGraphQL(
    val text: String,
    val id: String
)

data class CaptionGraphQL(
    val text: String,
    val id: String
)