package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.GenericFetchService
import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.service.filter.FilterSelectionsService
import com.borgnetzwerk.searchsnail.domain.service.media.MediaService
import com.borgnetzwerk.searchsnail.repository.internalapi.FilterSelectionRepository
import com.borgnetzwerk.searchsnail.repository.internalapi.MediaRepository
import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.annotation.Testable

@Testable
class SearchStrategyResolverTest: DescribeSpec({
    describe("SearchStrategyResolverTest") {
        it("only search Text") {
            val qsd = QueryServiceDispatcher()
            val strategy = SearchStrategyResolver(
                searchMiraheze = TextSearchMirahezeService(GenericFetchService(),qsd),
                searchWikibase = TextSearchWikibaseService(GenericFetchService(), qsd),
                mediaService = MediaService(MediaRepository(qsd), FilterSelectionsService(FilterSelectionRepository()))
            )

            val (indexedPageMap, foundFilters) = strategy.search(
                filters = listOf(FilterSelection(ResolvedFilterId.create(FreeText), mutableListOf(WikiDataLiteral("32", ValueType.String, null)))),
                limit = 50
            )
            println(indexedPageMap)
            println(foundFilters)
        }
    }
})