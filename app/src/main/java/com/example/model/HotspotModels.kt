package com.example.model

object PortalConstants {
    const val BASE_SECURE_URL = "https://alqanas.cc.cd"
    const val ADMIN_SERVER_URL = "http://172.16.255.10:5055"
    const val LOGIN_URL = "$BASE_SECURE_URL/login"
    const val STATUS_URL = "$BASE_SECURE_URL/status.html"
    const val API_URL = "$BASE_SECURE_URL/api.json"
    const val LOGOUT_URL = "$BASE_SECURE_URL/logout"

    const val ENTERTAINMENT_MOVIES_URL = "http://30.30.30.30:8096/"
    const val ENTERTAINMENT_TV_URL = "http://30.30.30.30:8080/tv/"

    const val SUPPORT_PHONE = "772432226"
    const val WHATSAPP_URL = "https://wa.me/967772432226"
    const val WHATSAPP_ORDER_URL =
        "https://wa.me/967772432226?text=%D8%A7%D9%84%D8%B3%D9%84%D8%A7%D9%85%20%D8%B9%D9%84%D9%8A%D9%83%D9%85%D8%8C%20%D9%82%D9%85%D8%AA%20%D8%A8%D8%A7%D9%84%D8%AA%D8%AD%D9%88%D9%8A%D9%84%20%D9%84%D8%B4%D8%B1%D8%A7%D8%A1%20%D9%83%D8%B1%D8%AA%20%D8%A5%D9%86%D8%AA%D8%B1%D9%86%D8%AA%20%D9%85%D9%86%20%D8%B4%D8%A8%D9%83%D8%A9%20%D8%A7%D9%84%D9%82%D9%86%D8%A7%D8%B5"

    const val DEFAULT_VALIDITY_SECS = 30L * 24L * 3600L

    const val ROUTE_LOGIN = "login"
    const val ROUTE_STATUS = "status"
    const val ROUTE_LOGOUT = "logout"
}

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "تلقائي (النظام)"),
    DARK("dark", "الوضع الليلي"),
    LIGHT("light", "الوضع النهاري");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class SpeedProfile(
    val key: String,
    val title: String,
    val shortLabel: String,
    val rate: String,
    val needleAngle: Float,
    val arcProgress: Float
) {
    ECO("eco", "سرعة اقتصادية", "اقتصادية", "715 Kbps", -50f, 0.222f),
    DEFAULT("default", "سرعة عادية", "عادية", "1 Mbps", -4f, 0.478f),
    HIGH("high", "سرعة عالية", "عالية", "2 Mbps", 42f, 0.733f),
    VERY("very", "عالية جداً", "عالية جداً", "4 Mbps", 86f, 0.978f);

    companion object {
        fun fromKey(key: String?): SpeedProfile? =
            entries.firstOrNull { it.key == key }
    }
}

data class PlanTier(
    val price: String,
    val note: String?,
    val quota: String,
    val duration: String,
    val validity: String,
    val speed: SpeedProfile,
    val blockUpdates: Boolean = true
)

val PRICING_PLANS = listOf(
    PlanTier("100 ريال", null, "400 MB", "4 ساعات", "4 أيام", SpeedProfile.ECO, true),
    PlanTier("200 ريال", null, "800 MB", "8 ساعات", "9 أيام", SpeedProfile.DEFAULT, true),
    PlanTier("250 ريال", null, "1100 MB", "10 ساعات", "10 أيام", SpeedProfile.DEFAULT, true),
    PlanTier("500 ريال", null, "2200 MB", "20 ساعة", "20 يوم", SpeedProfile.VERY, true),
    PlanTier("3000 ريال", "فئة VIP", "15 GB", "ساعات مفتوح", "شهر", SpeedProfile.VERY, true),
    PlanTier("100 ريال", "استراحة فقط", "مفتوح", "5 ساعات", "5 أيام", SpeedProfile.ECO, true)
)

data class WalletGuide(
    val name: String,
    val videoPath: String
) {
    val fullVideoUrl: String
        get() = "${PortalConstants.BASE_SECURE_URL}/$videoPath"
}

val WALLET_GUIDES = listOf(
    WalletGuide("جيب", "videos/1.mp4"),
    WalletGuide("فلوسك", "videos/2.mp4"),
    WalletGuide("ون كاش", "videos/3.mp4"),
    WalletGuide("جوالي", "videos/4.mp4")
)

data class WalletMerchant(
    val title: String,
    val subtitle: String,
    val code: String
)

