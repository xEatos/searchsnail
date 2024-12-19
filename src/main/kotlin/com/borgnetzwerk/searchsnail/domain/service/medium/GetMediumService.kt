package com.borgnetzwerk.searchsnail.domain.service.medium

import com.borgnetzwerk.searchsnail.domain.model.Medium
import com.borgnetzwerk.searchsnail.domain.model.MediumId
import com.borgnetzwerk.searchsnail.repository.internalapi.MediumRepository
import org.springframework.stereotype.Service

@Service
class GetMediumService (
    val mediumRepository: IMedium
){
    fun getMedium(id: MediumId): Medium? = mediumRepository.getMedium(id)
}