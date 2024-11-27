package com.borgnetzwerk.searchsnail.domain.model

enum class FilterType {
    Datepicker,
    LabelSearch,
    TextInput,
    ValueSlider,
}

data class FilterOption (
    val filterId: ResolvedFilterId,
    val filterType: FilterType,
    val label: String,
    val options: List<WikiData>,
    val group: String
)