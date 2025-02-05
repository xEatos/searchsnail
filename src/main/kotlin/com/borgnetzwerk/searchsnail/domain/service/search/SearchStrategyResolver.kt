package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.FilterSelection
import com.borgnetzwerk.searchsnail.domain.model.FreeText
import com.borgnetzwerk.searchsnail.domain.model.LeanMedium
import com.borgnetzwerk.searchsnail.domain.model.tryInjectLiteral
import com.borgnetzwerk.searchsnail.domain.service.media.MediaService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SearchStrategyResolver(
    @Autowired
    val searchWikibase: TextSearchWikibaseService,
    @Autowired
    val searchMiraheze: TextSearchMirahezeService,
    @Autowired
    val mediaService: MediaService
) {

    fun getMedia(first: Int, after: String?, filters: List<FilterSelection>): SearchStrategyAnswer {

        val searchTextFs = filters.find{ it -> it.filterId.value is FreeText && it.selections.size == 1 }
            ?.selections
            ?.first()
            ?.tryInjectLiteral { it -> it.value }
        return if(searchTextFs != null && searchTextFs.length > 0){
            println(searchTextFs)
            val searchWikibaseAnswer = searchWikibase.getBatch(searchTextFs, 0)
            val concatedFilters = (filters + searchWikibaseAnswer.newFilterSelections)
            val searchMirahezeAnswer = searchMiraheze.getBatch(searchTextFs, 0)
            val iris = (searchWikibaseAnswer.iris + searchMirahezeAnswer.iris).map { it.iri }
            println("wbSize: ${searchWikibaseAnswer.iris.size}, miraSize: ${searchMirahezeAnswer.iris.size}, $concatedFilters")
            println(iris)
            SearchStrategyAnswer(mediaService.filterIRIs(iris, concatedFilters), null, null)
        } else {
            println("No Text")
            SearchStrategyAnswer(mediaService.getMedia(first, after, filters), null, null)
        }
    }
}

data class SearchStrategyAnswer(
    val media: List<LeanMedium>,
    val canContinueWikibase: BatchContinueInfo?,
    val canContinueMiraheze: BatchContinueInfo?
)

