package com.borgnetzwerk.searchsnail.domain.service.media

import com.borgnetzwerk.searchsnail.controller.domain.LeanMediumGraphQL
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterSelectionsService
import com.borgnetzwerk.searchsnail.repository.model.IndexedElement
import com.borgnetzwerk.searchsnail.repository.model.IndexedPage
import com.borgnetzwerk.searchsnail.utils.sparqlqb.BasicGraphPattern
import com.borgnetzwerk.searchsnail.utils.sparqlqb.IRI
import org.springframework.stereotype.Service

@Service
class MediaService(
    val mediaRepository: Media,
    val filterSelectionsService: FilterSelectionsService,
) {

    fun getMedia(first: Int, after: String?, filters: List<FilterSelection>): List<LeanMedium> {
        val filtersWithMediumTyp = if (!filters.map { it.filterId.value }.contains(MediumTyp)) {
            filters + listOf(FilterSelection(ResolvedFilterId.create(MediumTyp), mutableListOf()))
        } else {
            filters
        }

        val queryPattern = filterSelectionsService.convertToFilterQueryPattern(filtersWithMediumTyp)
        return mediaRepository.getMedia(first, after, queryPattern)
    }

    fun getIndexedPage(offset: Int, limit: Int, filters: List<FilterSelection>): IndexedPage<LeanMedium> {
        val filtersWithMediumTyp = if (!filters.map { it.filterId.value }.contains(MediumTyp)) {
            filters + listOf(FilterSelection(ResolvedFilterId.create(MediumTyp), mutableListOf()))
        } else {
            filters
        }

        val queryPattern = filterSelectionsService.convertToFilterQueryPattern(filtersWithMediumTyp)
        val mediaResult = mediaRepository.getMedia(limit +1, (offset +1).toString() /* sparql query offset is inclusive */, queryPattern)
        return IndexedPage(
            mediaResult.sliceTo(0, limit -1).mapIndexed { index, it -> IndexedElement(offset + index + 1, it, "sparql" ) },
            hasNextPage = mediaResult.size > limit,
            hasPreviousPage = false,
            offset,
            limit
        )
    }


    fun filterIRIs(iris: List<IRI>, filters: List<FilterSelection>): List<LeanMedium> {
        val filtersWithMediumTyp = if (!filters.map { it.filterId.value }.contains(MediumTyp)) {
            filters + listOf(FilterSelection(ResolvedFilterId.create(MediumTyp), mutableListOf()))
        } else {
            filters
        }

        val queryPattern = filterSelectionsService.convertToFilterQueryPattern(filtersWithMediumTyp)
        return mediaRepository.filterIRIs(iris, queryPattern)
    }

}

fun <T> List<T>.sliceTo(startInclusive: Int, endInclusive: Int): List<T> =
    this.slice(IntRange(startInclusive,if (this.size -1 < endInclusive ) this.size - 1 else endInclusive))
