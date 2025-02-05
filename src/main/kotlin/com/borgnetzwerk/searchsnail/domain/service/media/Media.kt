package com.borgnetzwerk.searchsnail.domain.service.media

import com.borgnetzwerk.searchsnail.domain.model.FilterQueryPattern
import com.borgnetzwerk.searchsnail.domain.model.LeanMedium
import com.borgnetzwerk.searchsnail.utils.sparqlqb.IRI

interface Media {
    fun getMedia(first: Int, after: String?, queryPattern: FilterQueryPattern): List<LeanMedium>
    fun filterIRIs(iris: List<IRI>, queryPattern: FilterQueryPattern): List<LeanMedium>
}