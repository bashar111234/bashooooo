package com.example.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.HotspotPreferences
import com.example.data.HotspotRepository
import com.example.data.RemoteConfigRepository
import com.example.data.StatusFetchResult
import com.example.util.MikrotikParser
import java.util.concurrent.TimeUnit

class NetworkSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = HotspotPreferences(applicationContext)
        val remoteRepo = RemoteConfigRepository()
        val hotspotRepo = HotspotRepository()
        val notifier = QuotaNotifier(applicationContext)

        remoteRepo.fetchConfig()

        val lastNotifId = prefs.getLastAdminNotificationId()
        val newNotifs = remoteRepo.fetchNewNotifications(afterId = lastNotifId)
        if (newNotifs.isNotEmpty()) {
            var maxId = lastNotifId
            for (n in newNotifs) {
                if (n.title.isNotBlank() && n.body.isNotBlank()) {
                    notifier.notifyAdminBroadcast(
                        notifId = n.id,
                        title = n.title,
                        body = n.body,
                        highPriority = n.highPriority
                    )
                }
                if (n.id > maxId) maxId = n.id
            }
            prefs.setLastAdminNotificationId(maxId)
        }

        val status = hotspotRepo.fetchStatus(fastProbe = true, closeConnection = true)
        if (status is StatusFetchResult.Connected && !status.session.isProvisional) {
            val quotaState = prefs.getCardQuotaState(status.session.username)
            val analysis = MikrotikParser.analyzeQuota(
                session = status.session,
                savedMaxBytes = quotaState.maxBytes,
                savedMaxSecs = quotaState.maxSecs,
                alert30AlreadySent = quotaState.alert30Sent,
                alert15AlreadySent = quotaState.alert15Sent
            )
            if (analysis.shouldFireNotification15) {
                notifier.notifyQuota15(analysis.summaryText)
            } else if (analysis.shouldFireNotification30) {
                notifier.notifyQuota30(analysis.summaryText)
            }
            prefs.updateCardQuotaState(
                cardId = status.session.username,
                maxBytes = analysis.updatedMaxBytes,
                maxSecs = analysis.updatedMaxSecs,
                alert30Sent = analysis.alert30Sent,
                alert15Sent = analysis.alert15Sent
            )
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "alqanas_network_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<NetworkSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
