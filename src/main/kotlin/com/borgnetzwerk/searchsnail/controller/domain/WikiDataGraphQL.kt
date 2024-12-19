package com.borgnetzwerk.searchsnail.controller.domain

import com.borgnetzwerk.searchsnail.domain.model.ValueType
import com.borgnetzwerk.searchsnail.domain.model.WikiData
import com.borgnetzwerk.searchsnail.domain.model.WikiDataLiteral
import com.borgnetzwerk.searchsnail.domain.model.WikiDataResource


data class WikiDataResourceGraphQL(
    val id: String,
    val label: String,
) : WikiDataGraphQL()

data class WikiDataLiteralGraphQL(
    val value: String,
    val type: ValueType,
    val lang: String?,
) : WikiDataGraphQL()

sealed class WikiDataGraphQL


fun WikiData.toGraphQl(): WikiDataGraphQL =
    when(this) {
        is WikiDataResource -> WikiDataResourceGraphQL(this.iri, this.label)
        is WikiDataLiteral -> WikiDataLiteralGraphQL(this.value, this.type, this.lang?.value)
    }