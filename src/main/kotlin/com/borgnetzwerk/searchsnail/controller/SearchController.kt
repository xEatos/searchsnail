package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.*
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.search.SearchStrategyResolverService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class SearchController(
    @Autowired
    val searchService: SearchStrategyResolverService,
) {
    @QueryMapping
    fun mediaConnections(
        @Argument limit: Int,
        @Argument offsetMap: List<OffsetPair>?,
        @Argument filter: List<FilterSelectionInputGraphQL>?,
    ): MediaConnectionsGraphQL {
        val filterSelections = filter?.mapNotNull { it.toDomain() } ?: emptyList()

        val (pageMap, foundFilters) = searchService.search(
            filters = filterSelections,
            offsetMap = offsetMap?.associate { it.provenance to it.offset } ?: emptyMap(),
            limit = limit,
        )

        /*
        TODO: Add a optional skip to skip wiki if it hasNoNextPage to safe a query
         */


        return MediaConnectionsGraphQL(
            media = pageMap.entries.map { (provenance, indexedPage) ->
                MediaPageGraphQL(
                    pageInfo = SearchPageInfoGraphQL(
                        provenance = provenance,
                        hasNextPage = indexedPage.hasNextPage,
                        hasPreviousPage = indexedPage.hasPreviousPage,
                        offset = indexedPage.offset,
                        limit = indexedPage.limit
                    ),
                    edges = indexedPage.elements.map { indexedElement ->
                        MediumEdge(
                            cursor = indexedElement.index,
                            node = indexedElement.value.toGraphQL()
                        )
                    }
                )
            },
            foundFilters = foundFilters.map { it.toGraphQL() }
        )
    }

    private fun FilterSelectionInputGraphQL.toDomain(): FilterSelection? = UnresolvedFilterId(this.filterId)
        .resolve()
        ?.let { resolvedFilterId ->
            FilterSelection(
                resolvedFilterId, (
                    (
                        this.literals?.map { literal ->
                            WikiDataLiteral(
                                literal.value,
                                literal.type,
                                literal.lang?.let { ISO639(it) })
                        } ?: mutableListOf<WikiData>()
                    ) + (
                        this.resources?.map { resource ->
                            WikiDataResource(
                                resource.id,
                                resource.label
                            )
                        } ?: mutableListOf()
                    )
                ).toMutableList()
            )
        }

    private fun FilterSelection.toGraphQL(): FilterSelectionGraphQL = FilterSelectionGraphQL(
        this.filterId.value.toString(),
        this.selections.map { it.toGraphQl() }
    )
}


data class MediaConnectionsGraphQL(
    val media: List<MediaPageGraphQL>,
    val foundFilters: List<FilterSelectionGraphQL>,
)

data class MediaPageGraphQL(
    val pageInfo: SearchPageInfoGraphQL,
    val edges: List<MediumEdge>,
)

data class SearchPageInfoGraphQL(
    val provenance: String,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val offset: Int,
    val limit: Int,
)

data class MediumEdge(
    val cursor: Int,
    val node: LeanMediumGraphQL,
)

data class FilterSelectionGraphQL(
    val filterId: String,
    val data: List<WikiDataGraphQL>,
)

data class OffsetPair(
    val provenance: String,
    val offset: Int,
)

