package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.controller.domain.BoxInfoGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiBatchAfterQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiBatchContinueGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiBatchInfoGraphQL
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.media.MediaService
import com.borgnetzwerk.searchsnail.repository.model.IndexedElement
import com.borgnetzwerk.searchsnail.repository.model.IndexedPage
import com.borgnetzwerk.searchsnail.repository.model.optionalPlus
import com.borgnetzwerk.searchsnail.utils.sparqlqb.IRI
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.max

typealias Provenance = String
typealias Offset = Int

@Service
class SearchStrategyResolver(
    @Autowired
    val searchWikibase: TextSearchWikibaseService,
    @Autowired
    val searchMiraheze: TextSearchMirahezeService,
    @Autowired
    val mediaService: MediaService,
) {

    fun getMedia(first: Int, after: WikiBatchAfterQL, filters: List<FilterSelection>): SearchStrategyAnswer {

        val searchTextFs = filters.getFreeText()

        /*
        return if (!searchTextFs.isNullOrEmpty()) { // search string
            val searchWikibaseAnswer = searchWikibase.getBatch(searchTextFs, after.wikibase, first)
            val searchMirahezeAnswer = searchMiraheze.getBatch(searchTextFs, after.miraheze, first)

            val mergedIris = mutableListOf<IriWithMetadata>();
            for (i in 0..max(searchWikibaseAnswer.iris.size, searchMirahezeAnswer.iris.size)) {
                if (i < searchWikibaseAnswer.iris.size) {
                    mergedIris.add(searchWikibaseAnswer.iris[i])
                }
                if (i < searchMirahezeAnswer.iris.size) {
                    mergedIris.add(searchMirahezeAnswer.iris[i])
                }
            }

            val iris = mergedIris.slice(IntRange(0, first - 1)) // todo remapping with results from mediaService (indices)

            val mediaResult = mediaService.filterIRIs(iris.map { it.iri }, filters)
            SearchStrategyAnswer(
                mediaResult,
                WikiBatchInfoGraphQL(
                    WikiBatchContinueGraphQL(
                        after.wikibase,
                        iris.findLastIndexInMeta(after.wikibase, "origin", "wikibase"),
                        searchWikibaseAnswer.canContinue?.`continue` !== null
                    ),
                    WikiBatchContinueGraphQL(
                        after.miraheze,
                        iris.findLastIndexInMeta(after.wikibase, "origin", "miraheze"),
                        searchWikibaseAnswer.canContinue?.`continue` !== null
                    ),
                    WikiBatchContinueGraphQL(-1, -1, false),
                ),
                BoxInfoGraphQL(searchWikibaseAnswer.newFilterSelections)
            )
        } else { // no search string
            val sparqlWikiResult = mediaService.getMedia(first + 1, after.sparql.toString(), emptyList())
            SearchStrategyAnswer(
                sparqlWikiResult.subList(0, first),
                WikiBatchInfoGraphQL(
                    WikiBatchContinueGraphQL(-1, -1, false),
                    WikiBatchContinueGraphQL(-1, -1, false),
                    WikiBatchContinueGraphQL(
                        after.sparql,
                        after.sparql + sparqlWikiResult.size,
                        sparqlWikiResult.size == first + 1
                    ),
                ),
                BoxInfoGraphQL(emptyList())
            )
        }



         */
        return if (!searchTextFs.isNullOrEmpty()) {
            val searchWikibaseAnswer = searchWikibase.getBatch(searchTextFs, 0, 50)
            val concatedFilters = (filters + searchWikibaseAnswer.newFilterSelections)
            val searchMirahezeAnswer = searchMiraheze.getBatch(searchTextFs, 0, 50)
            val iris = (searchMirahezeAnswer.iris + searchWikibaseAnswer.iris).map { it.iri }
            println("wbSize: ${searchWikibaseAnswer.iris.size}, miraSize: ${searchMirahezeAnswer.iris.size}, $concatedFilters")
            println(iris)
            SearchStrategyAnswer(
                mediaService.filterIRIs(iris, concatedFilters), WikiBatchInfoGraphQL(
                    WikiBatchContinueGraphQL(0, 50, false),
                    WikiBatchContinueGraphQL(0, 50, false),
                    WikiBatchContinueGraphQL(0, 50, false),
                ), BoxInfoGraphQL(emptyList())
            )
        } else {
            println("No Text")
            SearchStrategyAnswer(
                mediaService.getMedia(first, after.toString(), filters), WikiBatchInfoGraphQL(
                    WikiBatchContinueGraphQL(0, 50, false),
                    WikiBatchContinueGraphQL(0, 50, false),
                    WikiBatchContinueGraphQL(0, 50, false),
                ), BoxInfoGraphQL(emptyList())
            )
        }


    }

    // only gives offset of pages where hasNextPage is true
    private fun batchSearch(
        searchText: ContentString?,
        filters: List<FilterSelection>,
        offsetMap: Map<Provenance, Offset> = mapOf("sparql" to -1),
        limit: Int,
    ): Pair<Map<Provenance, IndexedPage<LeanMedium>>, List<FilterSelection>> {
        return if (searchText != null) {
            val (wikibasePage, foundFilters) = searchWikibase.getIndexedPageAndFilters(
                searchText.value,
                offsetMap["wikibase"] ?: -1,
                limit
            )
            val mirahezePage = searchMiraheze.getIndexedPage(searchText.value, offsetMap["miraheze"] ?: -1, limit)

            val filteredMergedElements = mediaService.filterIRIs(
                (wikibasePage.elements.optionalPlus(mirahezePage.elements)).map { IRI(it.value) }, filters
            ).associateBy { it.id.value }

            println("before filtering and dups filtering:\nwikibase: $wikibasePage\nmiraheze: $mirahezePage")
            // TODO fix filtering error
            val a = Pair(
                mapOf(
                    "wikibase" to wikibasePage.filterByMedia(filteredMergedElements),
                    "miraheze" to mirahezePage.filterByMedia(filteredMergedElements)
                ).filterDups(), foundFilters
            )
            println("after filtering and dups filtering:\nwikibase: ${a.first["wikibase"]}\nmiraheze: ${a.first["miraheze"]}")
            a
        } else {
            offsetMap["sparql"]?.let { sparqlOffset ->
                Pair(
                    mapOf(
                        "sparql" to mediaService.getIndexedPage(
                            sparqlOffset,
                            limit,
                            filters
                        )
                    ), emptyList()
                )
            }
                ?: Pair(
                    mapOf(
                        "sparql" to IndexedPage(
                            emptyList(),
                            hasNextPage = false,
                            hasPreviousPage = false,
                            -1,
                            -1
                        )
                    ), emptyList()
                )
        }
    }

    fun search(
        filters: List<FilterSelection>,
        offsetMap: Map<Provenance, Offset> = mapOf("sparql" to -1),
        limit: Int,
    ): Pair<Map<Provenance, IndexedPage<LeanMedium>>, List<FilterSelection>> {
        val (indexedPageMap, foundFilters) = batchSearch(filters.getFreeText().toContent(), filters, offsetMap, limit)
        val mergedSize = indexedPageMap.entries.fold(0) { acc, (_, ie) -> acc + ie.elements.size }

        // we got a complete page and maybe more || we don't have a nextPage anywhere
        if (mergedSize >= limit
            || indexedPageMap.entries.fold(false) { acc, (_, indexedPage) -> indexedPage.hasNextPage || acc }
        ) {
            return indexedPageMap to foundFilters
        } else {
            // fetch more
            val (nextIndexedPageMap, nextFoundFilters) = batchSearch(
                filters.getFreeText().toContent(),
                filters,
                indexedPageMap.entries.associate { (provenance, indexedPage) ->
                    Pair(
                        provenance,
                        indexedPage.elements.lastOrNull()?.let { it.index } ?: indexedPage.offset + indexedPage.limit)
                },
                limit
            )
            return indexedPageMap.entries.associate { (provenance, indexedPage) ->
                Pair(provenance, nextIndexedPageMap[provenance]?.let { nextIndexedPage ->
                    IndexedPage(
                        indexedPage.elements.optionalPlus(nextIndexedPage.elements),
                        nextIndexedPage.hasNextPage,
                        indexedPage.hasPreviousPage,
                        indexedPage.offset,
                        indexedPage.limit + nextIndexedPage.limit
                    )
                } ?: indexedPage)
            } to foundFilters + nextFoundFilters
        }
    }

    private fun Map<Provenance, IndexedPage<LeanMedium>>.filterDups(): Map<Provenance, IndexedPage<LeanMedium>> =
        this.entries.associate { (key, indexedElements) ->
            Pair(
                key,
                indexedElements.run {
                    IndexedPage(
                        elements.fold(listOf()) { acc: List<IndexedElement<LeanMedium>>, element: IndexedElement<LeanMedium> ->
                            if (acc.notExists(element) { a, b -> a.value.id == b.value.id }) acc.optionalPlus(element) else acc
                        }
                            .filter { indexedElement ->
                                this@filterDups.existsInOther(key, indexedElement)
                            },
                        hasNextPage,
                        hasPreviousPage,
                        offset,
                        limit
                    )
                }
            )
        }

    inline private fun <T> List<T>.notExists(value: T, comparator: (a: T, b: T) -> Boolean): Boolean =
        this.fold(0) { acc, otherValue -> if (comparator(otherValue, value)) acc + 1 else acc } == 0

    private fun <T> List<T>.existsAtLeastOnce(value: T, comparator: (a: T, b: T) -> Boolean): Boolean =
        !this.notExists(value, comparator)

    private fun <T> Map<Provenance, IndexedPage<T>>.existsInOther(
        targetProvenance: Provenance,
        value: IndexedElement<T>,
    ): Boolean =
        entries.fold(false) { acc, (provenance, page) ->
            acc || (provenance != targetProvenance && page.elements.existsAtLeastOnce(value) { a, b -> a.value == b.value })
        }

    private fun IndexedPage<String>.filterByMedia(filteredMergedElements: Map<String, LeanMedium>): IndexedPage<LeanMedium> =
        IndexedPage(
            elements.mapNotNull { wbElement ->
                filteredMergedElements[wbElement.value]?.let { medium ->
                    IndexedElement(wbElement.index, medium, wbElement.provenance)
                }
            },
            hasNextPage,
            hasPreviousPage,
            offset,
            limit
        )

    private fun <T> List<IndexedElement<T>>.alternatingMerge(other: List<IndexedElement<T>>): List<IndexedElement<T>> =
        (0..max(this.size, other.size)).fold(listOf()) { acc, index ->
            acc.optionalPlus(
                if (index < this.size) {
                    acc.optionalPlus(this[index].let { IndexedElement(it.index, it.value, it.provenance) })
                } else null
            ).optionalPlus(
                if (index < other.size) {
                    acc.optionalPlus(other[index].let { IndexedElement(it.index, it.value, it.provenance) })
                } else null
            )
        }

    private fun <T> List<IndexedElement<T>>.optionalPlus(value: IndexedElement<T>?): List<IndexedElement<T>> =
        if (value != null) this.plus(value) else this

    private fun List<FilterSelection>.hasAtLeastOneFacetedFilter(): Boolean =
        this.find { it -> it.filterId.value is FreeText } != null

    private fun List<FilterSelection>.getFreeText(): String? =
        this.find { it -> it.filterId.value is FreeText && it.selections.size == 1 }
            ?.selections
            ?.first()
            ?.tryInjectLiteral { it -> it.value }

    private fun List<IriWithMetadata>.findLastIndexInMeta(default: Int, key: String, value: String) =
        this.lastOrNull() { it -> it.getMetadata(key) !== value }?.getMetadata("index")?.toInt() ?: default


}

data class ContentString private constructor(val value: String) {
    companion object {
        fun create(value: String): ContentString? =
            if (value.isNotEmpty() && value.isNotBlank()) ContentString(value) else null
    }
}

fun String?.toContent() = this?.let { ContentString.create(this) }

data class SearchStrategyAnswer(
    val media: List<LeanMedium>,
    val batchInfo: WikiBatchInfoGraphQL,
    val boxInfo: BoxInfoGraphQL,
)


