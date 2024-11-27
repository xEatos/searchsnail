package com.borgnetzwerk.searchsnail.domain.service.filter

import com.borgnetzwerk.searchsnail.domain.model.FilterId
import com.borgnetzwerk.searchsnail.domain.model.FilterOption
import com.borgnetzwerk.searchsnail.domain.model.ResolvedFilterId
import org.springframework.stereotype.Service

@Service
class FilterOptionsService (
    val filterOptions: FilterOptions
){
    fun getAllFilterOptions(): List<FilterOption>{
        return ResolvedFilterId.getIds().mapNotNull { it -> filterOptions.getFilterOptionById(it) }
    }
}