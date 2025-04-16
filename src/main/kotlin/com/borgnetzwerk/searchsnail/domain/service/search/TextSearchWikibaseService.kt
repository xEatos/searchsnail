package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.*
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


data class IRIwithType(
    val iri: IRI,
    val label: String,
    val type: IRI,
)

@Service
class TextSearchWikibaseService(
    @Autowired
    val apiFetchService: GenericFetchService,
    @Autowired
    val sparqlQueryService: QueryServiceDispatcher,
) {

    private fun generateURL(searchText: String, offset: Int, limit: Int) = "https://bnwiki.wikibase.cloud/w/api.php?" +
            "action=query" +
            "&format=json" +
            "&prop=" +
            "&list=search" +
            "&meta=" +
            "&srsearch=${searchText}" +
            "&srnamespace=120" +
            "&srlimit=${limit}" +
            "&sroffset=${offset}" +
            "&srqiprofile=wikibase" +
            "&srinfo=totalhits|suggestion|rewrittenquery" +
            "&srprop=" +
            "&srenablerewrites=1"


    fun getPage(searchText: String, offset: Int, limit: Int): Page<IRI> = apiFetchService
        .fetch<BatchInfo>(URL(generateURL(searchText, offset + 1 /* sroffset is inclusive */, limit)))
        .let { batchInfo ->
            Page(
                offset = offset,
                total = batchInfo.query.searchinfo.totalhits,
                elements = batchInfo.query.search.map { IRI(Namespace.ITEM(it.title.split(':')[1]).getFullIRI()) },
                hasNextPage = batchInfo.`continue` !== null,
                hasPreviousPage = offset > -1
            )
        }


    fun getIndexedPageAndFilters(
        searchText: String,
        offset: Int,
        limit: Int,
    ): Pair<IndexedPage<String>, List<FilterSelection>> = apiFetchService
        .fetch<BatchInfo>(URL(generateURL(searchText, offset + 1 /* sroffset is inclusive */, limit))).run {
            query.search.mapIndexed { index, it ->
                IndexedElement(
                    offset + index + 1,
                    Namespace.ITEM(it.title.split(':')[1]).getFullIRI(),
                    "wikibase"
                )
            }.let { indexedIriStrings ->
                getIRIsWithType(indexedIriStrings.map { IRI(it.value) }).let { irisWithType ->
                    indexedIriStrings.map { indexedElement ->
                        irisWithType.find { iriWithType -> iriWithType.iri.getFullIRI() == indexedElement.value }
                            ?.let { found ->
                                IndexedElement(indexedElement.index, found, indexedElement.provenance)
                            }
                    }
                }
            }.run {
                Pair(
                    IndexedPage(
                        filter { it != null && it.value.toFilterSelection() == null }
                            .mapNotNull { it ->
                                it?.let { IndexedElement(it.index, it.value.iri.getFullIRI(), it.provenance) }
                            },
                        hasNextPage = `continue` !== null,
                        hasPreviousPage = offset > -1,
                        offset = offset,
                        limit = limit
                    ),
                    filter { it != null && it.value.toFilterSelection() != null }
                        .mapNotNull { it?.value?.toFilterSelection() }
                )
            }
        }

    fun getNextIndexedPageAndFilters(searchText: String, limit: Int, indexedPage: IndexedPage<String>): Pair<IndexedPage<String>, List<FilterSelection>>? =
        if(indexedPage.hasNextPage){
            if(indexedPage.elements.isNotEmpty()){
                getIndexedPageAndFilters(searchText, indexedPage.elements.last().index, limit)
            } else {
                getIndexedPageAndFilters(searchText, indexedPage.offset + indexedPage.limit, limit)
            }
        } else {
            null
        }



    fun getMediaPageAndFilters(
        searchText: String,
        offset: Int,
        limit: Int,
    ): Pair<PageMap<String, IRI>, List<FilterSelection>> {
        val wikibasePage = getPage(searchText, offset, limit).toPageMap("wikibase") { it -> it.getFullIRI() }
        val irisWithTypeMap = getLabelAndTypeOfIRIs(wikibasePage.page.keys.map { IRI(it) }).associate { it ->
            Pair(it.iri.getFullIRI(), it)
        }

        return Pair(
            wikibasePage.filterByKey { key ->
                irisWithTypeMap[key]?.let { iriWithType -> iriWithType.toFilterSelection() == null } ?: true
            },
            irisWithTypeMap.values.filter { it.toFilterSelection() != null }
                .mapNotNull { it -> it.toFilterSelection() })
    }


    private fun IRIwithType.toFilterSelection(): FilterSelection? =
        when (type?.getFullIRI()) {
            Namespace.ITEM("Q3").getFullIRI() -> ResolvedFilterId.create(Channel)
            Namespace.ITEM("Q7").getFullIRI() -> ResolvedFilterId.create(Platform)
            Namespace.ITEM("Q10").getFullIRI() -> ResolvedFilterId.create(Category)
            else -> null
        }?.let { resolvedFilterId ->
            FilterSelection(
                ResolvedFilterId.create(resolvedFilterId.value),
                mutableListOf(WikiDataResource(iri.getFullIRI(), label ?: "<empty>"))
            )
        }

    fun getIRIsWithType(entities: List<IRI>): List<IRIwithType> = sparqlQueryService
        .fetch<QueryResult<EntityRow>>(getTypesOfIRIsDSL(entities))
        .results
        .bindings.map { it -> IRIwithType(IRI(it.entity.value), it.entityLabel.value, IRI(it.entityType.value)) }

    fun getLabelAndTypeOfIRIs(entities: List<IRI>): List<IRIwithType> {
        return sparqlQueryService.fetch<QueryResult<EntityRow>>(getTypesOfIRIsDSL(entities)).results.bindings.let { bindings ->
            entities.map { entity ->
                bindings.find { row -> row.entity.value == entity.getFullIRI() }?.let { foundrow ->
                    IRIwithType(entity, foundrow.entityLabel.value, IRI(foundrow.entityType.value))
                } ?: IRIwithType(entity, "", IRI(""))
            }
        }
    }

    private fun getTypesOfIRIsDSL(iris: List<IRI>): DSL {
        val entityVar = Var("entity")
        val entityTypeVar = Var("entityType")
        val entityLabelVar = Var("entityLabel")

        return DSL()
            .select(entityVar, entityTypeVar, entityLabelVar)
            .where(
                GraphPattern()
                    .addValues(entityVar, iris)
                    .add(
                        BasicGraphPattern(entityVar, Namespace.PROPT("P1"), entityTypeVar)
                            .add(Namespace.RDFS("label"), entityLabelVar)
                    )
            )
    }

    private fun get(
        searchText: String,
        offset: Int = 0,
        limit: Int = 50,
    ): TextSearchWikibaseAnswer {
        val searchURLString = generateURL(searchText, offset, limit)
        val mutableFilterSelections = mutableListOf<FilterSelection>()
        val mutableMediaInstances = mutableListOf<IriWithMetadata>()

        val batch = apiFetchService.fetch<BatchInfo>(URL(searchURLString))
        val batchIRIs = batch.query.search
            .mapIndexed { index, it ->
                IriWithMetadata(
                    Namespace.ITEM(it.title.split(':')[1]),
                    mutableMapOf("index" to (index + offset).toString(), "origin" to "wikibase")
                )
            }.associateBy({ it -> it.iri.getFullIRI() }, { it })

        val entityVar = Var("entity")
        val entityTypeVar = Var("entityType")
        val entityLabelVar = Var("entityLabel")

        val dsl = DSL()
            .select(entityVar, entityTypeVar, entityLabelVar)
            .where(
                GraphPattern()
                    .addValues(entityVar, batchIRIs.keys.toList().map { IRI(it) })
                    .add(
                        BasicGraphPattern(entityVar, Namespace.PROPT("P1"), entityTypeVar)
                            .add(Namespace.RDFS("label"), entityLabelVar)
                    )
            )

        sparqlQueryService.fetch<QueryResult<EntityRow>>(dsl).results.bindings.forEach { it ->
            batchIRIs[IRI(it.entity.value).getFullIRI()]?.let { iriWithMetadata ->
                iriWithMetadata.setMetadata("label", it.entityLabel.value)
                iriWithMetadata.setMetadata("type", it.entityType.value.split('/').last())
            }
        }

        batchIRIs.values.forEach { it ->
            val type = it.getMetadata("type")
            val label = it.getMetadata("label")
            if (type !== null && label !== null) {
                when (type) {
                    "Q3" -> {
                        mutableFilterSelections.appendOrSetFilterSelection(
                            Channel,
                            WikiDataResource(it.iri.getFullIRI(), label)
                        )
                    }

                    "Q4" -> {
                        mutableMediaInstances.add(it)
                    }

                    "Q7" -> {
                        mutableFilterSelections.appendOrSetFilterSelection(
                            Platform,
                            WikiDataResource(it.iri.getFullIRI(), label)
                        )
                    }

                    "Q10" -> {
                        mutableFilterSelections.appendOrSetFilterSelection(
                            Category,
                            WikiDataResource(it.iri.getFullIRI(), label)
                        )
                    }
                }
            }
        }

        return TextSearchWikibaseAnswer(
            searchText,
            mutableMediaInstances,
            mutableFilterSelections,
            batch.`continue`
        )
    }

    fun getBatch(searchText: String, _sroffset: Int, first: Int): TextSearchWikibaseAnswer {
        val limit = first
        var sroffset = _sroffset
        val irisBatch = mutableListOf<IriWithMetadata>()
        val fsBatch = mutableListOf<FilterSelection>()
        do {
            val searchAnswer = this.get(searchText, sroffset, limit)
            irisBatch.addAll(searchAnswer.iris)
            fsBatch.addAll(searchAnswer.newFilterSelections)
            if (searchAnswer.canContinue !== null) {
                sroffset = searchAnswer.canContinue.sroffset
            } else {
                return TextSearchWikibaseAnswer(
                    searchText,
                    irisBatch,
                    fsBatch,
                    null
                )
            }
        } while (irisBatch.size < limit)

        return TextSearchWikibaseAnswer(
            searchText,
            irisBatch.slice(IntRange(0, limit - 1)),
            fsBatch,
            BatchContinueInfo(
                irisBatch.slice(IntRange(0, limit - 1)).lastOrNull()?.getMetadata("index")?.toInt() ?: sroffset, ""
            )
        )
    }

    @Serializable
    private data class EntityRow(
        val entity: WikidataObject,
        val entityType: WikidataObject,
        @Serializable(with = WikidataObjectTransformer::class)
        val entityLabel: WikidataObject,
    )


}

