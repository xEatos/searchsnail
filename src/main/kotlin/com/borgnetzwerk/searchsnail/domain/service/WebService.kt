package com.borgnetzwerk.searchsnail.domain.service




import com.borgnetzwerk.searchsnail.utils.sparqlqb.DSL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
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
        //println("sparql response:$response")
        return json.decodeFromString<T>(response ?: "")
    }
}

/*
class YoutubeVideoDataService() {
    val restTemplate = RestTemplate()

    val baseUrl = URL("https://youtube.googleapis.com/youtube/v3").toString()


    val json = Json() { ignoreUnknownKeys = true } // make a @Component of it

    private var key = ""

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

    fun setKey(key: String) {this.key = key}

}
*/


class QueryMiraheze(){
    private val restService = RESTService()

    private final val json = Json() { ignoreUnknownKeys = true }

    fun fetchText(it: String) = restService.fetch(
        "https://borgnetzwerk.miraheze.org/w/api.php?",
        mapOf(
            "action" to "parse",
            "prop" to "wikitext",
            "pageid" to it,
            "format" to "json"
        ),
        HttpEntity<String>(HttpHeaders().apply {
            set(HttpHeaders.ACCEPT, "application/json")
        })
    )?.let { response ->
        try {
            val parsed = json.decodeFromString<MirahezeObject>(response).parse
            Text(
                id = parsed.pageid.toString(),
                text = parsed.wikitext.text
            )
        } catch (ex: Exception) {
            null
        }
    }
}

data class Text(
    val id: String,
    val text: String
)

@Serializable
data class MirahezeObject(
    val parse: MirahezeParse
)

@Serializable
data class Wikitext(
    val text: String,
)

@Serializable
data class MirahezeParse(
    val pageid: Int,
    val title: String,
    @Serializable(with = WikitextTransformer::class)
    val wikitext: Wikitext
)

object WikitextTransformer : JsonTransformingSerializer<Wikitext>(Wikitext.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> buildJsonObject {
                element.forEach { (key, value) ->
                    when (key) {
                        "*" -> put("text", value)
                        else -> put(key, value)
                    }
                }
            }
            else -> element
        }
    }
}

@Service
class GenericFetchService{
    val restTemplate = RestTemplate()

    val json = Json() { ignoreUnknownKeys = true }

    final inline fun <reified T> fetch(url: URL, httpMethod: HttpMethod = HttpMethod.GET, httpEntity: HttpEntity<String>? = null): T {
        println("request: $url")
        val response = restTemplate.exchange<String>(
            url.toString(),
            httpMethod,
            httpEntity
        ).body
        println("response: $response")
        return json.decodeFromString<T>(response ?: "")
    }
}


class RESTService {
    private val restTemplate = RestTemplate()

    fun fetch(url: String, urlParams: Map<String, String>, httpEntity: HttpEntity<String>): String? {
        val fullUrl = url + urlParams.map { it -> "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}" }.joinToString("&")
        val body = restTemplate.exchange<String>(
            fullUrl,
            HttpMethod.GET,
            httpEntity
        ).body

        println("url: $fullUrl")

        return body
    }

}

@Component
class WebService {
    private final val json = Json() { ignoreUnknownKeys = true }

    @Bean
    fun sparqlQueryService(): QueryServiceDispatcher {
        return QueryServiceDispatcher()
    }

    @Bean
    fun fetchTextService(): QueryMiraheze {
        return QueryMiraheze()
    }

}

