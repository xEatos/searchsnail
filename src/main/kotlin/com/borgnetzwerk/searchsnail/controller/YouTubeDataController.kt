package com.borgnetzwerk.searchsnail.controller
/*
import com.borgnetzwerk.searchsnail.controller.domain.LeanMediumGraphQL
import com.borgnetzwerk.searchsnail.domain.model.VideoId
import com.borgnetzwerk.searchsnail.domain.service.youtube.YoutubeService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class YouTubeDataController(
    val youtubeService: YoutubeService
) {
    @QueryMapping
    fun getYouTubeVideosData(@Argument key: String, @Argument watchIds: List<String>): List<LeanMediumGraphQL> {
        return youtubeService.get(key, watchIds.map { VideoId(it) }).map { it -> LeanMediumGraphQL(
            id = it.id.value,
            type = "Video",
            title = it.title,
            publication = it.publishedAt.toString(),
            channel = it.channelTitle,
            thumbnail = it.thumbnailUrl.toString(),
            duration = it.duration
        ) }
    }
}*/