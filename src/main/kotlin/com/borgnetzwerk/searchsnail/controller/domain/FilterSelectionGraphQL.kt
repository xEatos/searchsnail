package com.borgnetzwerk.searchsnail.controller.domain

import com.borgnetzwerk.searchsnail.domain.model.WikiDataLiteral
import com.borgnetzwerk.searchsnail.domain.model.WikiDataResource

data class FilterSelectionGraphQL(
    val filterId: String,
    val literals: List<WikiDataLiteralGraphQL>?,
    val resources: List<WikiDataResourceGraphQL>?
)
