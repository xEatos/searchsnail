package com.borgnetzwerk.searchsnail.domain.model

import com.borgnetzwerk.searchsnail.utils.sparqlqb.IRI

data class IriWithMetadata(
    val iri: IRI,
    private val metadata: MutableMap<String, String> = mutableMapOf()
) {
    fun setMetadata(key: String, value: String) {
        metadata[key] = value
    }

    fun getMetadata(key: String) = metadata[key]
}