val WALLET_MERCHANTS = listOf(
    WalletMerchant("محفظة جيب · فلوسك · ون كاش", "كود نقطة البيع", "20313"),
    WalletMerchant("محفظة جوالي", "كود نقطة البيع", "981")
)

data class SavedCard(
    val code: String,
    val speedKey: String?,
    val date: String
)

data class PackageSpec(
    val totalBytes: Double?,
    val totalSecs: Long
)

private const val MIB = 1024.0 * 1024.0
private const val GIB = 1024.0 * 1024.0 * 1024.0

val PACKAGE_DICTIONARY: Map<String, PackageSpec> = mapOf(
    "100R" to PackageSpec(400.0 * MIB, 4L * 3600L),
    "200R" to PackageSpec(800.0 * MIB, 8L * 3600L),
    "500R" to PackageSpec(2.2 * GIB, 25L * 3600L),
    "250R" to PackageSpec(1100.0 * MIB, 10L * 3600L),
    "300R" to PackageSpec(800.0 * MIB, 7L * 3600L),
    "100S" to PackageSpec(300.0 * MIB, 3L * 3600L),
    "Estraha100" to PackageSpec(null, 6L * 3600L),
    "hamid" to PackageSpec(null, 16L * 24L * 3600L),
    "dokan" to PackageSpec(10.0 * GIB, 17L * 24L * 3600L),
    "720h" to PackageSpec(10.0 * GIB, 30L * 24L * 3600L),
    "5000R" to PackageSpec(null, 30L * 24L * 3600L),
    "2500F" to PackageSpec(null, 30L * 24L * 3600L),
    "try" to PackageSpec(null, 30L * 24L * 3600L)
)

data class LoginChallenge(
    val actionUrl: String = PortalConstants.LOGIN_URL,
    val dst: String = PortalConstants.STATUS_URL,
    val chapId: String? = null,
    val chapChallenge: String? = null,
    val errorMessage: String? = null
)

data class SessionStatus(
    val loggedIn: Boolean = false,
    val isProvisional: Boolean = false,
    val username: String = "",
    val userProfile: String = "",
    val actualProfile: String = "",
    val bytesIn: String = "0 B",
    val bytesOut: String = "0 B",
    val remainBytes: String = "غير محدود",
    val remainBytesRaw: String = "",
    val limitBytesTotalRaw: String = "",
    val bytesInRaw: String = "",
    val bytesOutRaw: String = "",
    val sessionTimeLeft: String = "مفتوح",
    val timeLeftSecs: String = "",
    val uptime: String = "0s",
    val uptimeSecs: String = "",
    val linkLogout: String = PortalConstants.LOGOUT_URL,
    val linkLogin: String = PortalConstants.LOGIN_URL,
    val linkStatus: String = PortalConstants.STATUS_URL
) {
    val activeProfile: String
        get() {
            val p1 = userProfile.trim()
            val p2 = actualProfile.trim()
            return when {
                p1.isNotEmpty() && !p1.contains("$(") -> p1
                p2.isNotEmpty() && !p2.contains("$(") -> p2
                else -> ""
            }
        }
}

data class LogoutSummary(
    val username: String,
    val uptime: String,
    val timeLeft: String,
    val bytesIn: String,
    val bytesOut: String
)

enum class QuotaAlertLevel {
    NONE,
    WARN_30,
    CRIT_15
}

data class QuotaAnalysis(
    val bytesPct: Double? = null,
    val timePct: Double? = null,
    val activePct: Double? = null,
    val alert30Sent: Boolean = false,
    val alert15Sent: Boolean = false,
    val summaryText: String = "",
    val alertLevel: QuotaAlertLevel = QuotaAlertLevel.NONE,
    val updatedMaxBytes: Double = 0.0,
    val updatedMaxSecs: Long = 0L,
    val shouldFireNotification30: Boolean = false,
    val shouldFireNotification15: Boolean = false,
    val shouldResetAlerts: Boolean = false
)


object DynamicPackageStore {
    private val store = java.util.concurrent.ConcurrentHashMap<String, PackageSpec>(PACKAGE_DICTIONARY)

    fun updateAll(newMap: Map<String, PackageSpec>) {
        store.clear()
        store.putAll(PACKAGE_DICTIONARY)
        store.putAll(newMap)
    }

    fun get(profileName: String): PackageSpec? {
        return store[profileName]
            ?: store.entries.firstOrNull { it.key.equals(profileName, ignoreCase = true) }?.value
    }
}
