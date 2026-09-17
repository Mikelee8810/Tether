package com.relationshipradar.app.ui.gamification

data class OrbitLevel(
    val level: Int,
    val title: String,
    val minSparks: Int,
    val maxSparks: Int,
    val badgeIcon: String,
)

object GamificationSystem {
    val LEVELS = listOf(
        OrbitLevel(1, "Starlight Pioneer", 0, 49, "✨"),
        OrbitLevel(2, "Orbit Navigator", 50, 149, "🛸"),
        OrbitLevel(3, "Constellation Guide", 150, 299, "🌌"),
        OrbitLevel(4, "Cosmic Connector", 300, 499, "🪐"),
        OrbitLevel(5, "Galactic Anchor", 500, Int.MAX_VALUE, "🌟"),
    )

    fun getLevelForSparks(sparks: Int): OrbitLevel {
        return LEVELS.lastOrNull { sparks >= it.minSparks } ?: LEVELS.first()
    }
}

data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val isUnlocked: Boolean = currentProgress >= targetProgress,
    val sparkReward: Int
)

object OrbitMilestones {
    fun computeMilestones(
        sparks: Int,
        weeklyCount: Int,
        totalContacts: Int,
        contactsWithCustomAvatar: Int,
        innerCircleHealthyCount: Int,
        innerCircleTotalCount: Int
    ): List<Milestone> {
        return listOf(
            Milestone(
                id = "first_spark",
                title = "First Spark",
                description = "Log your first connection or reach out",
                icon = "🌟",
                currentProgress = (sparks / 10).coerceAtMost(1),
                targetProgress = 1,
                sparkReward = 10
            ),
            Milestone(
                id = "warm_circle",
                title = "Warm Circle",
                description = "Stay in touch with 3 people in a single week",
                icon = "☕",
                currentProgress = weeklyCount.coerceAtMost(3),
                targetProgress = 3,
                sparkReward = 25
            ),
            Milestone(
                id = "full_orbit",
                title = "Full Orbit",
                description = "Keep all Inner Circle contacts in good touch",
                icon = "🪐",
                currentProgress = if (innerCircleTotalCount > 0) innerCircleHealthyCount else 0,
                targetProgress = if (innerCircleTotalCount > 0) innerCircleTotalCount else 1,
                sparkReward = 50
            ),
            Milestone(
                id = "gemstone_aura",
                title = "Gemstone Aura",
                description = "Personalize a contact's avatar color or photo",
                icon = "💎",
                currentProgress = contactsWithCustomAvatar.coerceAtMost(1),
                targetProgress = 1,
                sparkReward = 15
            ),
            Milestone(
                id = "century_spark",
                title = "Century Spark",
                description = "Accumulate 100 relationship Sparks",
                icon = "✨",
                currentProgress = sparks.coerceAtMost(100),
                targetProgress = 100,
                sparkReward = 100
            )
        )
    }
}

