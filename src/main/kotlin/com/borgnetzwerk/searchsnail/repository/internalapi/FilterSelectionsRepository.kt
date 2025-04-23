package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterSelections
import com.borgnetzwerk.searchsnail.utils.sparqlqb.*
import org.springframework.stereotype.Repository

@Repository
class FilterSelectionRepository() : FilterSelections {
    override fun resolve(filterSelections: List<FilterSelection>): FilterQueryPattern {
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
            is Channel -> ChannelFilter(filterSelection.selections)
            is Platform -> PlatformFilter(filterSelection.selections)
            is Duration -> DurationFilter(filterSelection.selections)
            is HasTranscript -> HasTranscriptFilter(filterSelection.selections)
            is Language -> LanguageFilter(filterSelection.selections)
            is SubtitleLanguage -> SubtitleLanguageFilter(filterSelection.selections)
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
                    )
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

/*
SELECT ?channelName ?categoryStatements ?category ?subCategory
WHERE {
  ?media propt:P1 item:Q5 ;
         rdfs:label ?channelName ;
         prop:P4 ?categoryStatements  .
  ?categoryStatements pstat:P4 ?category .
  OPTIONAL {
    ?categoryStatements pqual:P5 ?subCategory ;
  }
}
TODO: need to support options to make this work
*/
class CategoryFilter(val selections: List<WikiData>) : SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> {
        val category = Var("category")
        val subCategory = Var("subCategory")
        val categoryStatements = Var("categoryStatements")

        val bgps = listOf( BasicGraphPattern(Var("media"), Namespace.PROP("P4"), categoryStatements),
        BasicGraphPattern(categoryStatements, Namespace.PSTAT("P4"), category))

        val filter = listOf(
            selections.mapNotNull { wikidata ->
                wikidata.tryInjectResource { resource -> " $category = <${resource.iri}> " }
            }.joinToString(separator = "||"),
        )

        return if (filter.isNotEmpty()) {
            Pair(bgps, filter)
        } else empty()
    }
}

class ChannelFilter(val selections: List<WikiData>): SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> {
        val channelEntity = Var("channelEntity")
        val bgp = BasicGraphPattern(Var("media"), listOf(Namespace.PROP("P10"), Namespace.PQUAL("P28")), channelEntity)
        val filter = selections.mapNotNull { wikidata ->
            wikidata.tryInjectResource { resource -> " $channelEntity = <${resource.iri}> " }
        }.joinToString(separator = "||")
        return if (filter.isNotEmpty()) {
            Pair(listOf(bgp), listOf(filter))
        } else empty()
    }
}

class PlatformFilter(val selections: List<WikiData>): SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> {
        val platformEntity = Var("platformEntity")
        val bgp = BasicGraphPattern(Var("media"), listOf(Namespace.PROP("P10"), Namespace.PQUAL("P14")), platformEntity)
        val filter = selections.mapNotNull { wikidata ->
            wikidata.tryInjectResource { resource -> " $platformEntity = <${resource.iri}> " }
        }.joinToString(separator = "||")
        return if (filter.isNotEmpty()) {
            Pair(listOf(bgp), listOf(filter))
        } else empty()
    }
}

// todo make duration to int in sec instead of ISO8601
class DurationFilter(val selections: List<WikiData>) : SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> {
        val duration = Var("duration")
        val bgp = BasicGraphPattern(Var("media"), Namespace.PROPT("P26"), duration)
        return if (selections.size == 2) {
            Pair(listOf(bgp), listOf("${selections[0].tryInjectLiteral { it.value }} <= xsd:integer($duration) && xsd:integer($duration) <= ${selections[1].tryInjectLiteral { it.value }}"))
        } else {
            empty()
        }
    }
}

class HasTranscriptFilter(val selections: List<WikiData>): SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> {
        val transcript = Var("transcript")
        val bgp = BasicGraphPattern(Var("media"), Namespace.PROPT("P24"), transcript)
        return Pair(listOf(bgp), emptyList())
    }
}

class SubtitleLanguageFilter(val selections: List<WikiData>): SelectionQueryPattern {
    override fun getQueryPattern(): Pair<List<BasicGraphPattern>, List<FilterString>> {
        val subtitleLang = Var("subtitleLang")
        val bgp = BasicGraphPattern(Var("media"), Namespace.PROPT("P25"), subtitleLang)
        val filter = selections.mapNotNull { wikidata ->
            wikidata.tryInjectLiteral { literal -> " $subtitleLang = \"${literal.value}\"" }
        }.joinToString(separator = "||")
        return if (filter.isNotEmpty()) {
            Pair(listOf(bgp), listOf(filter))
        } else empty()
    }
}