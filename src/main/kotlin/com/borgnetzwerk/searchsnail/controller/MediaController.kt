package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.MediumConnectionsGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.MediumEdgeGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.MediumGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.PageInfo
import com.borgnetzwerk.searchsnail.domain.service.MediaService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class MediaController(
    val mediaService: MediaService
) {

    @QueryMapping
    fun mediaConnections(@Argument first: Int, @Argument after: String?) =
        mediaService.getMedia(first + 1, after).mapIndexed { index, medium ->
            MediumEdgeGraphQL(
                index.toString(),
                MediumGraphQL(medium.id.value, medium.title, medium.thumbnail?.url.toString()),
            )
        }.let { list ->
            // make Interface for it
            MediumConnectionsGraphQL(
                PageInfo(
                    hasNextPage = list.size > first,
                    hasPreviousPage = (after?.toInt() ?: 0) > 0,
                    startCursor = after,
                    endCursor = ((after?.toInt() ?: 0) + list.size).toString()
                ),
                edges = list
            )
        }
}