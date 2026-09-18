package com.relationshipradar.app.connectors.callrecorder

import com.relationshipradar.app.data.repo.Repository

/** The single persistence boundary used by the review UI. */
class CallInsightReviewStore(private val repository: Repository) {
    suspend fun accept(
        personId: Long,
        sourceRecordingId: String,
        candidate: InsightCandidate,
        editedText: String,
    ): Boolean {
        val cleanText = editedText.trim()
        if (cleanText.isEmpty()) return false
        return repository.acceptCallInsight(
            personId = personId,
            sourceRecordingId = sourceRecordingId,
            candidateId = candidate.id,
            type = candidate.type.name,
            text = cleanText,
            sourceExcerpt = candidate.sourceExcerpt,
            sourceStartMillis = candidate.sourceStartMillis,
            suggestedDate = candidate.suggestedDate,
        ) > 0
    }

    suspend fun dismiss(
        personId: Long,
        sourceRecordingId: String,
        candidate: InsightCandidate,
    ): Boolean = repository.dismissCallInsight(
        personId = personId,
        sourceRecordingId = sourceRecordingId,
        candidateId = candidate.id,
        type = candidate.type.name,
        text = candidate.text,
        sourceExcerpt = candidate.sourceExcerpt,
        sourceStartMillis = candidate.sourceStartMillis,
    ) > 0
}
