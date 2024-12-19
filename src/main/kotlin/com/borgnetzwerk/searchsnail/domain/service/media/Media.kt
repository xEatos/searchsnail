package com.borgnetzwerk.searchsnail.domain.service.media

import com.borgnetzwerk.searchsnail.domain.model.FilterQueryPattern
import com.borgnetzwerk.searchsnail.domain.model.LeanMedium

interface Media {
    fun getMedia(first: Int, after: String?, queryPattern: FilterQueryPattern): List<LeanMedium>
}