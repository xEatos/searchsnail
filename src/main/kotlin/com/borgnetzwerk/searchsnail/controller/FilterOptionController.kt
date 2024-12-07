package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.FilterOptionGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiDataGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiDataLiteralGraphQL
import com.borgnetzwerk.searchsnail.controller.domain.WikiDataResourceGraphQL
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

fun WikiData.toGraphQl(): WikiDataGraphQL =
    when(this) {
        is WikiDataResource -> WikiDataResourceGraphQL(this.iri, this.label)
        is WikiDataLiteral -> WikiDataLiteralGraphQL(this.value, this.type, this.lang?.value)
    }