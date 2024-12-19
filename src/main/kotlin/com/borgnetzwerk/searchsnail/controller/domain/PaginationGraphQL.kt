package com.borgnetzwerk.searchsnail.controller.domain

import kotlin.math.min

/*
 TODO: Move to natural indexing, where first element has index 1. Instead of 0
    -> makes to handle after=0 easier, because it is same as null
    -> makes calculation with totalCount and first easier
*/
data class ConnectionGraphQL<T>(
    val edges: List<EdgeGraphQL<T>>,
    val pageInfo: PageInfoGraphQL,
    val totalCount: Int,
) {
    companion object {
        private fun hasPreviousPage(after: Int?): Boolean = after?.let { it -> it >= 0 } ?: false

        private fun hasNextPage(first: Int, after: Int?, totalCount: Int): Boolean =
            after?.let { (after + first) < totalCount } ?: (first < totalCount)

        fun <R> resolve(list: List<R>, first: Int, after: String?, totalCount: Int): ConnectionGraphQL<R>? {
            if (first < 0 || totalCount < 0) { return null }

            val afterAsInt: Int?

            try {
                afterAsInt = after?.toInt()
                if (afterAsInt != null && (afterAsInt < 0 || totalCount -1 < afterAsInt +1)) {
                    return null
                }
            } catch (e: Exception) { return null }

            val edges: List<EdgeGraphQL<R>> = list.subList(0, min(first, list.size)).mapIndexed { index, it ->
                val startCursor = afterAsInt ?: -1
                EdgeGraphQL(it, (index + startCursor +1).toString())
            }

            return ConnectionGraphQL(
                edges,
                PageInfoGraphQL(
                    hasPreviousPage = hasPreviousPage(afterAsInt),
                    hasNextPage = hasNextPage(first, afterAsInt, totalCount),
                    startCursor = edges.firstOrNull()?.cursor,
                    endCursor = edges.lastOrNull()?.cursor
                ),
                totalCount
            )
        }
    }
}

data class EdgeGraphQL<T>(val node: T, val cursor: String)

data class PageInfoGraphQL(
    val hasPreviousPage: Boolean,
    val hasNextPage: Boolean,
    val startCursor: String?,
    val endCursor: String?
)

