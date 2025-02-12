package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.*
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.media.MediaService
import com.borgnetzwerk.searchsnail.domain.service.search.SearchStrategyResolver
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class MediaController(
    val mediaService: MediaService,
    val searchStrategyResolver: SearchStrategyResolver,
) {

    @QueryMapping
    fun mediaConnections(
        @Argument first: Int,
        @Argument after: WikiBatchAfterQL,
        @Argument filter: List<FilterSelectionGraphQL>?,
    ): ConnectionGraphQL<LeanMediumGraphQL> {
        val filterSelections = filter?.mapNotNull { f ->
            UnresolvedFilterId(f.filterId).resolve()?.let { resolvedFilterId ->
                val literals = f.literals?.map { literal ->
                    WikiDataLiteral(
                        literal.value,
                        literal.type,
                        literal.lang?.let { ISO639(it) })
                } ?: mutableListOf<WikiData>()
                val resources = f.resources?.map { resource -> WikiDataResource(resource.id, resource.label) }
                    ?: mutableListOf()
                FilterSelection(
                    resolvedFilterId, (literals + resources).toMutableList()
                )
            }
        }

        return searchStrategyResolver.getMedia(first + 1, after, filterSelections ?: emptyList()).let {
            println(it)
            ConnectionGraphQL(
                edges = it.media.map { medium ->
                    EdgeGraphQL(
                        LeanMediumGraphQL(
                            id = medium.id.value,
                            type = medium.type,
                            title = medium.title,
                            publication = medium.publication.toString(),
                            channel = medium.channel,
                            thumbnail = medium.thumbnail?.url.toString(),
                            duration = medium.duration
                        ), 0 // TODO
                    )
                },
                pageInfo = PageInfoGraphQL(
                    hasPreviousPage = it.batchInfo.wikibase.startOffset > -1
                            || it.batchInfo.sparql.startOffset > -1
                            || it.batchInfo.miraheze.startOffset > -1,
                    hasNextPage = it.batchInfo.wikibase.`continue` || it.batchInfo.miraheze.`continue` || it.batchInfo.sparql.`continue`,
                    boxInfo = it.boxInfo,
                    batchInfo = it.batchInfo

                )
            )
        }
    }

}