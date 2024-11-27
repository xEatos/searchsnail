package com.borgnetzwerk.searchsnail.domain.service.filter

import com.borgnetzwerk.searchsnail.domain.model.FilterId
import com.borgnetzwerk.searchsnail.domain.model.FilterOption
import com.borgnetzwerk.searchsnail.domain.model.ResolvedFilterId
import com.borgnetzwerk.searchsnail.domain.model.WikiData

interface FilterOptions {
    fun getFilterOptionById(filterId : ResolvedFilterId) : FilterOption?
}