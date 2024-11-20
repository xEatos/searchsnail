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
import java.time.Duration
import java.time.LocalDate

@Repository
class MediaRepository(
    @Autowired
    val webClient: QueryServiceDispatcher
) : Media {

    val item = Namespace.ITEM
    val propt = Namespace.PROPT
    val prop = Namespace.PROP
    val pqual = Namespace.PQUAL
    val rdfs = Namespace.RDFS

    val media = Var("media")
    val title = Var("mediaName")
    val channel = Var("channel")
    val thumbnail = Var("thumbnail")
    val duration = Var("duration")
    val publication = Var("publication")


    private fun getMediaQuery(first: Int, after: String?): DSL = DSL()
        .select(media, title, channel, thumbnail, duration, Aggregation("(STR(?publication) AS ?isoDate)"))
        .where(
            GraphPattern().add(
                BasicGraphPattern(media, propt("P1"), item("Q5"))
                    .add(rdfs("label"), title)
                    .add(propt("P7"), thumbnail)
                    .add(propt("P26"), duration)
                    .add(propt("P6"), publication)
                    .add(listOf(prop("P10"), pqual("P28"), rdfs("label")), channel)
            )
        )
        .orderBy("ASC($title)")
        .limit(first)
        .offset(after?.toInt() ?: 0).let { it ->
            println(it.build())
            it
        }

    override fun getMedia(first: Int, after: String?): List<Medium> = webClient
        .fetch<MediaQueryResult>(getMediaQuery(first, after))
        .results.bindings.map { row ->
            Medium(
                MediumId(row.media.value),
                row.mediaName.value,
                row.channel.value,
                UnresolvedThumbnail(URL(row.thumbnail.value)).resolve(),
                Duration.parse(row.duration.value),
                LocalDate.parse(row.isoDate.value.split("T").first())
            )
        }

}

@Serializable
data class WikidataObject(val type: String, val value: String)

@Serializable
data class Head(val vars: List<String>)

@Serializable
data class Row(
    val media: WikidataObject,
    val mediaName: WikidataObject,
    val channel: WikidataObject,
    val thumbnail: WikidataObject,
    val duration: WikidataObject,
    val isoDate: WikidataObject,
)

@Serializable
data class Bindings(val bindings: List<Row>)

@Serializable
data class MediaQueryResult(val head: Head, val results: Bindings)

