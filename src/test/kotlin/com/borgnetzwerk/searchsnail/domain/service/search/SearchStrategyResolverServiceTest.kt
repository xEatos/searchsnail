package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.GenericFetchService
import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterSelectionsService
import com.borgnetzwerk.searchsnail.domain.service.media.MediaService
import com.borgnetzwerk.searchsnail.repository.internalapi.FilterSelectionRepository
import com.borgnetzwerk.searchsnail.repository.internalapi.MediaRepository
import io.kotest.core.spec.style.DescribeSpec
import org.junit.platform.commons.annotation.Testable

@Testable
class SearchStrategyResolverServiceTest: DescribeSpec({
    describe("SearchStrategyResolverTest") {
        it("only search Text") {
            val qsd = QueryServiceDispatcher()
            val strategy = SearchStrategyResolverService(
                searchMiraheze = TextSearchMirahezeService(GenericFetchService(),qsd),
                searchWikibase = TextSearchWikibaseService(GenericFetchService(), qsd),
                mediaService = MediaService(MediaRepository(qsd), FilterSelectionsService(FilterSelectionRepository()))
            )

            // 32, leber, ernährung
            val (indexedPageMap, foundFilters) = strategy.search(
                filters = listOf(FilterSelection(ResolvedFilterId.create(FreeText), mutableListOf(WikiDataLiteral("32", ValueType.String, null)))),
                offsetMap = mapOf( "miraheze" to 2, "wikibase" to 55),
                limit = 50
            )
            println(indexedPageMap.entries.map { (key, value) -> "$key: ${value.elements.map { "$it\n" }}" })
            println(indexedPageMap.entries.fold(0) { acc, (_, ie) -> acc + ie.elements.size })
            println(foundFilters)
        }
    }
})

@Testable
class SearchStrategyResolverServiceTestNoText: DescribeSpec({
    describe("NoText") {
        it("NoText") {
            val qsd = QueryServiceDispatcher()
            val strategy = SearchStrategyResolverService(
                searchMiraheze = TextSearchMirahezeService(GenericFetchService(),qsd),
                searchWikibase = TextSearchWikibaseService(GenericFetchService(), qsd),
                mediaService = MediaService(MediaRepository(qsd), FilterSelectionsService(FilterSelectionRepository()))
            )

            val (indexedPageMap, foundFilters) = strategy.search(
                filters = emptyList(), //listOf(FilterSelection(ResolvedFilterId.Companion.create(Category), mutableListOf(WikiDataResource(Namespace.ITEM("Q32").getFullIRI(), "Economics")))),
                mapOf("sparql" to 49),
                limit = 50
            )
            println(indexedPageMap.entries.map { (key, value) -> "$key: ${value.elements.map { "$it\n" }}" })
            println(indexedPageMap.entries.fold(0) { acc, (_, ie) -> acc + ie.elements.size })
            println(foundFilters)
        }
    }
})