package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.controller.domain.BoxInfoGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiBatchAfterQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiBatchContinueGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiBatchInfoGraphQL
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.media.MediaService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.max

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


        /*
        return if (!searchTextFs.isNullOrEmpty()) {
            val searchWikibaseAnswer = searchWikibase.getBatch(searchTextFs, 0)
            val concatedFilters = (filters + searchWikibaseAnswer.newFilterSelections)
            val searchMirahezeAnswer = searchMiraheze.getBatch(searchTextFs, 0)
            val iris = (searchMirahezeAnswer.iris + searchWikibaseAnswer.iris).map { it.iri }
            println("wbSize: ${searchWikibaseAnswer.iris.size}, miraSize: ${searchMirahezeAnswer.iris.size}, $concatedFilters")
            println(iris)
            SearchStrategyAnswer(mediaService.filterIRIs(iris, concatedFilters), null, null)
        } else {
            println("No Text")
            SearchStrategyAnswer(mediaService.getMedia(first, after, filters), null, null)
        }

         */
    }

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


data class SearchStrategyAnswer(
    val media: List<LeanMedium>,
    val batchInfo: WikiBatchInfoGraphQL,
    val boxInfo: BoxInfoGraphQL,
)


