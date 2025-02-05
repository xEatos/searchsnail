package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.GenericFetchService
import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.repository.internalapi.ChannelFilter
import com.borgnetzwerk.searchsnail.repository.serialization.QueryResult
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObject
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObjectTransformer
import com.borgnetzwerk.searchsnail.utils.sparqlqb.*
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URL

@Service
class TextSearchWikibaseService(
    @Autowired
    val apiFetchService: GenericFetchService,
    @Autowired
    val sparqlQueryService: QueryServiceDispatcher,
) {

    private fun generateURL(searchText: String, offset: Int, limit: Int) = "https://bnwiki.wikibase.cloud/w/api.php?" +
            "action=query" +
            "&format=json" +
            "&prop=" +
            "&list=search" +
            "&meta=" +
            "&srsearch=${searchText}" +
            "&srnamespace=120" +
            "&srlimit=${limit}" +
            "&sroffset=${offset}" +
            "&srqiprofile=wikibase" +
            "&srinfo=totalhits|suggestion|rewrittenquery" +
            "&srprop=" +
            "&srenablerewrites=1"

    private fun get(
        searchText: String,
        offset: Int = 0,
        limit: Int = 50,
    ): TextSearchWikibaseAnswer {
        val searchURLString = generateURL(searchText, offset, limit)
        val mutableFilterSelections = mutableListOf<FilterSelection>()
        val mutableMediaInstances = mutableListOf<IriWithMetadata>()

        val batch = apiFetchService.fetch<BatchInfo>(URL(searchURLString))
        val batchIRIs = batch.query.search
            .mapIndexed { index, it ->
                IriWithMetadata(
                    Namespace.ITEM(it.title.split(':')[1]),
                    mutableMapOf("index" to index.toString(), "origin" to "Wikibase")
                )
            }.associateBy({ it -> it.iri.getFullIRI() }, { it })

        val entityVar = Var("entity")
        val entityTypeVar = Var("entityType")
        val entityLabelVar = Var("entityLabel")

        val dsl = DSL()
            .select(entityVar, entityTypeVar, entityLabelVar)
            .where(
                GraphPattern()
                    .addValues(entityVar, batchIRIs.keys.toList().map { IRI(it) })
                    .add(
                        BasicGraphPattern(entityVar, Namespace.PROPT("P1"), entityTypeVar)
                            .add(Namespace.RDFS("label"), entityLabelVar)
                    )
            )

        sparqlQueryService.fetch<QueryResult<EntityRow>>(dsl).results.bindings.forEach { it ->
            batchIRIs[IRI(it.entity.value).getFullIRI()]?.let { iriWithMetadata ->
                iriWithMetadata.setMetadata("label", it.entityLabel.value)
                iriWithMetadata.setMetadata("type", it.entityType.value.split('/').last())
            }
        }

        batchIRIs.values.forEach { it ->
            val type = it.getMetadata("type")
            val label = it.getMetadata("label")
            if (type !== null && label !== null) {
                when (type) {
                    "Q3" -> {
                        mutableFilterSelections.appendOrSetFilterSelection(
                            Channel,
                            WikiDataResource(it.iri.getFullIRI(), label)
                        )
                    }

                    "Q4" -> {
                        mutableMediaInstances.add(it)
                    }

                    "Q7" -> {
                        mutableFilterSelections.appendOrSetFilterSelection(
                            Platform,
                            WikiDataResource(it.iri.getFullIRI(), label)
                        )
                    }

                    "Q10" -> {
                        mutableFilterSelections.appendOrSetFilterSelection(
                            Category,
                            WikiDataResource(it.iri.getFullIRI(), label)
                        )
                    }
                }
            }
        }

        return TextSearchWikibaseAnswer(
            searchText,
            mutableMediaInstances,
            mutableFilterSelections,
            batch.`continue`
        )
    }

    fun getBatch(searchText: String, _sroffset: Int): TextSearchWikibaseAnswer {
        val limit = 50
        var sroffset = _sroffset
        val irisBatch = mutableListOf<IriWithMetadata>()
        val fsBatch = mutableListOf<FilterSelection>()
        do {
            val searchAnswer = this.get(searchText, sroffset, limit)
            irisBatch.addAll(searchAnswer.iris)
            fsBatch.addAll(searchAnswer.newFilterSelections)
            if (searchAnswer.canContinue !== null) {
                sroffset = searchAnswer.canContinue.sroffset
            } else {
                return TextSearchWikibaseAnswer(
                    searchText,
                    irisBatch,
                    fsBatch,
                    null
                )
            }
        } while (irisBatch.size < limit)

        return TextSearchWikibaseAnswer(
            searchText,
            irisBatch.slice(IntRange(0, limit - 1)),
            fsBatch,
            BatchContinueInfo(sroffset - limit + irisBatch[limit - 1].getMetadata("index")?.toInt()!! + 1, "")
        )
    }

    @Serializable
    private data class EntityRow(
        val entity: WikidataObject,
        val entityType: WikidataObject,
        @Serializable(with = WikidataObjectTransformer::class)
        val entityLabel: WikidataObject,
    )

}

private fun MutableList<FilterSelection>.appendOrSetFilterSelection(filterId: FilterId, data: WikiDataResource) {
    val foundSelection = this.find { fs -> fs.filterId.value.toString() == filterId.toString() }
    if (foundSelection != null) {
        foundSelection.selections.add(data)
    } else {
        this.add(
            FilterSelection(
                ResolvedFilterId.create(filterId),
                mutableListOf(data)
            )
        )
    }
}

data class TextSearchWikibaseAnswer(
    val searchText: String,
    val iris: List<IriWithMetadata>,
    val newFilterSelections: MutableList<FilterSelection>,
    val canContinue: BatchContinueInfo?,
) {
    override fun toString(): String {
        return "searchText: ${searchText},\n" +
                "sroffset: ${canContinue?.sroffset}\n" +
                "iris: [${iris.joinToString(", ") { it.iri.getFullIRI() }}]\n" +
                "filter: ${newFilterSelections.joinToString("\n") { "${it.filterId}: [${it.selections.joinToString(", ") { wd -> wd.toString() }}]" }}"
    }
}


@Serializable
data class BatchInfo(
    val `continue`: BatchContinueInfo? = null,
    val query: BatchQuery,
)

@Serializable
data class BatchContinueInfo(
    val sroffset: Int,
    val `continue`: String,
)

@Serializable
data class BatchQuery(
    val searchinfo: BatchSearchInfo,
    val search: List<SearchResult>,
)

@Serializable
data class BatchSearchInfo(
    val totalhits: Int,
)

@Serializable
data class SearchResult(
    val ns: Int,
    val title: String,
    val pageid: Int,
)

