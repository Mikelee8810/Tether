package com.relationshipradar.app.connectors.callrecorder

import org.junit.Assert.assertEquals
import org.junit.Test

class WhisperTimestampParserTest {
    @Test fun `preserves source timestamps from whisper output`() {
        val segments = WhisperTimestampParser.parse("[00:12:22.000 --> 00:12:35.500]: Marcus has an interview Friday")
        assertEquals(742_000, segments.single().startMillis)
        assertEquals(755_500, segments.single().endMillis)
        assertEquals("Marcus has an interview Friday", segments.single().text)
    }
}