private fun MutableList<FilterSelection>.appendOrSetFilterSelection(filterId: FilterId, data: WikiDataResource) {
    val foundSelection = this.find { fs -> fs.filterId.value.toString() == filterId.toString() }
    if (foundSelection != null) {
        foundSelection.selections.add(data)
    } else {
        this.add(
            FilterSelection(
                ResolvedFilterId.create(filterId),
                mutableListOf(data)
            )
        )
    }
}

data class TextSearchWikibaseAnswer(
    val searchText: String,
    val iris: List<IriWithMetadata>,
    val newFilterSelections: MutableList<FilterSelection>,
    val canContinue: BatchContinueInfo?,
) {
    override fun toString(): String {
        return "searchText: ${searchText},\n" +
                "sroffset: ${canContinue?.sroffset}\n" +
                "iris: [${iris.joinToString(", ") { it.iri.getFullIRI() }}]\n" +
                "filter: ${newFilterSelections.joinToString("\n") { "${it.filterId}: [${it.selections.joinToString(", ") { wd -> wd.toString() }}]" }}"
    }
}


@Serializable
data class BatchInfo(
    val `continue`: BatchContinueInfo? = null,
    val query: BatchQuery,
)

@Serializable
data class BatchContinueInfo(
    val sroffset: Int,
    val `continue`: String,
)

@Serializable
data class BatchQuery(
    val searchinfo: BatchSearchInfo,
    val search: List<SearchResult>,
)

@Serializable
data class BatchSearchInfo(
    val totalhits: Int,
)

@Serializable
data class SearchResult(
    val ns: Int,
    val title: String,
    val pageid: Int,
)

