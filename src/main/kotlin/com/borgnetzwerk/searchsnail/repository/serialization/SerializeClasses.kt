package com.borgnetzwerk.searchsnail.repository.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject

@Serializable()
data class WikidataObject(
    val type: String,
    val value: String,
    val lang: String?
)

object WikidataObjectTransformer : JsonTransformingSerializer<WikidataObject>(WikidataObject.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when(element){
            is JsonObject -> buildJsonObject {
                element.forEach { (key, value) ->
                    when(key){
                        "xml:lang" -> put("lang", value)
                        else -> put(key, value)
                    }
                }
            }
            else -> element
        }
    }
}

@Serializable
data class Head(val vars: List<String>)

@Serializable
data class Bindings<T>(val bindings: List<T>)

@Serializable
data class QueryResult<TRow>(val head: Head, val results: Bindings<TRow>)

