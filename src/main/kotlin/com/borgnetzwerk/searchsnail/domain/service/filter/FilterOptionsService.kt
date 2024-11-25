package com.borgnetzwerk.searchsnail.domain.service.filter

import com.borgnetzwerk.searchsnail.domain.model.FilterId
import com.borgnetzwerk.searchsnail.domain.model.FilterOption
import org.springframework.stereotype.Service

@Service
class FilterOptionsService (
    val filterOptions: FilterOptions
){
    fun getAllFilterOptions(): List<FilterOption>{
        return filterOptions.getAllFilterIds().mapNotNull { it -> filterOptions.getFilterOptionById(it) }
    }
}