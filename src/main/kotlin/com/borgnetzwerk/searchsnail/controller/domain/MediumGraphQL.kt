package com.borgnetzwerk.searchsnail.controller.domain

import java.time.LocalDate

data class MediumGraphQL(
    val id: String,
    val title: String?,
    val channel: String?,
    val thumbnail: String?,
    val duration: Int?,
    val publication: String?
)