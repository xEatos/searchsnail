package com.borgnetzwerk.searchsnail.domain.service.filter

import com.borgnetzwerk.searchsnail.domain.model.FilterId
import com.borgnetzwerk.searchsnail.domain.model.FilterOption
import com.borgnetzwerk.searchsnail.domain.model.WikiData

interface FilterOptions {
    fun getOptionsByFilterId(filterId: FilterId) : List<WikiData>
    fun getAllFilterIds() : List<FilterId>
    fun getAllFilterWithoutOptions(filterId : FilterId) : List<FilterOption>
}