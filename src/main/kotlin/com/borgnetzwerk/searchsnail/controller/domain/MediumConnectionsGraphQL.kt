package com.borgnetzwerk.searchsnail.controller.domain


data class PageInfo(val hasNextPage: Boolean, val hasPreviousPage: Boolean, val startCursor: String?, val endCursor: String?)

data class MediumEdgeGraphQL (val cursor: String, val node: MediumGraphQL)

data class MediumConnectionsGraphQL(
    val pageInfo: PageInfo,
    val edges: List<MediumEdgeGraphQL>
)