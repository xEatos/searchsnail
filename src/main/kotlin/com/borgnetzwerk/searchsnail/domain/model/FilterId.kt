package com.borgnetzwerk.searchsnail.domain.model

data class UnresolvedFilterId(val value: String) {
    fun resolve(): ResolvedFilterId? = ResolvedFilterId.create(value)
}

@ConsistentCopyVisibility
data class ResolvedFilterId private constructor(val value: FilterId) {

    companion object {
        fun create(value: String): ResolvedFilterId? =
            ids[value]?.let { ResolvedFilterId(it) }

        fun create(value: FilterId): ResolvedFilterId = ResolvedFilterId(value)

        private val ids = mapOf(
            MediumTyp.toString() to MediumTyp,
            MinDate.toString() to MinDate,
            MaxDate.toString() to MaxDate,
            Category.toString() to Category,
            Subcategory.toString() to Subcategory,
            Channel.toString() to Channel,
            Platform.toString() to Platform,
            Duration.toString() to Duration,
            HasTranscript.toString() to HasTranscript,
            Language.toString() to Language,
            SubtitleLanguage.toString() to SubtitleLanguage,
        )

        fun getIds() = ids.map { create(it.value) }
    }
}

sealed class FilterId
data object MediumTyp : FilterId()
data object MinDate : FilterId()
data object MaxDate : FilterId()
data object Category : FilterId()
data object Subcategory : FilterId()
data object Channel : FilterId()
data object Platform : FilterId()
data object Duration : FilterId()
data object HasTranscript : FilterId()
data object Language : FilterId()
data object SubtitleLanguage : FilterId()