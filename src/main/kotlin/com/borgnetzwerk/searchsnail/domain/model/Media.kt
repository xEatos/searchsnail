package com.borgnetzwerk.searchsnail.domain.model

interface Media {
    fun getMedia(first: Int, after: String?): List<Medium>
}