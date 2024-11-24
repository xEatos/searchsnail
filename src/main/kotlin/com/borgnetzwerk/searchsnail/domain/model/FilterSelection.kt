package com.borgnetzwerk.searchsnail.domain.model

import com.borgnetzwerk.searchsnail.utils.sparqlqb.*

data class UnresolvedFilterSelection(
    val filterId: FilterId,
    val selections: List<WikiData>
) {

    // later always guarantee that "mediumType" is always included
    companion object {
        fun resolve(filterSelection: UnresolvedFilterSelection): SelectionQueryPattern? =
            when (filterSelection.filterId.value) {
                "mediumType" -> MediumTypeFilter(filterSelection.selections)
                "minDate" -> MinDateFilter(filterSelection.selections)
                "maxDate" -> MaxDateFilter(filterSelection.selections)
                "category" -> CategoryFilter(filterSelection.selections)
                "subcategory" -> null
                "channel" -> null
                "platform" -> null
                "duration" -> null
                "hasTranscript" -> null
                "language" -> LanguageFilter(filterSelection.selections)
                "subtitleLanguage" -> null
                else -> null
            }
    }
}

private typealias FilterString = String

interface SelectionQueryPattern {
    fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>>
}

// move this to service?
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

private fun empty(): Pair<List<BasicGraphPattern>, List<FilterString>> {
    return Pair(emptyList(), emptyList())
}

class MediumTypeFilter(val selections: List<WikiData>) : SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> =
        if (selections.isEmpty() || selections.size == 2) {
            Pair(
                listOf(
                    BasicGraphPattern(
                        Var("media"),
                        listOf(Namespace.PROPT("P1"), Namespace.PROPT("P9")),
                        Namespace.ITEM("Q5")
                    ),
                ), emptyList()
            )
        } else {
            selections[0].tryInjectResource { it ->
                Pair(
                    listOf(
                        BasicGraphPattern(Var("media"), Namespace.PROPT("P1"), IRI(it.iri))
                    ), emptyList()
                )
            } ?: empty()
        }
}

class LanguageFilter(val selections: List<WikiData>) : SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> =
        if (selections.isEmpty()) {
            empty()
        } else {
            val lang = Var("lang")
            val bgp = BasicGraphPattern(Var("media"), Namespace.PROPT("P8"), lang)
            val filter = selections.mapNotNull { it ->
                it.tryInjectLiteral { literal -> " ${literal.value} = ${lang} " }
            }.joinToString(separator = "||")
            if (filter.isNotEmpty()) {
                Pair(listOf(bgp), listOf(filter))
            } else empty()
        }
}

fun dateFilter(selection: WikiData, compareSymbol: String): Pair<List<BasicGraphPattern>, List<String>> {
    val pubDate = Var("publicationDate")
    val bgp = BasicGraphPattern(Var("media"), Namespace.PROPT("P6"), pubDate)
    val filter = selection.tryInjectLiteral { literal -> "$pubDate $compareSymbol ${literal.value}^^xsd:dateTime" } ?: ""
    return if (filter.isNotEmpty()) {
        Pair(listOf(bgp), listOf(filter))
    } else empty()
}

class MinDateFilter(val selections: List<WikiData>) : SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> =
        if (selections.isEmpty()) {
            empty()
        } else {
            dateFilter(selections[0], ">=")
        }
}

class MaxDateFilter(val selections: List<WikiData>) : SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> =
        if (selections.isEmpty()) {
            empty()
        } else {
            dateFilter(selections[0], "<=")
        }
}

class CategoryFilter(val selections: List<WikiData>) : SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> {
        val category = Var("category")
        val bgp = BasicGraphPattern(Var("media"), Namespace.PROPT("P4"), category)
        val filter = selections.mapNotNull { wikidata ->
            wikidata.tryInjectResource { resource -> " $category = ${resource.iri} "}
        }.joinToString(separator = "||")
        return if (filter.isNotEmpty()) {
            Pair(listOf(bgp), listOf(filter))
        } else empty()
    }
}
