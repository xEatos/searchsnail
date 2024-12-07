package com.borgnetzwerk.searchsnail.configuration

import com.borgnetzwerk.searchsnail.utils.sparqlqb.DSL
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Configuration
class QueryServiceDispatcher {
    val restTemplate = RestTemplate()

    val url = URL("https://bnwiki.wikibase.cloud/query/sparql").toString()

    val json = Json(){ignoreUnknownKeys = true}

    private val headers = HttpHeaders().apply {
        set("Accept", "application/sparql-results+json")
        contentType = MediaType.APPLICATION_FORM_URLENCODED
    }

    fun createEntity(body: String) = HttpEntity<String>(
        "query=" + URLEncoder.encode(body, StandardCharsets.UTF_8),
        headers)


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