package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.media.Media
import com.borgnetzwerk.searchsnail.repository.serialization.*
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
    val webClient: QueryServiceDispatcher,
) : Media {

    val item = Namespace.ITEM
    val propt = Namespace.PROPT
    val prop = Namespace.PROP
    val pqual = Namespace.PQUAL
    val rdfs = Namespace.RDFS

    val media = Var("media")
    val mediaType = Var("mediaType")
    val title = Var("mediaName")
    val channel = Var("channel")
    val thumbnail = Var("thumbnail")
    val duration = Var("duration")
    val publication = Var("publication")


    private fun getMediaQuery(first: Int, after: String?, queryPattern: FilterQueryPattern): DSL {
        val gp = GraphPattern()

        gp.add(
            BasicGraphPattern(media, rdfs("label"), title)
                .add(listOf(propt("P1"), rdfs("label")), mediaType)
                .add(propt("P7"), thumbnail)
                .add(propt("P26"), duration)
                .add(propt("P6"), publication)
                .add(listOf(prop("P10"), pqual("P28"), rdfs("label")), channel)
        )

        queryPattern.bgps.forEach { bgp ->
            gp.add(bgp)
        }

        queryPattern.filterStrings.forEach { filter ->
            gp.addFilter(filter)
        }

        return DSL()
            .isDistinct()
            .select(
                media,
                mediaType,
                title,
                channel,
                thumbnail,
                duration,
                Aggregation("(STR(?publication) AS ?isoDate)")
            )
            .where(gp)
            .orderBy("ASC($title)")
            .limit(first)
            .offset(after?.toInt() ?: 0).let { it ->
                println(it.build())
                it
            }
    }

    private fun getFilterIRIsQuery(iris: List<IRI>, queryPattern: FilterQueryPattern): DSL {
        val gp = GraphPattern()

        gp.addValues(media, iris.map { it -> IRI(it.getFullIRI()) })

        gp.add(
            BasicGraphPattern(media, rdfs("label"), title)
                .add(listOf(propt("P1"), rdfs("label")), mediaType)
                .add(propt("P7"), thumbnail)
                .add(propt("P26"), duration)
                .add(propt("P6"), publication)
                .add(listOf(prop("P10"), pqual("P28"), rdfs("label")), channel)
        )

        queryPattern.bgps.forEach { bgp ->
            gp.add(bgp)
        }

        queryPattern.filterStrings.forEach { filter ->
            gp.addFilter(filter)
        }

        return DSL()
            .isDistinct()
            .select(
                media,
                mediaType,
                title,
                channel,
                thumbnail,
                duration,
                Aggregation("(STR(?publication) AS ?isoDate)")
            )
            .where(gp)
            .orderBy("ASC($title)")
    }

    override fun getMedia(first: Int, after: String?, queryPattern: FilterQueryPattern): List<LeanMedium> = webClient
        .fetch<QueryResult<Row>>(getMediaQuery(first, after, queryPattern))
        .results.bindings.map { row ->
            LeanMedium(
                MediumId(row.media.value),
                row.mediaType.value,
                row.mediaName.value,
                LocalDate.parse(row.isoDate.value.split("T").first()),
                row.channel.value,
                UnresolvedThumbnail(URL(row.thumbnail.value)).resolve(),
                row.duration.value.toInt(),
            )
        }

    override fun filterIRIs(iris: List<IRI>, queryPattern: FilterQueryPattern): List<LeanMedium> {
        val res = webClient
            .fetch<QueryResult<Row>>(getFilterIRIsQuery(iris, queryPattern))
            .results.bindings.map { row ->
                LeanMedium(
                    MediumId(row.media.value),
                    row.mediaType.value,
                    row.mediaName.value,
                    LocalDate.parse(row.isoDate.value.split("T").first()),
                    row.channel.value,
                    UnresolvedThumbnail(URL(row.thumbnail.value)).resolve(),
                    row.duration.value.toInt(),
                )
            }
        return res
    }

}

@Serializable
data class Row(
    val media: WikidataObject,
    @Serializable(with = WikidataObjectTransformer::class)
    val mediaName: WikidataObject,
    @Serializable(with = WikidataObjectTransformer::class)
    val mediaType: WikidataObject,
    @Serializable(with = WikidataObjectTransformer::class)
    val channel: WikidataObject,
    val thumbnail: WikidataObject,
    val duration: WikidataObject,
    val isoDate: WikidataObject,
)

