package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.configuration.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.WebService
import com.borgnetzwerk.searchsnail.domain.service.medium.IMedium
import com.borgnetzwerk.searchsnail.repository.serialization.QueryResult
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObject
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObjectTransformer
import com.borgnetzwerk.searchsnail.utils.sparqlqb.*
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.net.URL
import java.time.LocalDate

@Repository
data class MediumRepository(
    @Autowired
    final val webService: WebService,
) : IMedium {

    val webClient = webService.sparqlQueryService()
    val textClient = webService.fetchTextService()

    val item = Namespace.ITEM
    val propt = Namespace.PROPT
    val prop = Namespace.PROP
    val pqual = Namespace.PQUAL
    val pstat = Namespace.PSTAT
    val rdfs = Namespace.RDFS

    val type = Var("type")
    val title = Var("title")
    val publicationDate = Var("publicationDate")
    val thumbnail = Var("thumbnail")
    val categories = Var("categories")
    val duration = Var("duration")
    val captionId = Var("captionId")

    val subtitleLanguageList = Var("subtitleLanguageList")
    val subtitleLanguages = Var("subtitleLanguages")

    val inLanguages = Var("inLanguages")
    val inLanguageList = Var("inLanguageList")

    val url = Var("url")
    val host = Var("host")
    val hostName = Var("hostName")
    val channel = Var("channel")
    val channelName = Var("channelName")

    val sectionsStatements = Var("sectionsStatements")
    val transcriptLanguage = Var("transcriptLanguage")
    val sectionTitle = Var("sectionTitle")
    val transcriptId = Var("transcriptId")
    val startTimestamp = Var("startTimestamp")
    val endTimestamp = Var("endTimestamp")

    override fun getMedium(id: MediumId): Medium? {
        val medium = IRI(id.value)

        val optionals = mapOf<Term, Term>(
            propt("P1") to BlankNodeTail(rdfs("label"), type),
            propt("P6") to Var("publication"),
            propt("P7") to thumbnail,
            propt("P4") to BlankNodeTail(rdfs("label"), categories),
            propt("P26") to duration,
            propt("P29") to captionId,
        )

        val gp = GraphPattern().add(
            BasicGraphPattern(medium, rdfs("label"), title)
                .add(
                    listOf(Namespace.PROPT("P1"), Namespace.PROPT("P9")),
                    Namespace.ITEM("Q5")
                )
        )

        optionals.forEach { it -> gp.addOptional(GraphPattern().add(BasicGraphPattern(medium, it.key, it.value))) }

        val mainQuery = DSL().select(
            type,
            title,
            thumbnail,
            categories,
            duration,
            captionId,
            Aggregation("(STR(?publication) AS $publicationDate)")
        ).where(gp)

        val subtitleLanguagesQuery =
            DSL().select(Aggregation("(group_concat($subtitleLanguages;separator=\"|\") as $subtitleLanguageList)"))
                .where(GraphPattern().add(BasicGraphPattern(medium, propt("P25"), subtitleLanguages)))

        val inLanguagesQuery =
            DSL().select(Aggregation("(group_concat($inLanguages;separator=\"|\") as $inLanguageList)"))
                .where(GraphPattern().add(BasicGraphPattern(medium, propt("P8"), inLanguages)))


        val referencesQuery = DSL().select(
            url,
            host,
            hostName,
            channel,
            channelName,
        ).where(
            GraphPattern().add(
                BasicGraphPattern(
                    medium, prop("P10"), BlankNodeTail(pstat("P10"), url)
                )
            ).addOptional(
                GraphPattern().add(
                    BasicGraphPattern(
                        medium, prop("P10"), BlankNodeTail(pqual("P14"), host)
                    )
                ).add(BasicGraphPattern(host, rdfs("label"), hostName))
            ).addOptional(
                GraphPattern().add(
                    BasicGraphPattern(
                        medium, prop("P10"), BlankNodeTail(pqual("P28"), channel)
                    )
                ).add(BasicGraphPattern(channel, rdfs("label"), channelName))
            )
        )

        val transcriptsQuery = DSL().select(
            transcriptLanguage,
            sectionTitle,
            transcriptId,
            startTimestamp,
            endTimestamp
        ).where(
            GraphPattern().add(
                BasicGraphPattern(
                    medium, propt("P24"), BlankNodeTail(propt("P8"), transcriptLanguage)
                        .with(prop("P20"), sectionsStatements)
                )
            ).add(
                BasicGraphPattern(
                    sectionsStatements, pstat("P20"), sectionTitle
                )
            )
                .addOptional(
                    GraphPattern().add(
                        BasicGraphPattern(sectionsStatements, pqual("P27"), transcriptId)
                            .add(pqual("P18"), startTimestamp)
                            .add(pqual("P19"), endTimestamp)
                    )
                )
        )
        val mainQueryResponse = webClient.fetch<QueryResult<MainQueryRow>>(mainQuery)
        if (mainQueryResponse.results.bindings.isEmpty()) {
            return null
        }

        val mainResponse = mainQueryResponse.results.bindings.first()
        val type = mainQueryResponse.results.bindings.fold(emptyList<WikidataObject?>()) { acc, it ->
            acc + listOf(it.type)
        }.find { it -> it?.value == "Video" || it?.value == "Podcast" }
        val categoryList = mainQueryResponse.results.bindings.fold(listOf<WikiData>()) { acc, binding ->
            binding.categories?.let { it ->
                acc + listOf(WikiDataLiteral(it.value, ValueType.String, it.lang.toISO639()))
            } ?: acc
        }

        // TODO length test if first one exists
        val inLanguagesQueryResponse =
            webClient.fetch<QueryResult<InLanguageRow>>(inLanguagesQuery)
        val inLanguageList =
            inLanguagesQueryResponse.results.bindings.first().inLanguageList?.value?.split('|').let { it ->
                if (it?.first()?.isEmpty() == true) {
                    null
                } else {
                    it
                }
            }

        val subtitleLanguagesQueryResponse =
            webClient.fetch<QueryResult<SubtitleLanguageRow>>(subtitleLanguagesQuery)
        val subtitleLanguageList = if (subtitleLanguagesQueryResponse.results.bindings.isEmpty()) {
            null
        } else {
            subtitleLanguagesQueryResponse.results.bindings.first().subtitleLanguageList?.value?.split('|')
        }.let { it ->
            if (it?.first()?.isEmpty() == true) {
                null
            } else {
                it
            }
        }


        // add host, hostname, url
        val referencesQueryResponse = webClient.fetch<QueryResult<ReferenceRow>>(referencesQuery)
        val referencesResponse = if (referencesQueryResponse.results.bindings.isEmpty()) {
            null
        } else {
            referencesQueryResponse.results.bindings.first()
        }

        val transcriptsQueryResponse =
            webClient.fetch<QueryResult<TranscriptRow>>(transcriptsQuery)
        val transcriptsResponse = transcriptsQueryResponse.results.bindings
        val transcriptPerLanguage = transcriptsResponse.groupBy { it ->
            it.transcriptLanguage?.value
        }.map { entry ->
            Transcript(
                entry.key.toISO639(),
                entry.value.map { row ->
                    TranscriptChapter(
                        row.transcriptId?.let { TranscriptId(it.value) },
                        Chapter(
                            row.startTimestamp?.let { start -> MediumDuration.of(start.value.toInt()) },
                            row.endTimestamp?.let { end -> MediumDuration.of(end.value.toInt()) },
                            row.sectionTitle?.value ?: ""
                        )
                    )
                }
            )
        }

        println(transcriptPerLanguage)

        return Medium(
            id = id,
            type = type?.value,
            title = mainResponse.title.value,
            caption = mainResponse.captionId?.let { it ->
                textClient.fetchText(it.value)
                    ?.let { it1 -> Caption(it1.text, it1.id) }
            },
            publication = mainResponse.publicationDate?.value?.split("T")?.first()?.let { LocalDate.parse(it) },
            channel = if (referencesResponse?.channel?.value !== null && referencesResponse.channelName?.value !== null) WikiDataResource(
                referencesResponse.channel.value,
                referencesResponse.channelName.value
            ) else null,
            thumbnail = mainResponse.thumbnail?.let {
                try {
                    UnresolvedThumbnail(URL(it.value)).resolve()
                } catch (e: Exception) {
                    null
                }
            },
            categories = categoryList,
            transcripts = transcriptPerLanguage,
            languages = inLanguageList?.mapNotNull { it.toISO639() },
            subtitleLanguages = subtitleLanguageList?.mapNotNull { it.toISO639() },
            duration = mainResponse.duration?.value?.toInt()
        )
    }


}

