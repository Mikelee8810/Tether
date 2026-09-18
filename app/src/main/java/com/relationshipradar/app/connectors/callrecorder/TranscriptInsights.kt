package com.relationshipradar.app.connectors.callrecorder

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class TranscriptSegment(
    val text: String,
    val startMillis: Long,
    val endMillis: Long,
    val confidence: Float? = null,
)

enum class InsightType(val label: String, val icon: String) {
    FOLLOW_UP("Follow-up", "↗"),
    UPCOMING_DATE("Upcoming", "◷"),
    MEMORY("Memory", "✦"),
    LITTLE_THING("Little thing", "♡"),
}

data class InsightCandidate(
    val id: String,
    val type: InsightType,
    val text: String,
    val sourceExcerpt: String,
    val sourceStartMillis: Long,
    val suggestedDate: LocalDate? = null,
    val confidence: Float? = null,
)

/**
 * Conservative first-pass extraction. It proposes only explicit language and always preserves
 * the exact source segment; acceptance remains a user decision. A model-backed extractor can
 * replace this later without changing the review contract.
 */
class TranscriptInsightExtractor(private val clock: Clock = Clock.systemDefaultZone()) {
    fun extract(segments: List<TranscriptSegment>): List<InsightCandidate> = buildList {
        segments.forEach { segment ->
            val text = segment.text.trim()
            if (text.isBlank()) return@forEach

            explicitFollowUp(text)?.let { followUp ->
                add(candidate(segment, InsightType.FOLLOW_UP, followUp, suggestedDate(text)))
            }

            explicitDate(text)?.let { datedText ->
                add(candidate(segment, InsightType.UPCOMING_DATE, datedText, suggestedDate(datedText)))
            }

            durableFact(text)?.let { fact ->
                add(candidate(segment, InsightType.MEMORY, fact))
            }
        }
    }.distinctBy { it.type to it.text.lowercase() }

    private fun explicitFollowUp(text: String): String? {
        val marker = FOLLOW_UP_MARKER.find(text) ?: return null
        return marker.groupValues[1].trim().trimEnd('.', '!', '?').replaceFirstChar(Char::uppercase)
    }

    private fun explicitDate(text: String): String? {
        if (!DATE_WORD.containsMatchIn(text)) return null
        return text.substringBefore('.').trim().takeIf { it.isNotBlank() }
    }

    private fun durableFact(text: String): String? {
        val sentence = text.substringBefore('.').trim()
        return sentence.takeIf { DURABLE_FACT.containsMatchIn(it) }
    }

    private fun suggestedDate(text: String): LocalDate? {
        val today = LocalDate.now(clock)
        if (text.contains("tomorrow", ignoreCase = true)) return today.plusDays(1)
        if (text.contains("next week", ignoreCase = true)) return today.plusWeeks(1)
        val day = DayOfWeek.entries.firstOrNull { text.contains(it.name, ignoreCase = true) } ?: return null
        return today.with(TemporalAdjusters.nextOrSame(day))
    }

    private fun candidate(
        segment: TranscriptSegment,
        type: InsightType,
        text: String,
        date: LocalDate? = null,
    ): InsightCandidate = InsightCandidate(
        id = UUID.nameUUIDFromBytes("${segment.startMillis}|${type.name}|$text".toByteArray()).toString(),
        type = type,
        text = text,
        sourceExcerpt = segment.text.trim(),
        sourceStartMillis = segment.startMillis,
        suggestedDate = date,
        confidence = segment.confidence,
    )

    private companion object {
        val FOLLOW_UP_MARKER = Regex("(?:remember to|follow up(?: with)?|don['’]t forget to)\\s+([^.!?]+)", RegexOption.IGNORE_CASE)
        val DATE_WORD = Regex("\\b(today|tomorrow|next week|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE)
        val DURABLE_FACT = Regex("\\b(has|have|is starting|is moving|works at|lives in|birthday)\\b", RegexOption.IGNORE_CASE)
    }
}

fun formatTranscriptTimestamp(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
