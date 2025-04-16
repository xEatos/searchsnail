package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.FilterSelection
import com.borgnetzwerk.searchsnail.domain.model.IriWithMetadata
import com.borgnetzwerk.searchsnail.domain.service.GenericFetchService
import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.repository.model.IndexedElement
import com.borgnetzwerk.searchsnail.repository.model.IndexedPage
import com.borgnetzwerk.searchsnail.repository.model.Page
import com.borgnetzwerk.searchsnail.repository.model.PageMap
import com.borgnetzwerk.searchsnail.repository.serialization.QueryResult
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObject
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObjectTransformer
import com.borgnetzwerk.searchsnail.utils.sparqlqb.*
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URL

@JvmInline
value class Pageid(val value: Int)

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


    fun getPage(searchText: String, offset: Int, limit: Int): Page<Pageid> =
        apiFetchService.fetch<BatchInfo>(URL(generateURL(searchText, offset + 1, limit))).let { batchInfo ->
            Page(
                offset = offset,
                total = batchInfo.query.searchinfo.totalhits,
                elements = batchInfo.query.search.map { Pageid(it.pageid) },
                hasNextPage = batchInfo.`continue` != null,
                hasPreviousPage = offset > -1
            )
        }

    fun getIndexedPage(searchText: String, offset: Int, limit: Int) = apiFetchService
        .fetch<BatchInfo>(URL(generateURL(searchText, offset + 1 /* sroffset is inclusive */, limit))).let { batch ->
            batch.query.search.mapIndexed { index, it ->
                IndexedElement(
                    offset + index + 1,
                    it.pageid,
                    "miraheze"
                )
            }.let { indexedPageIds ->
                IndexedPage(
                    getIRIByPageId(indexedPageIds.map { Pageid(it.value) }).associate { Pair(it.first.value, it.second) }
                        .let { pageIdToIriMap ->
                            println(pageIdToIriMap)
                            indexedPageIds.mapNotNull { pageId -> pageIdToIriMap[pageId.value]?.getFullIRI()?.run{ IndexedElement(pageId.index, this, pageId.provenance)} }
                        },
                    hasNextPage = batch.`continue` !== null,
                    hasPreviousPage = offset > -1,
                    offset = offset,
                    limit = limit
                )
            }
        }

    fun getNextIndexedPage(searchText: String, limit: Int, indexedPage: IndexedPage<String>): IndexedPage<String>? =
        if(indexedPage.hasNextPage){
            if(indexedPage.elements.isNotEmpty()){
                getIndexedPage(searchText, indexedPage.elements.last().index, limit)
            } else {
                getIndexedPage(searchText, indexedPage.offset + indexedPage.limit, limit)
            }
        } else {
            null
        }


    fun getMediaPage(searchText: String, offset: Int, limit: Int): PageMap<String, IRI> {
        val pageidPageMap = getPage(searchText, offset, limit).toPageMap("miraheze") { element -> element.value }
        val pageidWithIRIMap = getIRIByPageId(pageidPageMap.page.keys.map { it -> Pageid(it) }).associate {
            Pair(
                it.first,
                it.second.getFullIRI()
            )
        }
        val indexedIRIsPairs = pageidPageMap.mapValues { _, element ->
            pageidWithIRIMap[element]?.let { IRI(it) }
        }.page.map { (_, elements) ->
            elements.mapNotNull { element -> element.value?.let { iri -> Pair(iri, elements) } }
        }.flatten().mapNotNull { (iri, elements) ->
            val filteredElements = elements.filter { it -> it.value != null }
                .mapNotNull { it -> if (it.value != null) IndexedElement(it.index, it.value, it.provenance) else null }
            if (filteredElements.isNotEmpty()) {
                Pair(iri, filteredElements)
            } else {
                null
            }
        }
        val mutableMap = mutableMapOf<String, List<IndexedElement<IRI>>>()
        indexedIRIsPairs.forEach { (iri, elements) ->
            mutableMap[iri.getFullIRI()]?.let { currentElements ->
                mutableMap[iri.getFullIRI()] = elements + currentElements
            }
        }
        return PageMap(
            mutableMap,
            pageidPageMap.hasNextPage,
            pageidPageMap.hasNextPage
        )
    }

    private fun getIRIByPageId(pageids: List<Pageid>): List<Pair<Pageid, IRI>> =
        sparqlQueryService.fetch<QueryResult<EntityRow>>(getDSL(pageids)).results.bindings.map { it ->
            Pair(Pageid(it.pageid.value.toInt()), IRI(it.entity.value))
        }

    fun getDSL(pageids: List<Pageid>): DSL {
        val pageidVar = Var("pageid")
        val entityVar = Var("entity")

        return DSL()
            .isDistinct()
            .select(entityVar, pageidVar)
            .where(
                GraphPattern()
                    .addValues(pageidVar, pageids.map { Literal("\"${it.value}\"") })
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
    }


    private fun get(searchText: String, offset: Int, limit: Int): TextSearchMirahezeAnswer {
        val searchURLString = generateURL(searchText, offset, limit)

        val batch = apiFetchService.fetch<BatchInfo>(URL(searchURLString))

        val batchPageIds = batch.query.search.mapIndexed { index, it ->
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

    fun getBatch(searchText: String, _sroffset: Int, first: Int): TextSearchMirahezeAnswer {
        val limit = first
        var sroffset = _sroffset
        val irisBatch = mutableListOf<IriWithMetadata>()
        do {
            val searchAnswer = this.get(searchText, sroffset, limit)
            irisBatch.addAll(searchAnswer.iris)
            if (searchAnswer.canContinue !== null) {
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
            irisBatch.slice(IntRange(0, limit + 1)),
            BatchContinueInfo(
                irisBatch.slice(IntRange(0, limit - 1)).lastOrNull()?.getMetadata("index")?.toInt() ?: sroffset, ""
            )
        )
    }

    @Serializable
    private data class EntityRow(
        val entity: WikidataObject,
        @Serializable(with = WikidataObjectTransformer::class)
        val pageid: WikidataObject,
    )
}

data class TextSearchMirahezeAnswer(
    val searchText: String,
    val iris: List<IriWithMetadata>,
    val canContinue: BatchContinueInfo?,
)

