package com.borgnetzwerk.searchsnail.controller.domain


data class FilterOptionGraphQL (
    val filterId: String,
    val filterType: String,
    val label: String,
    val options: List<WikiDataGraphQL>,
    val group: String,
)