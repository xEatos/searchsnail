package com.borgnetzwerk.searchsnail.domain.model

import com.borgnetzwerk.searchsnail.utils.sparqlqb.*


typealias FilterString = String

data class FilterQueryPattern(
    val bgps: List<BasicGraphPattern>,
    val filterStrings: List<FilterString>,
)

data class FilterSelection(
    val filterId: ResolvedFilterId,
    val selections: MutableList<WikiData>,
)


