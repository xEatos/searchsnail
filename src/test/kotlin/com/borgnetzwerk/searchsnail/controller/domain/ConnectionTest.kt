package com.borgnetzwerk.searchsnail.controller.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.annotation.Testable

@Testable
class ConnectionTest : DescribeSpec({
    val totalCount = 20
    describe("Connection") {
        it("first=-1, after=null, totalCount=20 - Wrong"){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4),
                first = -1,
                after = null,
                totalCount = 20
            ) shouldBe null
        }
        it("first=20, after=-1, totalCount=20 - Wrong"){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4),
                first = 20,
                after = "-1",
                totalCount = 20
            ) shouldBe null
        }

        it("first=0, after=null, totalCount=20" ){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4),
                first = 0,
                after = null,
                totalCount = 20
            ) shouldBe ConnectionGraphQL(
                edges = listOf(),
                pageInfo = PageInfoGraphQL(
                    hasPreviousPage = false,
                    hasNextPage = true,
                    startCursor = null,
                    endCursor = null
                ),
                totalCount = 20,
            )
        }

        it("first=0, after=0, totalCount=20"){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4),
                first = 0,
                after = "0",
                totalCount = 20
            ) shouldBe ConnectionGraphQL(
                edges = listOf(),
                pageInfo = PageInfoGraphQL(
                    hasPreviousPage = true,
                    hasNextPage = true,
                    startCursor = null,
                    endCursor = null
                ),
                totalCount = 20,
            )
        }

        it("first=5, after=null, totalCount=20"){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4),
                first = 5,
                after = "0",
                totalCount = 20
            ) shouldBe ConnectionGraphQL(
                edges = listOf(
                    EdgeGraphQL(0, "1"),
                    EdgeGraphQL(1, "2"),
                    EdgeGraphQL(2, "3"),
                    EdgeGraphQL(3, "4"),
                    EdgeGraphQL(4, "5")),
                pageInfo = PageInfoGraphQL(
                    hasPreviousPage = true,
                    hasNextPage = true,
                    startCursor = "1",
                    endCursor = "5"
                ),
                totalCount = 20,
            )
        }

        it("first=5, after=19, totalCount=20"){
            ConnectionGraphQL.resolve(
                list = listOf<Int>(),
                first = 5,
                after = "19",
                totalCount = 20
            ) shouldBe null
        }
        it("first=5, after=18, totalCount=20"){
            ConnectionGraphQL.resolve(
                list = listOf(0),
                first = 5,
                after = "18",
                totalCount = 20
            ) shouldBe ConnectionGraphQL(
                edges = listOf(
                    EdgeGraphQL(0, "19")),
                pageInfo = PageInfoGraphQL(
                    hasPreviousPage = true,
                    hasNextPage = false,
                    startCursor = "19",
                    endCursor = "19"
                ),
                totalCount = 20,
            )
        }
        it("first=50, after=50, totalCount=20"){
            ConnectionGraphQL.resolve(
                list = listOf(0),
                first = 50,
                after = "50",
                totalCount = 20
            ) shouldBe null
        }

        it("first=50, after=2, totalCount=13"){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
                first = 50,
                after = "2",
                totalCount = 13
            ) shouldBe ConnectionGraphQL(
                edges = listOf(
                    EdgeGraphQL(0, "3"),
                    EdgeGraphQL(1, "4"),
                    EdgeGraphQL(2, "5"),
                    EdgeGraphQL(3, "6"),
                    EdgeGraphQL(4, "7"),
                    EdgeGraphQL(5, "8"),
                    EdgeGraphQL(6, "9"),
                    EdgeGraphQL(7, "10"),
                    EdgeGraphQL(8, "11"),
                    EdgeGraphQL(9, "12"),
                    ),
                pageInfo = PageInfoGraphQL(
                    hasPreviousPage = true,
                    hasNextPage = false,
                    startCursor = "3",
                    endCursor = "12"
                ),
                totalCount = 13,
            )
        }

        it("first=50, after=5, totalCount=5"){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4),
                first = 50,
                after = "5",
                totalCount = 5
            ) shouldBe null
        }

        it("first=50, after=null, totalCount=5"){
            ConnectionGraphQL.resolve(
                list = listOf(0, 1, 2, 3, 4),
                first = 50,
                after = null,
                totalCount = 5
            ) shouldBe ConnectionGraphQL(
                edges = listOf(
                    EdgeGraphQL(0, "0"),
                    EdgeGraphQL(1, "1"),
                    EdgeGraphQL(2, "2"),
                    EdgeGraphQL(3, "3"),
                    EdgeGraphQL(4, "4"),
                ),
                pageInfo = PageInfoGraphQL(
                    hasPreviousPage = false,
                    hasNextPage = false,
                    startCursor = "0",
                    endCursor = "4"
                ),
                totalCount = 5,
            )
        }


    }
})