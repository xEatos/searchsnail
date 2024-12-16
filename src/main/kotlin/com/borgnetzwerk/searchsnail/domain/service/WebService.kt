package com.borgnetzwerk.searchsnail.domain.service


import com.borgnetzwerk.searchsnail.domain.model.JSONYouTubeVideoData
import com.borgnetzwerk.searchsnail.domain.model.VideoId
import com.borgnetzwerk.searchsnail.repository.serialization.SecretYouTubeKey
import com.borgnetzwerk.searchsnail.utils.sparqlqb.DSL
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class QueryServiceDispatcher {
    val restTemplate = RestTemplate()

    val url = URL("https://bnwiki.wikibase.cloud/query/sparql").toString()

    val json = Json() { ignoreUnknownKeys = true }

    private val headers = HttpHeaders().apply {
        set("Accept", "application/sparql-results+json")
        contentType = MediaType.APPLICATION_FORM_URLENCODED
    }

    fun createEntity(body: String) = HttpEntity<String>(
        "query=" + URLEncoder.encode(body, StandardCharsets.UTF_8),
        headers
    )


    final inline fun <reified T> fetch(query: DSL): T {
        val queryStr = query.build()
        //println(queryStr)
        val response = restTemplate.exchange<String>(
            url,
            HttpMethod.POST,
            createEntity(queryStr)
        ).body
        return json.decodeFromString<T>(response ?: "")
    }
}

// https://youtube.googleapis.com/youtube/v3/videos?part=snippet&part=statistics&id=NHHUef7mrfY&key=AIzaSyBDb9q9lMnzeIbNauMLhCN2Gn1HHITRxo4
class YoutubeVideoDataService(val key: String) {
    val restTemplate = RestTemplate()

    val baseUrl = URL("https://youtube.googleapis.com/youtube/v3").toString()


    val json = Json() { ignoreUnknownKeys = true } // make a @Component of it

    private val headers = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    fun fetchVideos(videoIds: List<VideoId>): JSONYouTubeVideoData {
        val videosPath = baseUrl
            .plus("/videos?part=snippet&part=contentDetails")
            .plus(videoIds.map { it -> "&id=" + it.value }.joinToString(""))
            .plus("&key=$key") // move to own file and exclude in git

        println("videosPath:$videosPath")
        val response = restTemplate.exchange<String>(
            videosPath,
            HttpMethod.GET,
            HttpEntity<String>(headers),
        ).body

        return json.decodeFromString<JSONYouTubeVideoData>(response ?: "")
    }
}

// TODO encode it back
class RESTService {
    val restTemplate = RestTemplate()

    val baseUrl = URL("https://www.wikidata.org/w/rest.php/wikibase/v0/entities/items/Q42").toString()

    private val headers = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set(
            "Authorization",
            "***"
        )
    }

    fun fetchStatement(): String? = restTemplate.exchange<String>(
        baseUrl,
        HttpMethod.GET,
        HttpEntity<String>(headers),
    ).body
}

@Component
class WebService {
    private final val json = Json() { ignoreUnknownKeys = true }
    final val key =
        json.decodeFromString<SecretYouTubeKey>(File("src/main/resources/secrets/YouTubeApiKey.json").readText())

    @Bean
    fun sparqlQueryService(): QueryServiceDispatcher {
        return QueryServiceDispatcher()
    }

    @Bean
    fun youTubeQueryService(): YoutubeVideoDataService {
        return YoutubeVideoDataService(key.key)
    }

}

