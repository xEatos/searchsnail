package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.*
import com.borgnetzwerk.searchsnail.domain.model.WikiData
import com.borgnetzwerk.searchsnail.domain.model.WikiDataLiteral
import com.borgnetzwerk.searchsnail.domain.model.WikiDataResource
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterOptionsService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class FilterOptionController(
    val filterOptionsService: FilterOptionsService
) {
    @QueryMapping
    fun filterOptions(): List<FilterOptionGraphQL> =
        filterOptionsService.getAllFilterOptions().map {
            FilterOptionGraphQL(
                it.filterId.value.toString(),
                it.filterType.toString(),
                it.label,
                it.options.map { wikiData ->  wikiData.toGraphQl() },
                it.group
                ) }
}
