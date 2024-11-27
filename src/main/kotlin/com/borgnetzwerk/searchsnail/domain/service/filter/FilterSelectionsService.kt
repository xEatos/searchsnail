package com.borgnetzwerk.searchsnail.domain.service.filter

import com.borgnetzwerk.searchsnail.domain.model.FilterQueryPattern
import com.borgnetzwerk.searchsnail.domain.model.FilterSelection
import com.borgnetzwerk.searchsnail.repository.internalapi.FilterSelectionRepository
import org.springframework.stereotype.Service

@Service
class FilterSelectionsService(
    val filterSelectionRepository: FilterSelections
) {
    fun convertToFilterQueryPattern(filterSelections: List<FilterSelection>): FilterQueryPattern =
        filterSelectionRepository.resolve(filterSelections)
}