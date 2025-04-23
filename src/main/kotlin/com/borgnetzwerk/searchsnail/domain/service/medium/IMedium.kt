package com.borgnetzwerk.searchsnail.domain.service.medium

import com.borgnetzwerk.searchsnail.domain.model.Medium
import com.borgnetzwerk.searchsnail.domain.model.MediumId

interface IMedium {
    fun getMedium(id: MediumId): Medium?
}