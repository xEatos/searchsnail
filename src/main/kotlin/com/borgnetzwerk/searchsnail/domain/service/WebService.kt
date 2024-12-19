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
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
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
        println(queryStr)
        val response = restTemplate.exchange<String>(
            url,
            HttpMethod.POST,
            createEntity(queryStr)
        ).body
        println(response)
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

    val data = "data=" + URLEncoder.encode("""{"claims":{"P26":[{"mainsnak":{"snaktype":"value","property":"P26","datatype":"string","datavalue":{"type":"string","value":"1486"}},"type":"statement","rank":"normal"}]}}""", StandardCharsets.UTF_8)
    val action = """action=wbeditentity&format=json&id=Q18&token=8af2a30c1b3e25a4bdeda2e8d5ddb1166761a39f%2B%5C&"""

    val baseUrl = URL("https://bnwiki.wikibase.cloud/w/api.php?").toString()

    private val headers = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_FORM_URLENCODED

    }

    private val headers2 = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0")
        set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

    }

    // f1f94f12e557163c6c4a250d0f5989896761b7c4%2B%5C%5C
    fun fetchToken(): String = restTemplate.exchange<String>(
        """https://bnwiki.wikibase.cloud/w/api.php?action=query&format=json&meta=tokens&type=csrf|login""",
        HttpMethod.GET,
        HttpEntity<String>(headers2)
    ).body!!

    fun fetchStatement(): String? {
        val token = URLEncoder.encode("""a2fc05664119bfea7ee5482a29bb30f66761b423+\""", StandardCharsets.UTF_8)
        return restTemplate.exchange<String>(
            """https://bnwiki.wikibase.cloud/w/api.php""",
            HttpMethod.POST,
            HttpEntity<String>(
                """action=wbeditentity&format=json&new=item&token=${token}&data=%7B%22labels%22%3A%7B%22en%22%3A%7B%22language%22%3A%22en%22%2C%22value%22%3A%22So%20verbreiten%20sich%20Fake%20News%20%7C%20Reaktion%20auf%20Hoss%20%26%20Hopf%22%7D%7D%2C%22descriptions%22%3A%7B%22en%22%3A%7B%22language%22%3A%22en%22%2C%22value%22%3A%22A%20video%20of%20Doktor%20Whatson%20correcting%20the%20false%20statements%20of%20Hoss%20%26%20Hopf%22%7D%7D%2C%22claims%22%3A%7B%22P1%22%3A%5B%7B%22mainsnak%22%3A%7B%22snaktype%22%3A%22value%22%2C%22property%22%3A%22P1%22%2C%22datatype%22%3A%22wikibase-item%22%2C%22datavalue%22%3A%7B%22type%22%3A%22wikibase-entityid%22%2C%22value%22%3A%7B%22entity-type%22%3A%22item%22%2C%22numeric-id%22%3A%225%22%2C%22id%22%3A%22Q5%22%7D%7D%7D%2C%22type%22%3A%22statement%22%2C%22rank%22%3A%22normal%22%7D%5D%7D%7D""",
                headers
            ),
        ).body
    }

    fun httpClientGetRequest() {
        val requestFactory = SimpleClientHttpRequestFactory()
        val uri: URI = URI.create("https://bnwiki.wikibase.cloud/w/api.php?action=query&meta=tokens&format=json&type=csrf")


        // Create a new ClientHttpRequest
        val request: ClientHttpRequest = requestFactory.createRequest(uri, HttpMethod.GET)


        request.execute().use { response ->
            BufferedReader(InputStreamReader(response.getBody())).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    println(line)
                }
            }
        }
    }
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

    @Bean
    fun restFetchService(): RESTService {
        return RESTService()
    }

}

