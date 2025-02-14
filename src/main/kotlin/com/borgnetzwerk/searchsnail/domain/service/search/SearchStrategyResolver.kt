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
                (wikibasePage.elements.plus(mirahezePage.elements)).map { IRI(it.value) }, filters
            ).associateBy { it.id.value }

            Pair(
                mapOf(
                    "wikibase" to wikibasePage.filterByMedia(filteredMergedElements),
                    "miraheze" to mirahezePage.filterByMedia(filteredMergedElements)
                ).filterDups(), foundFilters
            )

        } else {
            Pair(
                mapOf(
                    "sparql" to mediaService.getIndexedPage(
                        offsetMap["sparql"] ?: -1,
                        limit,
                        filters
                    )
                ), emptyList()
            )
        }
    }

    fun search(
        filters: List<FilterSelection>,
        offsetMap: Map<Provenance, Offset>,
        limit: Int,
    ): Pair<Map<Provenance, IndexedPage<LeanMedium>>, List<FilterSelection>> {
        val (indexedPageMap, foundFilters) = batchSearch(filters.getFreeText().toContent(), filters, offsetMap, limit)
        val mergedSize = indexedPageMap.entries.fold(0) { acc, (_, ie) -> acc + ie.elements.size }
        if (mergedSize >= limit
            || !indexedPageMap.entries.fold(false) { acc, (_, indexedPage) -> indexedPage.hasNextPage || acc }
        ) { // can't fetch more or don't need to
            return indexedPageMap.reduceTo(limit) to foundFilters
        } else { // fetch more
            val (nextIndexedPageMap, nextFoundFilters) = search(
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
                        indexedPage.elements.plus(nextIndexedPage.elements),
                        nextIndexedPage.hasNextPage,
                        indexedPage.hasPreviousPage,
                        indexedPage.offset,
                        indexedPage.limit + nextIndexedPage.limit
                    )
                } ?: indexedPage)
            }.reduceTo(limit) to foundFilters + nextFoundFilters
        }
    }

    private fun Map<Provenance, IndexedPage<LeanMedium>>.reduceTo(limit: Int): Map<Provenance, IndexedPage<LeanMedium>> {
        val pageMap = toMutableMap()
        while (pageMap.entries.fold(0) { acc, (_, ie) -> acc + ie.elements.size } > limit && limit > 0) {
            val provenanceWithMostElements = pageMap.provenanceWithMostElements()
            val page = pageMap[provenanceWithMostElements?.first]
            if (provenanceWithMostElements != null && page != null) {
                pageMap[provenanceWithMostElements.first] = page.copy(
                    elements = page.elements.slice(IntRange(0, provenanceWithMostElements.second -2)),
                )
            } else {
                break;
            }
        }
        return pageMap.toMap()
    }

    private fun Map<Provenance, IndexedPage<LeanMedium>>.provenanceWithMostElements(): Pair<Provenance, Int>? = this.entries
        .fold(null) { acc: Pair<Provenance, Int>?, (provenance, page) ->
            if (acc != null && acc.second > page.elements.size) {
                acc
            } else {
                Pair(provenance, page.elements.size)
            }
        }

    private fun Map<Provenance, IndexedPage<LeanMedium>>.filterDups(): Map<Provenance, IndexedPage<LeanMedium>> =
        this.toMutableMap().let { mutableMap ->
            mutableMap.entries.forEach() { (key, indexedPage) ->
                mutableMap[key] = IndexedPage(
                    indexedPage.elements
                        .fold(listOf()) { acc: List<IndexedElement<LeanMedium>>, element: IndexedElement<LeanMedium> ->
                            if (acc.notExists(element) { a, b -> a.value.id == b.value.id } && !mutableMap.existsInOther(
                                    key, element
                                )
                            ) {
                                acc.plus(element)
                            } else {
                                acc
                            }
                        },
                    indexedPage.hasNextPage,
                    indexedPage.hasPreviousPage,
                    indexedPage.offset,
                    indexedPage.limit
                )
            }
            mutableMap
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


    private fun List<FilterSelection>.getFreeText(): String? =
        this.find { it -> it.filterId.value is FreeText && it.selections.size == 1 }
            ?.selections
            ?.first()
            ?.tryInjectLiteral { it -> it.value }


}

@ConsistentCopyVisibility
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


