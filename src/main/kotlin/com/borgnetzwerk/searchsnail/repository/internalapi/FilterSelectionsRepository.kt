package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.configuration.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterSelections
import com.borgnetzwerk.searchsnail.utils.sparqlqb.BasicGraphPattern
import com.borgnetzwerk.searchsnail.utils.sparqlqb.IRI
import com.borgnetzwerk.searchsnail.utils.sparqlqb.Namespace
import com.borgnetzwerk.searchsnail.utils.sparqlqb.Var
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class FilterSelectionRepository(
    @Autowired private val webClient: QueryServiceDispatcher
) : FilterSelections {
    override fun resolve(filterSelections: List<FilterSelection>): FilterQueryPattern {
        println("FilterSelections:$MediumTyp")
        val bgps = mutableListOf<BasicGraphPattern>()
        val filterStr = mutableListOf<FilterString>()

        filterSelections.forEach { selection ->
            getQueryPattern(selection)?.let {
                val (first, second) = it.getQueryPattern()
                bgps.addAll(first)
                filterStr.addAll(second)
            }
        }

        return FilterQueryPattern(bgps, filterStr)
    }

    private fun getQueryPattern(filterSelection: FilterSelection): SelectionQueryPattern? =
        when (filterSelection.filterId.value) {
            is MediumTyp -> MediumTypeFilter(filterSelection.selections)
            is MinDate -> MinDateFilter(filterSelection.selections)
            is MaxDate -> MaxDateFilter(filterSelection.selections)
            is Category -> CategoryFilter(filterSelection.selections)
            is Subcategory -> null
            is Channel -> null
            is Platform -> null
            is Duration -> null
            is HasTranscript -> null
            is Language -> LanguageFilter(filterSelection.selections)
            is SubtitleLanguage -> null
            else -> null
        }
}


interface SelectionQueryPattern {
    fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>>
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
                it.tryInjectLiteral { literal -> " \"${literal.value}\" = $lang " }
            }.joinToString(separator = "||")
            if (filter.isNotEmpty()) {
                Pair(listOf(bgp), listOf(filter))
            } else empty()
        }
}

private fun dateFilter(selection: WikiData, compareSymbol: String): Pair<List<BasicGraphPattern>, List<String>> {
    val pubDate = Var("publicationDate")
    val bgp = BasicGraphPattern(Var("media"), Namespace.PROPT("P6"), pubDate)
    val filter =
        selection.tryInjectLiteral { literal -> "$pubDate $compareSymbol \"${literal.value}\"^^xsd:dateTime" } ?: ""
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
            wikidata.tryInjectResource { resource -> " $category = <${resource.iri}> " }
        }.joinToString(separator = "||")
        return if (filter.isNotEmpty()) {
            Pair(listOf(bgp), listOf(filter))
        } else empty()
    }
}