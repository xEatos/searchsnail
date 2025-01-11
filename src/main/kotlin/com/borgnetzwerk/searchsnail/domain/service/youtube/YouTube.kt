package com.borgnetzwerk.searchsnail.domain.service.youtube

import com.borgnetzwerk.searchsnail.domain.model.VideoId
import com.borgnetzwerk.searchsnail.domain.model.YouTubeVideoData

interface YouTube {
    fun get(key: String, ids: List<VideoId>): List<YouTubeVideoData>
}