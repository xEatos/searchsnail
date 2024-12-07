package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.*
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.media.MediaService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class MediaController(
    val mediaService: MediaService
) {

    // TODO Testplan
    @QueryMapping
    fun mediaConnections(
        @Argument first: Int,
        @Argument after: String?,
        @Argument filter: List<FilterSelectionGraphQL>?
    ): MediumConnectionsGraphQL {
        val filterSelections = filter?.mapNotNull { f ->
            UnresolvedFilterId(f.filterId).resolve()?.let { resolvedFilterId ->
                FilterSelection(
                    resolvedFilterId,
                    (f.literals?.map { literal ->
                        WikiDataLiteral(
                            literal.value,
                            literal.type,
                            literal.lang?.let { ISO639(it) })
                    } ?: emptyList())
                            + (f.resources?.map { resource -> WikiDataResource(resource.id, resource.label) }
                        ?: emptyList())
                )
            }
        }


        return mediaService.getMedia(first + 1, after, filterSelections ?: emptyList()).mapIndexed { index, medium ->
            MediumEdgeGraphQL(
                index.toString(),
                MediumGraphQL(
                    medium.id.value,
                    medium.title,
                    medium.channel,
                    medium.thumbnail?.url.toString(),
                    medium.duration,
                    medium.publication?.toString()
                ),
            )
        }.let { list ->
            // make Interface and class for pagination it
            println(filter)
            MediumConnectionsGraphQL(
                PageInfo(
                    hasNextPage = list.size > first,
                    hasPreviousPage = (after?.toInt() ?: 0) > 0,
                    startCursor = after ?: "0",
                    endCursor = ((after?.toInt() ?: 0) + list.size - 1).toString()
                ),
                edges = list
            )
        }
    }

}