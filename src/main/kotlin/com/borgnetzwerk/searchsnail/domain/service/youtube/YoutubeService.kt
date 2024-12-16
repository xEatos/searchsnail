package com.borgnetzwerk.searchsnail.domain.service.youtube

import com.borgnetzwerk.searchsnail.domain.model.VideoId
import com.borgnetzwerk.searchsnail.domain.model.YouTubeVideoData
import com.borgnetzwerk.searchsnail.repository.internalapi.YouTubeVideosRepository
import org.springframework.stereotype.Service

@Service
class YoutubeService (
    val youTubeRepo: YouTube
){
    fun get(ids: List<VideoId>): List<YouTubeVideoData> = youTubeRepo.get(ids)
}