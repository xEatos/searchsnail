package com.borgnetzwerk.searchsnail.domain.model

import java.net.URL
import java.time.Duration
import java.time.LocalDate

@JvmInline
value class MediumId(val value: String)

data class Medium(
    val id: MediumId,
    val title: String?,
    val channel: String?,
    val thumbnail: ResolvedThumbnail?,
    val duration: Int?,
    val publication: LocalDate?
)
