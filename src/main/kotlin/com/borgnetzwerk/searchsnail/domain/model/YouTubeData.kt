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

data class Chapter(val start: MediumDuration?, val end: MediumDuration?, val title: String) {
    companion object {

        private fun ofDescriptionEntry(str: String): Pair<MediumDuration?, String> {
            val d = str.substring(0, str.indexOf(' '))
            val t = str.substring(d.length)
            return Pair(MediumDuration.of(d), t)
        }

        fun resolveFromYouTubeDescription(description: String, duration: MediumDuration): List<Chapter> {
            val regex = Regex("([0-9]+:[0-9][0-9] [^\\n]+)", RegexOption.MULTILINE)
            val matches = regex.findAll(description).toList()
            return matches.mapIndexed { index, it ->
                val (startTimestamp, title) = ofDescriptionEntry(it.value)
                val endTimestamp = if (index == matches.size - 1) {
                    duration
                } else {
                   ofDescriptionEntry(matches[index + 1].value).first
                }
                Chapter(startTimestamp, endTimestamp, title)
            }.toList()
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
    val tags: List<String>,
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