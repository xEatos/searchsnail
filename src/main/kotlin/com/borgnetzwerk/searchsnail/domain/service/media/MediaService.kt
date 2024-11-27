package com.borgnetzwerk.searchsnail.domain.service.media

import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterSelectionsService
import com.borgnetzwerk.searchsnail.utils.sparqlqb.BasicGraphPattern
import org.springframework.stereotype.Service

@Service
class MediaService(
    val mediaRepository: Media,
    val filterSelectionsService: FilterSelectionsService
) {
    // todo make it possible to add a filter
    fun getMedia(first: Int, after: String?, filters: List<FilterSelection>): List<Medium> {
        val filtersWithMediumTyp = if (!filters.map { it.filterId.value }.contains(MediumTyp)){
            filters + listOf(FilterSelection(ResolvedFilterId.create(MediumTyp), listOf()))
        } else { filters }

        val queryPattern = filterSelectionsService.convertToFilterQueryPattern(filtersWithMediumTyp)
        println(queryPattern)

        return mediaRepository.getMedia(first, after, queryPattern)
    }

}