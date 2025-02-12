package com.borgnetzwerk.searchsnail.repository.model

// maybe not good idead
data class SearchPageParameters<T>(
    val offset: Int,
    val limit: Int,
    val parameters: T
)

interface Query {
    fun <T, R>query(search: SearchPageParameters<T>): Page<R>
}
