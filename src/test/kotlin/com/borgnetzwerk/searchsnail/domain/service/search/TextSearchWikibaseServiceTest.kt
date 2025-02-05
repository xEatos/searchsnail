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
        it("test 1"){
            val textSearchWbService = TextSearchWikibaseService(
                GenericFetchService(),
                QueryServiceDispatcher()
            )

            val searchAnswer1 = textSearchWbService.getBatch("32", 0)
            println(searchAnswer1)
            val searchAnswer2 = textSearchWbService.getBatch("32", searchAnswer1.canContinue?.sroffset!!)
            println(searchAnswer2)
            val searchAnswer3 = textSearchWbService.getBatch("32", searchAnswer2.canContinue?.sroffset!!)
            println(searchAnswer3)

        }

        it("test 2"){

            val textSearchMiraService = TextSearchMirahezeService(
                GenericFetchService(),
                QueryServiceDispatcher()
            )

            val searchAnswer1 = textSearchMiraService.getBatch("leber", 0)
            println(searchAnswer1.iris)

        }

    }
})