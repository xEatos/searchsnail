package com.borgnetzwerk.searchsnail.domain.model


enum class ValueType {
    String,
    Date,
    Duration,
    ISO639,
    Number,
    Boolean,
}

@JvmInline
value class ISO639(val value: String) //need resolving

fun String?.toISO639(): ISO639? = this?.let { ISO639(it) }

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


fun <T> WikiData.tryInjectResource(callBack: (wr: WikiDataResource) -> T): T? =
    when (this) {
        is WikiDataResource -> callBack(this)
        is WikiDataLiteral -> null
    }

fun <T> WikiData.tryInjectLiteral(callBack: (wr: WikiDataLiteral) -> T): T? =
    when (this) {
        is WikiDataResource -> null
        is WikiDataLiteral -> callBack(this)
    }