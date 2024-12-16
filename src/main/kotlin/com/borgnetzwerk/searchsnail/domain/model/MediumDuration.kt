package com.borgnetzwerk.searchsnail.domain.model

import java.time.Duration

@ConsistentCopyVisibility
data class MediumDuration private constructor(val value: Int) {
    companion object {
        fun of(duration: Int): MediumDuration? =
            if (duration <= 0) {
                null
            } else {
                MediumDuration(duration)
            }

        fun of(duration: String): MediumDuration? {
            val parts = duration.split(":")
            return when (parts.size) {
                3 -> MediumDuration(parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt())
                2 -> MediumDuration(parts[0].toInt() * 60 + parts[1].toInt())
                else -> null
            }
        }

        fun of(duration: Long): MediumDuration? = MediumDuration.of(duration.toInt())

        fun of(duration: Duration): MediumDuration? = MediumDuration.of(duration.toSeconds())
    }

    private fun padNumber(num: Int): String = if(num < 10) "0$num" else num.toString()

    override fun toString(): String {
        val seconds = this.value % 60;
        val minutesInSeconds = (this.value - seconds) % 3600;
        val minutes = minutesInSeconds / 60;
        val hoursInSeconds = this.value - minutesInSeconds - seconds;
        val hours = hoursInSeconds / 3600;

        return if(hours > 0){ "${hours}:${padNumber(minutes)}:${padNumber(seconds)}"} else { "${minutes}:${padNumber(seconds)}"}
    }
}