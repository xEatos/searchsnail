package com.borgnetzwerk.searchsnail.repository.internalapi

import com.borgnetzwerk.searchsnail.domain.model.*
import com.borgnetzwerk.searchsnail.domain.service.QueryServiceDispatcher
import com.borgnetzwerk.searchsnail.repository.serialization.WikidataObject
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.annotation.Testable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClient
import java.net.URL
import java.time.Duration
import java.time.LocalDate

// TODO how to mock Configuration in MediaRepository

// https://i1.ytimg.com/vi/Y9-GikUoNpk/mqdefault.jpg

@Testable
class MediaRepositoryTest : DescribeSpec({

    val mediaRepo = MediaRepository(QueryServiceDispatcher())


    describe("test request") {
        it("fetch") {
            mediaRepo.getMedia(
                10, "0",
                queryPattern = FilterQueryPattern(emptyList(), emptyList())
            ) shouldContain LeanMedium(
                id = MediumId("https://bnwiki.wikibase.cloud/entity/Q6"),
                title = "32% aller Erwachsenen haben diese Krankheit. Du auch?",
                channel = "Doktor Whatson",
                thumbnail = UnresolvedThumbnail(URL("https://i.ytimg.com/vi_webp/FuV3ysSKOsw/maxresdefault.webp")).resolve(),
                duration = 787,
                publication = LocalDate.parse("2023-11-19"),
                type = "Video"
            )
        }
    }

})