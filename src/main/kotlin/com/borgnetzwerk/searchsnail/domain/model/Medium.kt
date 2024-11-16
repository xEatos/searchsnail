package com.borgnetzwerk.searchsnail.domain.model

import java.net.URL

@JvmInline
value class MediumId(val value: String)

data class Medium(
    val id: MediumId,
    val title: String?,
    val thumbnail: ResolvedThumbnail?
)
