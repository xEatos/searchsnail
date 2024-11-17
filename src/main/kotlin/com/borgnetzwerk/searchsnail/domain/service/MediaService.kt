package com.borgnetzwerk.searchsnail.domain.service

import com.borgnetzwerk.searchsnail.domain.model.Media
import com.borgnetzwerk.searchsnail.domain.model.Medium
import org.springframework.stereotype.Service

@Service
class MediaService(
    val mediaRepository: Media
) {
    fun getMedia(first: Int, after: String?): List<Medium> = mediaRepository.getMedia(first, after)

}