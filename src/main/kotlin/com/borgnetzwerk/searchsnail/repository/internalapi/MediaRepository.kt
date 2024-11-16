package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.configuration.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.model.Media
import com.borgnetzwerk.searchsnail.domain.model.Medium
import com.borgnetzwerk.searchsnail.domain.model.MediumId
import com.borgnetzwerk.searchsnail.domain.model.UnresolvedThumbnail
import com.borgnetzwerk.searchsnail.utils.sparqlqb.*
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.net.URL

@Repository
class MediaRepository(
    @Autowired
    val webClient: QueryServiceDispatcher
) : Media {

    val item = Namespace.ITEM
    val propt = Namespace.PROPT
    val rdfs = Namespace.RDFS

    val media = Var("media")
    val name = Var("mediaName")
    val thumbnail = Var("thumbnail")


    private fun getMediaQuery(first: Int, after: String?) = DSL()
        .select(media, name, thumbnail)
        .where(
            GraphPattern().add(
                BasicGraphPattern(media, propt("P1"), item("Q5"))
                    .add(rdfs("label"), name)
                    .add(propt("P7"), thumbnail)
            )
        )
        .orderBy("ASC($name)")
        .limit(first)
        .offset(after?.toInt() ?: 0)


    override fun getMedia(first: Int, after: String?) = webClient
        .fetch<MediaQueryResult>(getMediaQuery(first, after))
        .results.bindings.map { row ->
            Medium(
                MediumId(row.media.value),
                row.mediaName.value,
                UnresolvedThumbnail(URL(row.thumbnail.value)).resolve()
            )
        }
}

@Serializable
data class WikidataObject(val type: String, val value: String)

@Serializable
data class Head(val vars: List<String>)

@Serializable
data class Row(val media: WikidataObject, val mediaName: WikidataObject, val thumbnail: WikidataObject)

@Serializable
data class Bindings(val bindings: List<Row>)

@Serializable
data class MediaQueryResult(val head: Head, val results: Bindings)

