package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.configuration.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.domain.model.MediumId
import com.borgnetzwerk.searchsnail.domain.service.WebService
import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.annotation.Testable

@Testable
class MediumRepositoryTest : DescribeSpec({
    describe("MediumRepositoryTest") {
        it("queries"){
            val repository = MediumRepository(QueryServiceDispatcher())
            val res = repository.getMedium(MediumId("https://bnwiki.wikibase.cloud/entity/Q6"))
            println(res)

        }
    }
})