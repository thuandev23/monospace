package com.monospace.app.core.domain.model

data class DetoxStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalSessions: Int = 0,
    val badges: List<DetoxBadge> = emptyList()
)

data class DetoxBadge(
    val id: BadgeId,
    val emoji: String,
    val name: String,
    val description: String,
    val unlocked: Boolean
)

enum class BadgeId {
    FIRST_SESSION,
    THREE_DAY_STREAK,
    SEVEN_DAY_STREAK,
    TEN_SESSIONS,
    THIRTY_DAY_STREAK
}

val ALL_BADGES = listOf(
    BadgeId.FIRST_SESSION to Triple("🌱", "Bắt đầu", "Hoàn thành session đầu tiên"),
    BadgeId.TEN_SESSIONS to Triple("🧘", "Kiên trì", "Hoàn thành 10 sessions"),
    BadgeId.THREE_DAY_STREAK to Triple("🔥", "3 ngày", "Streak 3 ngày liên tiếp"),
    BadgeId.SEVEN_DAY_STREAK to Triple("⚡", "7 ngày", "Streak 7 ngày liên tiếp"),
    BadgeId.THIRTY_DAY_STREAK to Triple("🏆", "30 ngày", "Streak 30 ngày liên tiếp")
)

fun computeDetoxStats(
    sessionDates: List<java.time.LocalDate>,
    totalSessions: Int
): DetoxStats {
    val currentStreak = computeCurrentStreak(sessionDates)
    val longestStreak = computeLongestStreak(sessionDates)

    val badges = ALL_BADGES.map { (id, meta) ->
        val (emoji, name, desc) = meta
        DetoxBadge(
            id = id,
            emoji = emoji,
            name = name,
            description = desc,
            unlocked = when (id) {
                BadgeId.FIRST_SESSION -> totalSessions >= 1
                BadgeId.TEN_SESSIONS -> totalSessions >= 10
                BadgeId.THREE_DAY_STREAK -> longestStreak >= 3
                BadgeId.SEVEN_DAY_STREAK -> longestStreak >= 7
                BadgeId.THIRTY_DAY_STREAK -> longestStreak >= 30
            }
        )
    }

    return DetoxStats(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalSessions = totalSessions,
        badges = badges
    )
}

private fun computeCurrentStreak(sortedDates: List<java.time.LocalDate>): Int {
    if (sortedDates.isEmpty()) return 0
    val unique = sortedDates.toSortedSet(reverseOrder())
    val today = java.time.LocalDate.now()
    var streak = 0
    var expected = if (unique.first() == today) today else today.minusDays(1)
    for (date in unique) {
        if (date == expected) {
            streak++
            expected = expected.minusDays(1)
        } else if (date.isBefore(expected)) {
            break
        }
    }
    return streak
}

private fun computeLongestStreak(sortedDates: List<java.time.LocalDate>): Int {
    if (sortedDates.isEmpty()) return 0
    val unique = sortedDates.distinct().sorted()
    var longest = 1
    var current = 1
    for (i in 1 until unique.size) {
        if (unique[i] == unique[i - 1].plusDays(1)) {
            current++
            if (current > longest) longest = current
        } else {
            current = 1
        }
    }
    return longest
}
