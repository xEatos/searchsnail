package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.configuration.QueryServiceDispatcher
import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.annotation.Testable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.reactive.function.client.WebClient

// TODO how to mock Configuration in MediaRepository

// https://i1.ytimg.com/vi/Y9-GikUoNpk/mqdefault.jpg

@Testable
class MediaRepositoryTest : DescribeSpec({

    val mediaRepo = MediaRepository(QueryServiceDispatcher())

    describe("test request"){
        it("fetch first 10"){
            try {
                val res = mediaRepo.getMedia(10, "0")
                println("res:\n$res")
            } catch(e: Exception) {
                println(e.message)
            }


        }
    }

})