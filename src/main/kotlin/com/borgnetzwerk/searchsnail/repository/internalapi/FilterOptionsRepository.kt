package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.configuration.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterOptions
import com.borgnetzwerk.searchsnail.repository.serialization.QueryResult
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObject
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObjectTransformer
import com.borgnetzwerk.searchsnail.utils.sparqlqb.*
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class FilterOptionsRepository(
    @Autowired
    val webClient: QueryServiceDispatcher
) : FilterOptions {
    override fun getAllFilterIds(): List<FilterId> =
        listOf(
            "mediumType",
            "minDate",
            "maxDate",
            "category",
            "subcategory",
            "channel",
            "platform",
            "duration",
            "hasTranscript",
            "language",
            "subtitleLanguage"
        ).map { FilterId(it) }

    // TODO make FilterId to sealed object or enum
    override fun getOptionsById(filterId: FilterId): List<WikiData> =
        when (filterId.value) {
            "mediumType" -> getMediumTypOptions()
            "minDate" -> emptyList()
            "maxDate" -> emptyList()
            "category" -> getCategoryOptions()
            "subcategory" -> emptyList()
            "channel" -> emptyList()
            "platform" -> emptyList()
            "duration" -> emptyList()
            "hasTranscript" -> emptyList()
            "language" -> emptyList()
            "subtitleLanguage" -> emptyList()
            else -> emptyList()
        }

    override fun getFilterOptionById(filterId: FilterId): FilterOption? {
        return when (filterId.value) {
            "mediumType" -> FilterOption(filterId, FilterType.LabelSearch, "Medium", getMediumTypOptions(), "Media", DSL())
            "minDate" -> FilterOption(filterId, FilterType.Datepicker, "From", getMinDateOptions(), "Date Range", DSL())
            "maxDate" -> FilterOption(filterId, FilterType.Datepicker, "To", getMaxDateOptions(), "Date Range", DSL())
            "category" -> FilterOption(filterId, FilterType.LabelSearch, "Category", getCategoryOptions(), "Category", DSL())
            "subcategory" -> FilterOption(filterId, FilterType.LabelSearch, "Subcategory", getCategoryOptions(), "Category", DSL())
            "channel" -> null
            "platform" -> null
            "duration" -> null
            "hasTranscript" -> null
            "language" -> FilterOption(filterId, FilterType.LabelSearch, "Language", getLanguageOptions(), "Language", DSL())
            "subtitleLanguage" -> null
            else -> null
        }
    }


    @Serializable
    data class EntityRow(
        val entity: WikidataObject,
        @Serializable(with = WikidataObjectTransformer::class)
        val entityLabel: WikidataObject
    )

    @Serializable
    data class LiteralRow(
        @Serializable(with = WikidataObjectTransformer::class)
        val entityLabel: WikidataObject
    )

    val entity = Var("entity")
    val entityLabel = Var("entityLabel")

    // use this only if you want exclusively IRIs
    private fun getOnlyEntities(predicate: IRI, tail: Term): List<WikiDataResource> {
        val dsl = DSL()
            .select(entity, entityLabel)
            .where(
                GraphPattern()
                    .add(
                        BasicGraphPattern(entity, predicate, tail)
                            .add(Namespace.RDFS("label"), entityLabel)
                    )
            )
        return webClient.fetch<QueryResult<EntityRow>>(dsl).results.bindings.mapNotNull { row ->
            when (row.entity.type) {
                "uri" -> WikiDataResource(row.entity.value, row.entityLabel.value)
                else -> null
            }
        }
    }

    private fun getOnlyLiterals(predicate: IRI, type: ValueType): List<WikiDataLiteral> {
        return this.getOnlyLiterals(entityLabel, entityLabel, predicate, type)
    }

    private fun getOnlyLiterals(aggregationOrVar: Term, variable: Term, predicate: IRI, type: ValueType): List<WikiDataLiteral> {
        val dsl = DSL()
            .select(aggregationOrVar)
            .where(
                GraphPattern()
                    .add(
                        BasicGraphPattern(entity, predicate, variable)
                    )
            )
        return webClient.fetch<QueryResult<LiteralRow>>(dsl).results.bindings.mapNotNull { row ->
            when (row.entityLabel.type) {
                "literal" -> WikiDataLiteral(row.entityLabel.value, type, row.entityLabel.lang?.let { ISO639(it) })
                else -> null
            }
        }
    }

    private fun getMediumTypOptions(): List<WikiData> = getOnlyEntities(Namespace.PROPT("P9"), Namespace.ITEM("Q5"))

    private fun getCategoryOptions(): List<WikiData> = getOnlyEntities(Namespace.PROPT("P1"), Namespace.ITEM("Q10"))

    private fun getLanguageOptions(): List<WikiData> = getOnlyLiterals(Namespace.PROPT("P25"), ValueType.ISO639)


    private fun getMinDateOptions(): List<WikiData> =
        getOnlyLiterals(Aggregation("(STR(MIN(${Var("date")})) AS $entityLabel)"), Var("date"), Namespace.PROPT("P6"), ValueType.Date)

    private fun getMaxDateOptions(): List<WikiData> =
        getOnlyLiterals(Aggregation("(STR(MAX(${Var("date")})) AS $entityLabel)"), Var("date"), Namespace.PROPT("P6"), ValueType.Date)

}
