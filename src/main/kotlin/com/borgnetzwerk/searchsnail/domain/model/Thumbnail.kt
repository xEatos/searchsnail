package com.borgnetzwerk.searchsnail.domain.model

import java.net.URL

data class UnresolvedThumbnail(val url: URL) {
    fun resolve(): ResolvedThumbnail = when (url.host) {
        "i.ytimg.com" -> ResolvedThumbnail(URL("https://i.ytimg.com/vi_webp/${url.path.split("/")[2]}/mqdefault.webp"))
        else -> ResolvedThumbnail(url)
    }
}

data class ResolvedThumbnail(val url: URL)