package com.borgnetzwerk.searchsnail.domain.service.filter

import com.borgnetzwerk.searchsnail.domain.model.FilterQueryPattern
import com.borgnetzwerk.searchsnail.domain.model.FilterSelection

interface FilterSelections {
    fun resolve(filterSelections: List<FilterSelection>): FilterQueryPattern
}