package com.borgnetzwerk.searchsnail.domain.model


enum class ValueType {
    String,
    Date,
    Duration,
    ISO639,
    Number,
}

@JvmInline
value class ISO639(val value: String) //need resolving

sealed class WikiData

class WikiDataResource(
    val iri: String,
    val label: String,
) : WikiData()

class WikiDataLiteral(
    val value: String,
    val type: ValueType,
    val lang: ISO639?,
) : WikiData()