@Serializable
data class MainQueryRow(
    @Serializable(with = WikidataObjectTransformer::class)
    val type: WikidataObject? = null,
    @Serializable(with = WikidataObjectTransformer::class)
    val title: WikidataObject,
    val publicationDate: WikidataObject? = null,
    val thumbnail: WikidataObject? = null,
    @Serializable(with = WikidataObjectTransformer::class)
    val categories: WikidataObject? = null,
    val duration: WikidataObject? = null,
    val captionId: WikidataObject? = null,
)

@Serializable
data class InLanguageRow(
    val inLanguageList: WikidataObject? = null,
)

@Serializable
data class SubtitleLanguageRow(
    val subtitleLanguageList: WikidataObject? = null,
)

@Serializable
data class ReferenceRow(
    val url: WikidataObject,
    val host: WikidataObject? = null,
    @Serializable(with = WikidataObjectTransformer::class)
    val hostName: WikidataObject? = null,
    val channel: WikidataObject? = null,
    @Serializable(with = WikidataObjectTransformer::class)
    val channelName: WikidataObject? = null,
)

@Serializable
data class TranscriptRow(
    @Serializable(with = WikidataObjectTransformer::class)
    val transcriptLanguage: WikidataObject? = null,
    @Serializable(with = WikidataObjectTransformer::class)
    val sectionTitle: WikidataObject? = null,
    val transcriptId: WikidataObject? = null,
    val startTimestamp: WikidataObject? = null,
    val endTimestamp: WikidataObject? = null,
)