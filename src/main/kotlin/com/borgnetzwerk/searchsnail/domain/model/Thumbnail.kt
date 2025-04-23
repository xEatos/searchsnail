package com.borgnetzwerk.searchsnail.domain.model

import java.net.URL

data class UnresolvedThumbnail(val url: URL) {
    fun resolve(): ResolvedThumbnail = when (url.host) {
        "i.ytimg.com" -> ResolvedThumbnail(url)
        else -> ResolvedThumbnail(url)
    }
}

data class ResolvedThumbnail(val url: URL)