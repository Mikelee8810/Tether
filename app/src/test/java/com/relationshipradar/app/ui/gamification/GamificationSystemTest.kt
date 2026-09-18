package com.relationshipradar.app.ui.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationSystemTest {
    @Test
    fun `progress is measured inside the current level`() {
        val progress = GamificationSystem.progressFor(75)

        assertEquals(2, progress.currentLevel.level)
        assertEquals(3, progress.nextLevel?.level)
        assertEquals(0.25f, progress.fraction, 0.001f)
        assertEquals(75, progress.sparksToNextLevel)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `level boundary starts a fresh progress range`() {
        val progress = GamificationSystem.progressFor(150)

        assertEquals(3, progress.currentLevel.level)
        assertEquals(0f, progress.fraction, 0.001f)
        assertEquals(150, progress.sparksToNextLevel)
    }

    @Test
    fun `top level is complete without overflowing integer math`() {
        val progress = GamificationSystem.progressFor(Int.MAX_VALUE)

        assertEquals(5, progress.currentLevel.level)
        assertEquals(null, progress.nextLevel)
        assertEquals(1f, progress.fraction, 0.001f)
        assertEquals("Top orbit reached", progress.supportingLabel)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `negative sparks are treated as zero`() {
        val progress = GamificationSystem.progressFor(-20)

        assertEquals(1, progress.currentLevel.level)
        assertEquals(0f, progress.fraction, 0.001f)
        assertEquals(50, progress.sparksToNextLevel)
    }
}
