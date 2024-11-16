package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.MediumGraphQL
import com.borgnetzwerk.searchsnail.domain.model.Medium
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
        mediaService.getMedia(first, after).map { MediumGraphQL(it.id.value, it.title, it.thumbnail?.url.toString()) }

}