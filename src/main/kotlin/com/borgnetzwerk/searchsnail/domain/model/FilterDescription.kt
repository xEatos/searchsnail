package com.borgnetzwerk.searchsnail.domain.model

import com.borgnetzwerk.searchsnail.utils.sparqlqb.DSL
import com.borgnetzwerk.searchsnail.utils.sparqlqb.Var

sealed class FilterDescription(
    val id: FilterId,
    val label: String,
    val description: String,
    val type: FilterType,
    val targetClass: WikiDataResource? = null,
    private val optionQuery: () -> DSL,
    private val filterPatternQuery: (rootVar: Var, data: List<WikiData>) -> DSL
){

    fun getOptionQuery() = this.optionQuery()

    fun getFilterPatternQuery(rootVar: Var, data: List<WikiData>) = this.filterPatternQuery(rootVar, data)

    companion object{
        fun getByFilterId(id: FilterId): FilterDescription? = getByFilterId(id.toString())

        fun getByFilterId(id: String): FilterDescription? {
            // TODO
            return null
        }
    }
}