package com.borgnetzwerk.searchsnail.domain.model

import kotlinx.serialization.Serializable
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@JvmInline
value class VideoId(val value: String)

data class YouTubeVideoData(
    val id: VideoId,
    val publishedAt: LocalDate,
    val channelId: String,
    val title: String,
    val description: String,
    val chapters: List<Chapter>,
    val thumbnailUrl: URL,
    val channelTitle: String,
    val tags: List<String>,
    val defaultAudioLanguage: ISO639,
    val duration: Int
) {
    companion object {
        fun resolve(item: VideoItem): YouTubeVideoData? { // move this to VideoItem?
            try {
                return YouTubeVideoData(
                    id = VideoId(item.id),
                    publishedAt = LocalDateTime.parse(item.snippet.publishedAt.dropLast(1)).toLocalDate(),
                    channelId = item.snippet.channelId,
                    title = item.snippet.title,
                    description = item.snippet.description,
                    thumbnailUrl = URL(item.snippet.thumbnails.medium.url),
                    channelTitle = item.snippet.channelTitle,
                    tags = item.snippet.tags,
                    defaultAudioLanguage = ISO639(item.snippet.defaultAudioLanguage),
                    duration = Duration.parse(item.contentDetails.duration).toSeconds().toInt(),
                    chapters = MediumDuration.of(Duration.parse(item.contentDetails.duration))
                        ?.let { it -> Chapter.resolveFromYouTubeDescription(item.snippet.description, it) }
                        ?: emptyList()
                )
            } catch (e: Exception) {
                println(e)
                return null
            }
        }
    }
}




@Serializable
data class Thumbnail(
    val url: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class Thumbnails(
    val medium: Thumbnail
)

@Serializable
data class ContentDetails(
    val duration: String,
)

@Serializable
data class Snippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: Thumbnails,
    val channelTitle: String,
    val tags: List<String> = emptyList(),
    val defaultAudioLanguage: String,

    )

@Serializable
data class VideoItem(
    val id: String,
    val snippet: Snippet,
    val contentDetails: ContentDetails
)

@Serializable
data class YouTubePageInfo(
    val totalResults: Int,
    val resultsPerPage: Int,
)

@Serializable
data class JSONYouTubeVideoData(
    val items: List<VideoItem>,
    val pageInfo: YouTubePageInfo
)