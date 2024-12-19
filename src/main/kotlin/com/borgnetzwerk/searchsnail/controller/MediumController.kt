package com.borgnetzwerk.searchsnail.controller

import com.borgnetzwerk.searchsnail.controller.domain.MediumGraphQL
import com.borgnetzwerk.searchsnail.domain.model.Medium
import com.borgnetzwerk.searchsnail.domain.model.MediumId
import com.borgnetzwerk.searchsnail.domain.service.medium.GetMediumService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class MediumController(
    val mediumService: GetMediumService
) {

    @QueryMapping
    fun medium(@Argument mediumId: String): MediumGraphQL? {
        return mediumService.getMedium(MediumId(mediumId))?.toGraphQL()
    }
}