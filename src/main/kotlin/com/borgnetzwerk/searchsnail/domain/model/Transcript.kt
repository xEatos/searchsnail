package com.borgnetzwerk.searchsnail.domain.model

import com.borgnetzwerk.searchsnail.controller.domain.TranscriptChapterGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.TranscriptGraphQL

@JvmInline
value class TranscriptId(val value: String)

data class Transcript(
    val language: ISO639?,
    val chapters: List<TranscriptChapter>
){
    fun toGraphQL(): TranscriptGraphQL = TranscriptGraphQL(
        language?.value,
        chapters.map { it.toGraphQL() }
    )
}

data class TranscriptChapter(
    val id: TranscriptId?,
    val chapter: Chapter,
) {
    fun toGraphQL(): TranscriptChapterGraphQL = TranscriptChapterGraphQL(
        id?.value,
        heading = chapter.title,
        startTimestamp = chapter.start?.value,
        endTimestamp = chapter.end?.value
    )
}
