package com.relationshipradar.app.connectors.callrecorder

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptInsightExtractorTest {
    private val extractor = TranscriptInsightExtractor(
        Clock.fixed(Instant.parse("2026-09-18T12:00:00Z"), ZoneOffset.UTC)
    )

    @Test
    fun `explicit memory date and follow-up remain anchored to source`() {
        val segment = TranscriptSegment(
            text = "Marcus has an interview on Friday. Remember to ask how it went next week.",
            startMillis = 742_000,
            endMillis = 755_000,
            confidence = 0.91f,
        )

        val candidates = extractor.extract(listOf(segment))

        assertEquals(3, candidates.size)
        assertTrue(candidates.any { it.type == InsightType.MEMORY && it.text.contains("interview") })
        assertTrue(candidates.any { it.type == InsightType.UPCOMING_DATE && it.suggestedDate == LocalDate.parse("2026-09-18") })
        assertTrue(candidates.any { it.type == InsightType.FOLLOW_UP && it.suggestedDate == LocalDate.parse("2026-09-25") })
        assertTrue(candidates.all { it.sourceStartMillis == 742_000L && it.sourceExcerpt == segment.text })
    }

    @Test
    fun `speculative emotional language is ignored`() {
        val candidates = extractor.extract(
            listOf(TranscriptSegment("Marcus sounded anxious and probably dislikes the new job.", 0, 4_000))
        )

        assertTrue(candidates.isEmpty())
    }
}
