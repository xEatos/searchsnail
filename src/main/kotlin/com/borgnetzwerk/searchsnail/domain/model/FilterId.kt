package com.borgnetzwerk.searchsnail.domain.model

data class UnresolvedFilterId(val value: String) {
    fun resolve(): ResolvedFilterId? = ResolvedFilterId.create(value)
}

@ConsistentCopyVisibility
data class ResolvedFilterId private constructor(val value: NewFilterId) {

    companion object {
        fun create(value: String): ResolvedFilterId? =
            ids[value]?.let { ResolvedFilterId(it) }

        fun create(value: NewFilterId): ResolvedFilterId = ResolvedFilterId(value)

        private val ids = mapOf(
            "mediumType" to MediumTyp,
            "minDate" to MinDate,
            "maxDate" to MaxDate,
            "category" to Category,
            "subcategory" to Subcategory,
            "channel" to Channel,
            "platform" to Platform,
            "duration" to Duration,
            "hasTranscript" to HasTranscript,
            "language" to Language,
            "subtitleLanguage" to SubtitleLanguage,
        )

        fun getIds() = ids.map { it.value }
    }
}

sealed class NewFilterId
data object MediumTyp : NewFilterId()
data object MinDate : NewFilterId()
data object MaxDate : NewFilterId()
data object Category : NewFilterId()
data object Subcategory : NewFilterId()
data object Channel : NewFilterId()
data object Platform : NewFilterId()
data object Duration : NewFilterId()
data object HasTranscript : NewFilterId()
data object Language : NewFilterId()
data object SubtitleLanguage : NewFilterId()