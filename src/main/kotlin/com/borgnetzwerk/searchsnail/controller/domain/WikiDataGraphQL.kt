package com.borgnetzwerk.searchsnail.controller.domain

import com.borgnetzwerk.searchsnail.domain.model.ValueType


data class WikiDataResourceGraphQL(
    val id: String,
    val label: String,
) : WikiDataGraphQL()

data class WikiDataLiteralGraphQL(
    val value: String,
    val type: ValueType,
    val lang: String?,
) : WikiDataGraphQL()

sealed class WikiDataGraphQL

