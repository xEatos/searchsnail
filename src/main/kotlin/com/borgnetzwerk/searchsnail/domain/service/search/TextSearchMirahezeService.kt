package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.FilterSelection
import com.borgnetzwerk.searchsnail.domain.model.IriWithMetadata
import com.borgnetzwerk.searchsnail.domain.service.GenericFetchService
import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.repository.serialization.QueryResult
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObject
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObjectTransformer
import com.borgnetzwerk.searchsnail.utils.sparqlqb.*
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URL

@Service
class TextSearchMirahezeService(
    @Autowired
    val apiFetchService: GenericFetchService,
    @Autowired
    val sparqlQueryService: QueryServiceDispatcher,
) {

    private fun generateURL(searchText: String, offset: Int, limit: Int) =
        "https://borgnetzwerk.miraheze.org/w/api.php?" +
                "action=query" +
                "&format=json" +
                "&prop=" +
                "&list=search" +
                "&meta=" +
                "&indexpageids=1" +
                "&formatversion=2" +
                "&srsearch=$searchText" +
                "&srlimit=$limit" +
                "&sroffset=$offset" +
                "&srwhat=text" +
                "&srinfo=totalhits|rewrittenquery|suggestion" +
                "&srprop=" +
                "&srenablerewrites=1"

    private fun get(searchText: String, offset: Int, limit: Int): TextSearchMirahezeAnswer{
        val searchURLString = generateURL(searchText, offset, limit)

        val batch = apiFetchService.fetch<BatchInfo>(URL(searchURLString))

        val batchPageIds = batch.query.search.mapIndexed{ index, it ->
            Pair(it.pageid.toString(), mutableMapOf("index" to index.toString(), "origin" to "miraheze"))
        }.associateBy({ it -> it.first }, { it.second })

        val pageidVar = Var("pageid")
        val entityVar = Var("entity")

        val dsl = DSL()
            .isDistinct()
            .select(entityVar, pageidVar)
            .where(
                GraphPattern()
                    .addValues(pageidVar, batch.query.search.map { Literal("\"${it.pageid}\"") })
                    .add(
                        BasicGraphPattern(
                            entityVar,
                            listOf(Namespace.PROPT("P1"), Namespace.PROPT("P9")),
                            Namespace.ITEM("Q5")
                        )
                            .add(
                                listOf(Namespace.PROPT("P24"), Namespace.PROP("P20"), Namespace.PQUAL("P27")),
                                pageidVar
                            )
                    )
            )

        sparqlQueryService.fetch<QueryResult<EntityRow>>(dsl).results.bindings.forEach { it ->
            batchPageIds[it.pageid.value]?.put("iri", it.entity.value)
        }

        // batch.query.search and irisWithTypes doesn't have same order,  -> order irisWithTypes like in batch to be consistent with offset
        return TextSearchMirahezeAnswer(
            searchText,
            batchPageIds.values.filter { it ->
                it["iri"] !== null
            }.map { it ->
                IriWithMetadata(
                    IRI(it["iri"]!!),
                    mutableMapOf("index" to it["index"]!!, "origin" to "miraheze"),
                )
            },
            batch.`continue`
        )
    }

    fun getBatch(searchText: String, _sroffset: Int): TextSearchMirahezeAnswer{
        val limit = 50
        var sroffset = _sroffset
        val irisBatch = mutableListOf<IriWithMetadata>()
        do {
            val searchAnswer = this.get(searchText, sroffset, limit)
            irisBatch.addAll(searchAnswer.iris)
            if(searchAnswer.canContinue !== null) {
                sroffset = searchAnswer.canContinue.sroffset
            } else {
                return TextSearchMirahezeAnswer(
                    searchText,
                    irisBatch,
                    null
                )
            }
        } while (irisBatch.size < limit)

        return TextSearchMirahezeAnswer(
            searchText,
            irisBatch.slice(IntRange(0, limit +1)),
            BatchContinueInfo(sroffset - limit + irisBatch[limit].getMetadata("index")?.toInt()!!, "")
        )
    }

    @Serializable
    private data class EntityRow(
        val entity: WikidataObject,
        @Serializable(with = WikidataObjectTransformer::class)
        val pageid: WikidataObject
    )
}

data class TextSearchMirahezeAnswer(
    val searchText: String,
    val iris: List<IriWithMetadata>,
    val canContinue: BatchContinueInfo?,
)

