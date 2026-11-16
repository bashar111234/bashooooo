package com.example.data

import com.example.model.DynamicPackageStore
import com.example.model.PackageSpec
import com.example.model.PlanTier
import com.example.model.PortalConstants
import com.example.model.SpeedProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RemoteBanner(
    val enabled: Boolean = false,
    val title: String = "",
    val text: String = "",
    val link: String = ""
)

data class RemoteVersionInfo(
    val code: Int = 1,
    val name: String = "1.0.0",
    val apkUrl: String = "",
    val forceUpdate: Boolean = false,
    val notes: String = ""
)

data class RemoteNotificationItem(
    val id: Int,
    val title: String,
    val body: String,
    val highPriority: Boolean
)

data class RemoteAppConfig(
    val version: RemoteVersionInfo = RemoteVersionInfo(),
    val banner: RemoteBanner = RemoteBanner(),
    val supportPhone: String = PortalConstants.SUPPORT_PHONE,
    val whatsappUrl: String = PortalConstants.WHATSAPP_URL,
    val moviesUrl: String = PortalConstants.ENTERTAINMENT_MOVIES_URL,
    val tvUrl: String = PortalConstants.ENTERTAINMENT_TV_URL,
    val pricingPlans: List<PlanTier> = emptyList(),
    val latestNotificationId: Int = 0
)

class RemoteConfigRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun fetchConfig(): RemoteAppConfig? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${PortalConstants.ADMIN_SERVER_URL}/api/v1/config?_t=${System.currentTimeMillis()}")
                .header("Cache-Control", "no-store")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return@withContext null
                val root = JSONObject(raw)

                // Sync profiles into DynamicPackageStore for accurate 30% / 15% alerts
                val profilesObj = root.optJSONObject("profiles")
                if (profilesObj != null) {
                    val synced = mutableMapOf<String, PackageSpec>()
                    val keys = profilesObj.keys()
                    while (keys.hasNext()) {
                        val profileName = keys.next()
                        val item = profilesObj.optJSONObject(profileName) ?: continue
                        val quotaMb = if (item.isNull("quota_mb")) null else item.optDouble("quota_mb", 0.0)
                        val quotaHours = if (item.isNull("quota_hours")) null else item.optDouble("quota_hours", 0.0)
                        val validityDays = item.optInt("validity_days", 30)

                        val totalBytes = quotaMb?.takeIf { it > 0.0 }?.let { it * 1024.0 * 1024.0 }
                        val totalSecs = when {
                            quotaHours != null && quotaHours > 0.0 -> (quotaHours * 3600.0).toLong()
                            validityDays > 0 -> validityDays.toLong() * 86400L
                            else -> PortalConstants.DEFAULT_VALIDITY_SECS
                        }
                        synced[profileName] = PackageSpec(totalBytes = totalBytes, totalSecs = totalSecs)
                    }
                    if (synced.isNotEmpty()) {
                        DynamicPackageStore.updateAll(synced)
                    }
                }

                val versionObj = root.optJSONObject("version")
                val versionInfo = if (versionObj != null) {
                    RemoteVersionInfo(
                        code = versionObj.optInt("code", 1),
                        name = versionObj.optString("name", "1.0.0"),
                        apkUrl = versionObj.optString("apk_url", ""),
                        forceUpdate = versionObj.optBoolean("force_update", false),
                        notes = versionObj.optString("notes", "")
                    )
                } else RemoteVersionInfo()

                val bannerObj = root.optJSONObject("banner")
                val banner = if (bannerObj != null) {
                    RemoteBanner(
                        enabled = bannerObj.optBoolean("enabled", false),
                        title = bannerObj.optString("title", ""),
                        text = bannerObj.optString("text", ""),
                        link = bannerObj.optString("link", "")
                    )
                } else RemoteBanner()

                val linksObj = root.optJSONObject("links")
                val plansArr = root.optJSONArray("pricing_plans")
                val plans = mutableListOf<PlanTier>()
                if (plansArr != null) {
                    for (i in 0 until plansArr.length()) {
                        val p = plansArr.optJSONObject(i) ?: continue
                        plans.add(
                            PlanTier(
                                price = p.optString("price", ""),
                                note = p.optString("note", "").ifBlank { null },
                                quota = p.optString("quota", ""),
                                duration = p.optString("duration", ""),
                                validity = p.optString("validity", ""),
                                speed = SpeedProfile.fromKey(p.optString("speed_key")) ?: SpeedProfile.DEFAULT,
                                blockUpdates = p.optBoolean("block_updates", true)
                            )
                        )
                    }
                }

                RemoteAppConfig(
                    version = versionInfo,
                    banner = banner,
                    supportPhone = linksObj?.optString("support_phone")?.ifBlank { null } ?: PortalConstants.SUPPORT_PHONE,
                    whatsappUrl = linksObj?.optString("whatsapp_url")?.ifBlank { null } ?: PortalConstants.WHATSAPP_URL,
                    moviesUrl = linksObj?.optString("entertainment_movies_url")?.ifBlank { null } ?: PortalConstants.ENTERTAINMENT_MOVIES_URL,
                    tvUrl = linksObj?.optString("entertainment_tv_url")?.ifBlank { null } ?: PortalConstants.ENTERTAINMENT_TV_URL,
                    pricingPlans = plans,
                    latestNotificationId = root.optInt("latest_notification_id", 0)
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchNewNotifications(afterId: Int): List<RemoteNotificationItem> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${PortalConstants.ADMIN_SERVER_URL}/api/v1/notifications?after_id=$afterId&_t=${System.currentTimeMillis()}")
                .header("Cache-Control", "no-store")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return@withContext emptyList()
                val arr = JSONObject(raw).optJSONArray("notifications") ?: return@withContext emptyList()
                val list = mutableListOf<RemoteNotificationItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    list.add(
                        RemoteNotificationItem(
                            id = obj.optInt("id", 0),
                            title = obj.optString("title", ""),
                            body = obj.optString("body", ""),
                            highPriority = obj.optString("priority", "high") == "high"
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
