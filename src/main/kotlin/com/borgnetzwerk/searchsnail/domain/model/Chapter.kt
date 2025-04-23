package com.borgnetzwerk.searchsnail.domain.model

data class Chapter(val start: MediumDuration?, val end: MediumDuration?, val title: String) {
    companion object {

        private fun ofDescriptionEntry(str: String): Pair<MediumDuration?, String> {
            val d = str.substring(0, str.indexOf(' '))
            val t = str.substring(d.length)
            return Pair(MediumDuration.of(d), t)
        }

        fun resolveFromYouTubeDescription(description: String, duration: MediumDuration): List<Chapter> {
            val regex = Regex("([0-9]+:[0-9][0-9] [^\\n]+)", RegexOption.MULTILINE)
            val matches = regex.findAll(description).toList()
            return matches.mapIndexed { index, it ->
                val (startTimestamp, title) = ofDescriptionEntry(it.value)
                val endTimestamp = if (index == matches.size - 1) {
                    duration
                } else {
                    ofDescriptionEntry(matches[index + 1].value).first
                }
                Chapter(startTimestamp, endTimestamp, title)
            }.toList()
        }
    }
}
