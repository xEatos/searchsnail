package com.borgnetzwerk.searchsnail.domain.model


import com.borgnetzwerk.searchsnail.controller.domain.MediumGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.toGraphQl
import java.time.LocalDate

@JvmInline
value class MediumId(val value: String)

data class Medium(
    val id: MediumId,
    val type: String?,
    val title: String,
    val caption: Caption?,
    val publication: LocalDate?,
    val channel: WikiData?,
    val thumbnail: ResolvedThumbnail?,
    val categories: List<WikiData>?,
    val transcripts: List<Transcript>?,
    val languages: List<ISO639>?,
    val subtitleLanguages: List<ISO639>?,
    val duration: Int?,
) {
    fun toGraphQL(): MediumGraphQL =
        MediumGraphQL(
            id = id.value,
            type = type,
            title = title,
            caption = null,
            publication = publication.toString(),
            channel = channel?.toGraphQl(),
            thumbnail = thumbnail?.url.toString(),
            categories = categories?.map { it.toGraphQl() },
            transcripts = transcripts?.map { it.toGraphQL() },
            languages = languages?.map { it.value },
            subtitleLanguages = subtitleLanguages?.map { it.value },
            duration = duration
        )

}

data class Caption(
    val text: String,
    val id: String
)

data class LeanMedium(
    val id: MediumId,
    val type: String,
    val title: String,
    val publication: LocalDate?,
    val channel: String?,
    val thumbnail: ResolvedThumbnail?,
    val duration: Int?,
)