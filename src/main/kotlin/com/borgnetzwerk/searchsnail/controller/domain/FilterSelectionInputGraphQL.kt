package com.borgnetzwerk.searchsnail.controller.domain

data class FilterSelectionInputGraphQL(
    val filterId: String,
    val literals: List<WikiDataLiteralGraphQL>?,
    val resources: List<WikiDataResourceGraphQL>?
)
