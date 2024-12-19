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

    @QueryMapping
    fun mediaConnections(
        @Argument first: Int,
        @Argument after: String?,
        @Argument filter: List<FilterSelectionGraphQL>?
    ): ConnectionGraphQL<LeanMediumGraphQL> {
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


        return mediaService.getMedia(first + 1, after, filterSelections ?: emptyList()).let {
            println(it)
            it
        }.map { medium ->
                LeanMediumGraphQL(
                    id = medium.id.value,
                    type = medium.type,
                    title = medium.title,
                    publication = medium.publication.toString(),
                    channel = medium.channel,
                    thumbnail = medium.thumbnail?.url.toString(),
                    duration = medium.duration
                )
        }.let { list ->
            ConnectionGraphQL.resolve(list, first, after, 1)
        }!!
    }

}