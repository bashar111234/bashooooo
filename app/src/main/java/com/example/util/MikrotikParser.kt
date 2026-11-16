package com.example.util

import com.example.model.LoginChallenge
import com.example.model.LogoutSummary
import com.example.model.DynamicPackageStore
import com.example.model.PACKAGE_DICTIONARY
import com.example.model.PackageSpec
import com.example.model.PortalConstants
import com.example.model.QuotaAlertLevel
import com.example.model.QuotaAnalysis
import com.example.model.SessionStatus
import java.io.ByteArrayOutputStream
import java.net.URI
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MikrotikParser {

    private val ERROR_TRANSLATION_MAP = mapOf(
        "internal-error" to "حدث خطأ داخلي في النظام، يرجى المحاولة مرة أخرى",
        "config-error" to "خطأ في إعدادات خدمة الهوتسبوت",
        "not-logged-in" to "أنت غير مسجل الدخول حالياً",
        "ippool-empty" to "لا توجد عناوين IP متاحة حالياً في الشبكة، يرجى المحاولة بعد قليل",
        "shutting-down" to "الخدمة متوقفة مؤقتاً للصيانة",
        "user-session-limit" to "هذا الكرت قيد الاستخدام حالياً على جهاز آخر (تم الوصول للحد الأقصى للأجهزة)",
        "license-session-limit" to "تم الوصول للحد الأقصى للجلسات المسموحة في النظام",
        "wrong-mac-username" to "هذا الكرت مقترن بجهاز آخر ولا يمكن استخدامه على هذا الجهاز",
        "chap-missing" to "انتهت جلسة التشفير في البوابة، يرجى المحاولة مرة أخرى",
        "invalid-username" to "رقم الكرت غير صحيح، يرجى التحقق من الرقم المدخل",
        "invalid-mac" to "هذا الجهاز غير مصرح له بالدخول عبر هذا الكرت",
        "uptime-limit" to "انتهت صلاحية الوقت المسموح به لهذا الكرت",
        "traffic-limit" to "انتهى رصيد البيانات (الميجا) المتبقي لهذا الكرت",
        "radius-timeout" to "خادم المصادقة لا يستجيب حالياً، يرجى المحاولة بعد لحظات",
        "auth-in-progress" to "جاري التحقق من تسجيل الدخول حالياً، يرجى الانتظار...",
        "invalid username or password" to "رقم الكرت غير صحيح، يرجى التحقق من الرقم المدخل",
        "already logged in" to "الكرت قيد الاستخدام حالياً على جهاز آخر",
        "cannot log in - already logged in" to "لا يمكن تسجيل الدخول - الجهاز متصل بالفعل",
        "uptime limit reached" to "انتهت صلاحية الوقت المسموح به لهذا الكرت",
        "traffic limit reached" to "انتهى رصيد البيانات المتبقي لهذا الكرت",
        "transfer limit reached" to "انتهى حد نقل البيانات المحدد لهذا الاشتراك",
        "not found" to "البيانات المطلوبة غير موجودة",
        "session limit reached" to "تم الوصول إلى الحد الأقصى للجلسات",
        "shared users limit reached" to "تم تجاوز عدد الأجهزة المسموح بها لهذا الكرت",
        "radius server is not responding" to "خادم المصادقة لا يستجيب، يرجى المحاولة بعد لحظات",
        "invalid calling station id" to "هذا الكرت مقترن بجهاز آخر ولا يمكن تشغيله على جهازك",
        "internal error" to "حدث خطأ داخلي في الخادم، يرجى المحاولة لاحقاً"
    )

    fun maskCardNumber(card: String): String {
        val str = card.trim()
        if (str.isEmpty()) return ""
        if (str.contains("$(")) return str
        if (str.length <= 4) return str
        return str.take(4) + "*".repeat(str.length - 4)
    }

    fun translateMikrotikError(rawError: String?): String {
        if (rawError.isNullOrBlank()) return ""
        val cleaned = rawError.trim()
        if (cleaned.contains("$(")) return ""
        val lower = cleaned.lowercase()
        ERROR_TRANSLATION_MAP[lower]?.let { return it }
        if (Regex("^user\\s+.*\\s+not found$").matches(lower) ||
            (lower.contains("not found") && lower.contains("user"))
        ) {
            return "رقم الكرت غير موجود في النظام"
        }
        if (lower.contains("no more sessions are allowed for user")) {
            return "هذا الكرت مستخدم حالياً في جهاز آخر"
        }
        for ((key, value) in ERROR_TRANSLATION_MAP) {
            if (lower.contains(key)) return value
        }
        return cleaned
    }

    fun ensureSecureUrl(rawUrl: String?, fallbackPath: String): String {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.contains("$(")) {
            return PortalConstants.BASE_SECURE_URL + fallbackPath
        }
        if (trimmed.startsWith("http://")) {
            return try {
                val uri = URI(trimmed)
                val path = uri.rawPath?.ifEmpty { fallbackPath } ?: fallbackPath
                val query = uri.rawQuery?.let { "?$it" }.orEmpty()
                PortalConstants.BASE_SECURE_URL + path + query
            } catch (_: Exception) {
                PortalConstants.BASE_SECURE_URL + fallbackPath
            }
        }
        if (trimmed.startsWith("/")) {
            return PortalConstants.BASE_SECURE_URL + trimmed
        }
        return trimmed
    }

    /**
     * Decodes JS string literal escapes (such as MikroTik's octal `\377` or hex `\xFF`)
     * into raw byte values for standard CHAP MD5 password hashing.
     */
    fun decodeJsOctalEscapesToBytes(jsLiteral: String): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < jsLiteral.length) {
            val ch = jsLiteral[i]
            if (ch == '\\' && i + 1 < jsLiteral.length) {
                val next = jsLiteral[i + 1]
                when {
                    next in '0'..'7' -> {
                        var end = i + 1
                        while (end < jsLiteral.length && end < i + 4 && jsLiteral[end] in '0'..'7') {
                            end++
                        }
                        val octalVal = jsLiteral.substring(i + 1, end).toInt(8)
                        out.write(octalVal and 0xFF)
                        i = end
                        continue
                    }
                    next == 'x' && i + 3 < jsLiteral.length -> {
                        val hex = jsLiteral.substring(i + 2, i + 4).toIntOrNull(16)
                        if (hex != null) {
                            out.write(hex and 0xFF)
                            i += 4
                            continue
                        }
                    }
                    next == '\\' -> {
                        out.write('\\'.code)
                        i += 2
                        continue
                    }
                }
            }
            out.write(ch.code and 0xFF)
            i++
        }
        return out.toByteArray()
    }

    fun computeChapPassword(chapId: String?, password: String, chapChallenge: String?): String {
        if (chapId.isNullOrBlank() || chapChallenge.isNullOrBlank() ||
            chapId.contains("$(") || chapChallenge.contains("$(")
        ) {
            return password
        }
        val idBytes = decodeJsOctalEscapesToBytes(chapId)
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val challengeBytes = decodeJsOctalEscapesToBytes(chapChallenge)

        val md = MessageDigest.getInstance("MD5")
        md.update(idBytes)
        md.update(passBytes)
        md.update(challengeBytes)
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun parseLoginChallenge(html: String): LoginChallenge {
        val actionMatch = Regex("""<form[^>]*name=["']login["'][^>]*action=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)
            ?: Regex("""<form[^>]*id=["']loginForm["'][^>]*action=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)
        val actionUrl = ensureSecureUrl(actionMatch?.groupValues?.getOrNull(1), "/login")

        val dstMatch = Regex("""name=["']dst["'][^>]*value=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(html)
        val rawDst = dstMatch?.groupValues?.getOrNull(1)?.takeIf { !it.contains("$(") && it.isNotBlank() }
            ?: PortalConstants.STATUS_URL

        // MikroTik injects: hexMD5('<chap-id>' + '' + '<chap-challenge>')
        val chapRegex = Regex("""hexMD5\(\s*['"]([^'"]*)['"]\s*\+\s*[^+]*\+\s*['"]([^'"]*)['"]\s*\)""")
        val chapMatch = chapRegex.find(html)
        val chapId = chapMatch?.groupValues?.getOrNull(1)?.takeIf { !it.contains("$(") && it.isNotEmpty() }
        val chapChallenge = chapMatch?.groupValues?.getOrNull(2)?.takeIf { !it.contains("$(") && it.isNotEmpty() }

        val errSpanMatch = Regex("""id=["']mikrotikErrorText["'][^>]*>(.*?)</span>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)
        val rawErr = errSpanMatch?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]*>"), "")?.trim()
        val translatedErr = translateMikrotikError(rawErr).ifBlank { null }

        return LoginChallenge(
            actionUrl = actionUrl,
            dst = rawDst,
            chapId = chapId,
            chapChallenge = chapChallenge,
            errorMessage = translatedErr
        )
    }

    fun isLoginPageHtml(html: String): Boolean {
        return html.contains("name=\"login\"") ||
            html.contains("name='login'") ||
            html.contains("id=\"loginForm\"") ||
            html.contains("id='loginForm'")
    }

    fun isLogoutPageHtml(html: String): Boolean {
        return html.contains("id=\"logoutUsername\"") || html.contains("تم تسجيل الخروج بنجاح")
    }

    fun isAloginPageHtml(html: String): Boolean {
        return html.contains("تم الدخول بنجاح") ||
            html.contains("تم تسجيل الدخول بنجاح") ||
            (html.contains("status.html") && html.contains("window.location.replace"))
    }

    fun parseStatusHtml(html: String): SessionStatus? {
        if (isLoginPageHtml(html) && !html.contains("window.STATUS_CONFIG")) {
            return null
        }

        fun extractConfigField(key: String): String {
            val match = Regex("""$key\s*:\s*"([^"]*)"""").find(html)
            val valStr = match?.groupValues?.getOrNull(1)?.trim().orEmpty()
            return if (valStr.contains("$(")) "" else valStr
        }

        fun extractElementText(id: String): String {
            val match = Regex("""id=["']$id["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)
            val text = match?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]*>"), "")?.trim().orEmpty()
            return if (text.contains("$(")) "" else text
        }

        val loggedInRaw = extractConfigField("loggedIn")
        if (loggedInRaw == "no") return null

        val username = extractConfigField("username").ifEmpty { extractElementText("valUsername") }
        if (username.isEmpty() && loggedInRaw != "yes") {
            return null
        }

        val remainBytesRaw = extractConfigField("remainBytesRaw")
        val remainBytesNice = extractConfigField("remainBytes").ifEmpty { extractElementText("valRemainBytes") }
        val resolvedRemain = when {
            remainBytesNice.isNotBlank() -> remainBytesNice
            remainBytesRaw.isNotBlank() && Regex("^\\d+$").matches(remainBytesRaw) ->
                formatBytesNice(remainBytesRaw.toDouble())
            else -> "غير محدود"
        }

        return SessionStatus(
            loggedIn = true,
            username = username,
            userProfile = extractConfigField("userProfile"),
            actualProfile = extractConfigField("actualProfile"),
            bytesIn = extractConfigField("bytesIn").ifEmpty { extractElementText("valBytesIn") }.ifEmpty { "0 B" },
            bytesOut = extractConfigField("bytesOut").ifEmpty { extractElementText("valBytesOut") }.ifEmpty { "0 B" },
            remainBytes = resolvedRemain,
            remainBytesRaw = remainBytesRaw,
            limitBytesTotalRaw = extractConfigField("limitBytesTotalRaw"),
            bytesInRaw = extractConfigField("bytesInRaw"),
            bytesOutRaw = extractConfigField("bytesOutRaw"),
            sessionTimeLeft = extractConfigField("sessionTimeLeft").ifEmpty { extractElementText("valTimeLeft") }.ifEmpty { "مفتوح" },
            timeLeftSecs = extractConfigField("timeLeftSecs"),
            uptime = extractConfigField("uptime").ifEmpty { extractElementText("valUptime") }.ifEmpty { "0s" },
            uptimeSecs = extractConfigField("uptimeSecs"),
            linkLogout = ensureSecureUrl(extractConfigField("linkLogout"), "/logout"),
            linkLogin = ensureSecureUrl(extractConfigField("linkLogin"), "/login"),
            linkStatus = ensureSecureUrl(extractConfigField("linkStatus"), "/status.html")
        )
    }

    fun parseLogoutHtml(html: String, fallback: SessionStatus?): LogoutSummary {
        fun extractText(id: String): String {
            val match = Regex("""id=["']$id["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)
            val txt = match?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]*>"), "")?.trim().orEmpty()
            return if (txt.contains("$(")) "" else txt
        }

        return LogoutSummary(
            username = extractText("logoutUsername").ifEmpty { fallback?.username ?: "—" },
            uptime = extractText("logoutUptime").ifEmpty { fallback?.uptime ?: "—" },
            timeLeft = extractText("logoutTimeLeft").ifEmpty { fallback?.sessionTimeLeft ?: "—" },
            bytesIn = extractText("logoutBytesIn").ifEmpty { fallback?.bytesIn ?: "—" },
            bytesOut = extractText("logoutBytesOut").ifEmpty { fallback?.bytesOut ?: "—" }
        )
    }

    data class ApiJsonStatus(
        val captive: Boolean,
        val userPortalUrl: String?,
        val secondsRemaining: Long?,
        val bytesRemaining: Double?
    )

    fun parseApiJson(json: String): ApiJsonStatus? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val captiveMatch = Regex(""""captive"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE).find(trimmed)
            ?: return null
        val captive = captiveMatch.groupValues[1].equals("true", ignoreCase = true)
        val portalUrl = Regex(""""user-portal-url"\s*:\s*"([^"]*)"""")
            .find(trimmed)?.groupValues?.getOrNull(1)
            ?.takeIf { it.isNotBlank() && !it.contains("$(") }
        val secs = Regex(""""seconds-remaining"\s*:\s*(\d+)""")
            .find(trimmed)?.groupValues?.getOrNull(1)?.toLongOrNull()
        val bytes = Regex(""""bytes-remaining"\s*:\s*(\d+(?:\.\d+)?)""")
            .find(trimmed)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        return ApiJsonStatus(captive, portalUrl, secs, bytes)
    }

    fun resolvePackageTotals(profileName: String): Pair<Boolean, PackageSpec> {
        val raw = profileName.trim()
        if (raw.isEmpty() || raw.contains("$(")) {
            return false to PackageSpec(null, 0L)
        }
        val spec = DynamicPackageStore.get(raw)
            ?: PACKAGE_DICTIONARY[raw]
            ?: PACKAGE_DICTIONARY.entries.firstOrNull { it.key.equals(raw, ignoreCase = true) }?.value
            ?: return false to PackageSpec(null, PortalConstants.DEFAULT_VALIDITY_SECS)
        return true to spec
    }

    fun parseBytesValue(rawVal: String?, niceVal: String?): Double {
        val rawClean = rawVal?.trim().orEmpty()
        if (rawClean.isNotEmpty() && !rawClean.contains("$(") && Regex("^\\d+(\\.\\d+)?$").matches(rawClean)) {
            return rawClean.toDoubleOrNull() ?: 0.0
        }
        val str = if (!niceVal.isNullOrBlank() && !niceVal.contains("$(")) niceVal else rawClean
        if (str.isEmpty() || str.contains("$(")) return 0.0
        val numMatch = Regex("""[\d.,]+""").find(str) ?: return 0.0
        val num = numMatch.value.replace(",", "").toDoubleOrNull() ?: return 0.0
        val upper = str.uppercase()
        return when {
            upper.contains("G") -> num * 1024.0 * 1024.0 * 1024.0
            upper.contains("M") -> num * 1024.0 * 1024.0
            upper.contains("K") -> num * 1024.0
            else -> num
        }
    }

    fun formatBytesNice(bytes: Double): String {
        if (bytes.isNaN() || bytes <= 0.0) return "غير محدود"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            "${mb.roundToInt()} MB"
        }
    }

    fun parseTimeSeconds(secsVal: String?, strVal: String?): Long {
        val rawClean = secsVal?.trim().orEmpty()
        if (rawClean.isNotEmpty() && !rawClean.contains("$(") && Regex("^\\d+$").matches(rawClean)) {
            return rawClean.toLongOrNull() ?: 0L
        }
        val str = strVal?.trim().orEmpty()
        if (str.isEmpty() || str.contains("$(")) return 0L
        var total = 0L
        Regex("(\\d+)\\s*w", RegexOption.IGNORE_CASE).find(str)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it * 7L * 86400L }
        Regex("(\\d+)\\s*d", RegexOption.IGNORE_CASE).find(str)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it * 86400L }
        Regex("(\\d+)\\s*h", RegexOption.IGNORE_CASE).find(str)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it * 3600L }
        Regex("(\\d+)\\s*m", RegexOption.IGNORE_CASE).find(str)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it * 60L }
        Regex("(\\d+)\\s*s", RegexOption.IGNORE_CASE).find(str)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it }
        return total
    }

    fun analyzeQuota(
        session: SessionStatus,
        savedMaxBytes: Double,
        savedMaxSecs: Long,
        alert30AlreadySent: Boolean,
        alert15AlreadySent: Boolean
    ): QuotaAnalysis {
        if (session.isProvisional) {
            return QuotaAnalysis(
                updatedMaxBytes = savedMaxBytes,
                updatedMaxSecs = savedMaxSecs,
                alert30Sent = alert30AlreadySent,
                alert15Sent = alert15AlreadySent
            )
        }

        val (pkgMatched, pkgSpec) = resolvePackageTotals(session.activeProfile)

        val remainBytes = parseBytesValue(session.remainBytesRaw, session.remainBytes)
        val limitBytes = parseBytesValue(session.limitBytesTotalRaw, "")
        val usedBytes = parseBytesValue(session.bytesInRaw, session.bytesIn) +
            parseBytesValue(session.bytesOutRaw, session.bytesOut)

        val remainSecs = parseTimeSeconds(session.timeLeftSecs, session.sessionTimeLeft)
        val usedSecs = parseTimeSeconds(session.uptimeSecs, session.uptime)

        var newMaxBytes = savedMaxBytes
        var bytesPct: Double? = null
        val explicitZeroBytes = session.remainBytesRaw.trim() == "0"

        if (pkgMatched && pkgSpec.totalBytes == null) {
            bytesPct = null
        } else if (pkgSpec.totalBytes != null && pkgSpec.totalBytes > 0.0 && (remainBytes > 0.0 || explicitZeroBytes)) {
            bytesPct = min(100.0, (remainBytes / pkgSpec.totalBytes) * 100.0)
        } else if (remainBytes > 0.0 || (explicitZeroBytes && (limitBytes > 0.0 || savedMaxBytes > 0.0))) {
            val sessionTotalBytes = remainBytes + usedBytes
            val totalBytes = max(max(limitBytes, sessionTotalBytes), savedMaxBytes)
            if (totalBytes > newMaxBytes) newMaxBytes = totalBytes
            if (totalBytes > 0.0) {
                bytesPct = min(100.0, (remainBytes / totalBytes) * 100.0)
            }
        }

        var newMaxSecs = savedMaxSecs
        var timePct: Double? = null
        val explicitZeroSecs = session.timeLeftSecs.trim() == "0"
        if (remainSecs > 0L || (explicitZeroSecs && (pkgSpec.totalSecs > 0L || savedMaxSecs > 0L))) {
            if (pkgSpec.totalSecs > 0L) {
                timePct = min(100.0, (remainSecs.toDouble() / pkgSpec.totalSecs.toDouble()) * 100.0)
            } else {
                val sessionTotalSecs = remainSecs + usedSecs
                val totalSecs = max(sessionTotalSecs, savedMaxSecs)
                if (totalSecs > newMaxSecs) newMaxSecs = totalSecs
                if (totalSecs > 0L) {
                    timePct = min(100.0, (remainSecs.toDouble() / totalSecs.toDouble()) * 100.0)
                }
            }
        }

        var activePct: Double? = null
        val details = mutableListOf<String>()
        if (bytesPct != null) {
            activePct = bytesPct
            details.add("الرصيد: ${formatBytesNice(remainBytes)} (${bytesPct.roundToInt()}%)")
        }
        if (timePct != null) {
            if (activePct == null || timePct < activePct) activePct = timePct
            details.add("الوقت: ${session.sessionTimeLeft} (${timePct.roundToInt()}%)")
        }

        val summary = details.joinToString(" · ")
        if (activePct == null) {
            return QuotaAnalysis(
                bytesPct = null,
                timePct = null,
                activePct = null,
                updatedMaxBytes = newMaxBytes,
                updatedMaxSecs = newMaxSecs
            )
        }

        if (activePct > 35.0) {
            return QuotaAnalysis(
                bytesPct = bytesPct,
                timePct = timePct,
                activePct = activePct,
                alert30Sent = false,
                alert15Sent = false,
                summaryText = summary,
                alertLevel = QuotaAlertLevel.NONE,
                updatedMaxBytes = newMaxBytes,
                updatedMaxSecs = newMaxSecs,
                shouldResetAlerts = alert30AlreadySent || alert15AlreadySent
            )
        }

        var nextAlert30 = alert30AlreadySent
        var nextAlert15 = alert15AlreadySent
        var fire30 = false
        var fire15 = false
        val level: QuotaAlertLevel

        if (activePct <= 15.0) {
            level = QuotaAlertLevel.CRIT_15
            if (!alert15AlreadySent) {
                nextAlert15 = true
                nextAlert30 = true
                fire15 = true
            }
        } else if (activePct <= 30.0) {
            level = QuotaAlertLevel.WARN_30
            if (!alert30AlreadySent) {
                nextAlert30 = true
                fire30 = true
            }
        } else {
            level = QuotaAlertLevel.NONE
        }

        return QuotaAnalysis(
            bytesPct = bytesPct,
            timePct = timePct,
            activePct = activePct,
            alert30Sent = nextAlert30,
            alert15Sent = nextAlert15,
            summaryText = summary,
            alertLevel = level,
            updatedMaxBytes = newMaxBytes,
            updatedMaxSecs = newMaxSecs,
            shouldFireNotification30 = fire30,
            shouldFireNotification15 = fire15
        )
    }
}
