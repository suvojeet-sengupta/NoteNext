package com.suvojeet.notenext.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.suvojeet.notenext.data.repository.ReviewSettingsRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewManager @Inject constructor(
    private val repository: ReviewSettingsRepository
) {
    companion object {
        private const val DAYS_UNTIL_PROMPT = 3L
        private const val LAUNCHES_UNTIL_PROMPT = 5
    }

    suspend fun shouldRequestReview(): Boolean {
        val settings = repository.getReviewSettings()
        
        if (settings.isReviewRequested) {
            return false
        }

        if (settings.firstOpenTime == 0L) {
            repository.setFirstOpenTime(System.currentTimeMillis())
        }

        repository.incrementAppOpensCount()
        
        // Re-read settings after increment
        val updatedSettings = repository.getReviewSettings()

        val timeSinceFirstOpen = System.currentTimeMillis() - updatedSettings.firstOpenTime
        val daysSinceFirstOpen = TimeUnit.MILLISECONDS.toDays(timeSinceFirstOpen)

        return daysSinceFirstOpen >= DAYS_UNTIL_PROMPT || updatedSettings.appOpensCount >= LAUNCHES_UNTIL_PROMPT
    }

    fun requestReviewFlow(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // Mark as requested even if dialog didn't show (Google policy)
                    // We need a scope to save this.
                }
            }
        }
    }
    
    suspend fun markReviewRequested() {
        repository.setReviewRequested(true)
    }
}
