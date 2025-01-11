package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.domain.model.VideoId
import com.borgnetzwerk.searchsnail.domain.model.YouTubeVideoData
import com.borgnetzwerk.searchsnail.domain.service.YoutubeVideoDataService
import com.borgnetzwerk.searchsnail.domain.service.youtube.YouTube
import org.springframework.stereotype.Repository

@Repository
class YouTubeVideosRepository(
    val youtubeService: YoutubeVideoDataService
) : YouTube {
    override fun get(key: String, ids: List<VideoId>): List<YouTubeVideoData> {
        youtubeService.setKey(key)
        return youtubeService.fetchVideos(ids).items.mapNotNull { videoItem -> YouTubeVideoData.resolve(videoItem) }
    }

}