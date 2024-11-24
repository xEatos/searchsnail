package com.borgnetzwerk.searchsnail.domain.model

import com.borgnetzwerk.searchsnail.utils.sparqlqb.DSL

@JvmInline
value class FilterId(val value: String)

enum class FilterType {
    Datepicker,
    LabelSearch,
    TextInput,
    ValueSlider,
}

data class FilterOption (
    val filterId: FilterId,
    val filterType: FilterType,
    val label: String,
    val options: List<WikiData>,
    val group: String,
    val retrieveOptions: DSL
)

// queries to get the options for the FilterOption

// given a set of selections to extend the Media query
// every filterId extends the query with properties and FILTER commands -> Service