package com.borgnetzwerk.searchsnail.domain.service.search

import com.borgnetzwerk.searchsnail.domain.model.FilterSelection
import com.borgnetzwerk.searchsnail.domain.service.GenericFetchService
import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.annotation.Testable

@Testable
class TextSearchWikibaseServiceTest : DescribeSpec({
    describe("TextSearchWikibaseServiceTest") {
        it("test Wikibase"){
            val textSearchWbService = TextSearchWikibaseService(
                GenericFetchService(),
                QueryServiceDispatcher()
            )

            val searchAnswer1 = textSearchWbService.getIndexedPageAndFilters("32", -1, 10)
            println(searchAnswer1.first.elements.map { "${it.index}: ${it.value}" })
            println(searchAnswer1.second)
            if(searchAnswer1.first.hasNextPage){
                val searchAnswer2 = textSearchWbService.getIndexedPageAndFilters("32", searchAnswer1.first.elements.last().index -1, 10)
                println(searchAnswer2.first.elements.map { "${it.index}: ${it.value} ${it.provenance}" })
                println(searchAnswer2.second)
                println("hasPrevious: ${searchAnswer2.first.hasPreviousPage}, next: ${searchAnswer2.first.hasNextPage}")
            }

        }

    }
})

@Testable
class TextSearchMirahezeServiceTest : DescribeSpec({
    describe("TextSearchWikibaseServiceTest") {
        it("test Miraheze"){
            val textSearchWbService = TextSearchMirahezeService(
                GenericFetchService(),
                QueryServiceDispatcher()
            )

            val searchAnswer1 = textSearchWbService.getIndexedPage("leber", -1, 1)
            val searchAnswer2 = textSearchWbService.getNextIndexedPage("leber", 10, searchAnswer1)
            println(searchAnswer1)
            println(searchAnswer2)
            println(textSearchWbService.getIndexedPage("leber", -1, 50))

        }

    }
})