package com.example.data

import com.example.model.LoginChallenge
import com.example.model.LogoutSummary
import com.example.model.PortalConstants
import com.example.model.SessionStatus
import com.example.model.SpeedProfile
import com.example.util.MikrotikParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed interface StatusFetchResult {
    data class Connected(val session: SessionStatus) : StatusFetchResult
    data class NotLoggedIn(val challenge: LoginChallenge?) : StatusFetchResult
    data class NetworkError(val message: String) : StatusFetchResult
}

sealed interface LoginAttemptResult {
    data class Success(val session: SessionStatus) : LoginAttemptResult
    data class RouterError(val message: String) : LoginAttemptResult
    data class NetworkError(val message: String) : LoginAttemptResult
}

class HotspotRepository {

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val existing = cookieStore[url.host].orEmpty().associateBy { it.name }.toMutableMap()
                cookies.forEach { existing[it.name] = it }
                cookieStore[url.host] = existing.values.toList()
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val now = System.currentTimeMillis()
                return cookieStore[url.host].orEmpty().filter { it.expiresAt >= now }
            }
        })
        .build()

    private val probeClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(3000, TimeUnit.MILLISECONDS)
            .writeTimeout(3000, TimeUnit.MILLISECONDS)
            .build()
    }

    private sealed interface LoginChallengeFetchResult {
        data class Challenge(val challenge: LoginChallenge) : LoginChallengeFetchResult
        data class AlreadyLoggedIn(val session: SessionStatus) : LoginChallengeFetchResult
        data object Unavailable : LoginChallengeFetchResult
    }

    suspend fun fetchStatus(
        fastProbe: Boolean = false,
        closeConnection: Boolean = false
    ): StatusFetchResult = withContext(Dispatchers.IO) {
        val activeClient = if (fastProbe) probeClient else client
        try {
            val timestamp = System.currentTimeMillis()
            // Fast check via /api.json first if available
            val apiReqBuilder = Request.Builder()
                .url("${PortalConstants.API_URL}?_check=$timestamp")
                .header("Cache-Control", "no-store")
                .get()
            if (closeConnection) apiReqBuilder.header("Connection", "close")
            val apiReq = apiReqBuilder.build()

            var apiPortalUrl: String? = null
            var apiDnsFailure: IOException? = null
            try {
                activeClient.newCall(apiReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val parsed = MikrotikParser.parseApiJson(body)
                        if (parsed != null) {
                            apiPortalUrl = parsed.userPortalUrl
                            if (parsed.captive) {
                                val loginChallenge = fetchLoginChallengeOnly(apiPortalUrl, activeClient, closeConnection)
                                return@withContext StatusFetchResult.NotLoggedIn(loginChallenge)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                if (fastProbe && e is java.net.UnknownHostException) {
                    apiDnsFailure = e
                }
            }

            if (apiDnsFailure != null) {
                return@withContext StatusFetchResult.NetworkError(describeNetworkException(apiDnsFailure))
            }

            // Full status.html fetch
            val statusReqBuilder = Request.Builder()
                .url("${PortalConstants.STATUS_URL}?_poll=$timestamp")
                .header("Cache-Control", "no-store")
                .get()
            if (closeConnection) statusReqBuilder.header("Connection", "close")
            val statusReq = statusReqBuilder.build()

            activeClient.newCall(statusReq).execute().use { resp ->
                val finalUrl = resp.request.url.toString()
                val html = resp.body?.string().orEmpty()
                if (finalUrl.contains("/login") || (MikrotikParser.isLoginPageHtml(html) && !html.contains("window.STATUS_CONFIG"))) {
                    return@withContext StatusFetchResult.NotLoggedIn(MikrotikParser.parseLoginChallenge(html))
                }
                val parsed = MikrotikParser.parseStatusHtml(html)
                if (parsed != null && parsed.loggedIn) {
                    StatusFetchResult.Connected(parsed)
                } else {
                    val challenge = fetchLoginChallengeOnly(apiPortalUrl, activeClient, closeConnection)
                    StatusFetchResult.NotLoggedIn(challenge)
                }
            }
        } catch (e: IOException) {
            StatusFetchResult.NetworkError(describeNetworkException(e))
        } catch (e: Exception) {
            StatusFetchResult.NetworkError("تعذر الاتصال ببوابة شبكة القناص (${e.localizedMessage ?: "خطأ غير متوقع"})")
        }
    }

    private suspend fun fetchStatusWithRetry(
        initialDelayMs: Long = 300L,
        delaysMs: List<Long> = listOf(500L, 850L, 1250L)
    ): StatusFetchResult {
        delay(initialDelayMs)
        var lastResult: StatusFetchResult = StatusFetchResult.NetworkError("تعذر الاتصال ببوابة الشبكة.")
        val totalAttempts = 1 + delaysMs.size
        for (attempt in 0 until totalAttempts) {
            if (attempt > 0) {
                client.connectionPool.evictAll()
                delay(delaysMs[attempt - 1])
            }
            when (val res = fetchStatus(fastProbe = false, closeConnection = true)) {
                is StatusFetchResult.Connected -> return res
                is StatusFetchResult.NotLoggedIn -> return res
                is StatusFetchResult.NetworkError -> lastResult = res
            }
        }
        return lastResult
    }

    suspend fun login(
        cardNumber: String,
        speed: SpeedProfile,
        blockUpdates: Boolean
    ): LoginAttemptResult = withContext(Dispatchers.IO) {
        try {
            val trimmedCard = cardNumber.trim()
            val challengeResult = fetchLoginChallengeInternal(null)
            if (challengeResult is LoginChallengeFetchResult.AlreadyLoggedIn) {
                return@withContext LoginAttemptResult.Success(challengeResult.session)
            }
            val challenge = (challengeResult as? LoginChallengeFetchResult.Challenge)?.challenge

            val domainVal = "${speed.key}_${if (blockUpdates) "block" else "allow"}"
            val hashedPassword = MikrotikParser.computeChapPassword(
                chapId = challenge?.chapId,
                password = "",
                chapChallenge = challenge?.chapChallenge
            )
            val actionUrl = MikrotikParser.ensureSecureUrl(challenge?.actionUrl, "/login")

            val formBody = FormBody.Builder()
                .add("dst", PortalConstants.STATUS_URL)
                .add("popup", "false")
                .add("username", trimmedCard)
                .add("password", hashedPassword)
                .add("domain", domainVal)
                .add("erase-cookie", "true")
                .build()

            val postReq = Request.Builder()
                .url(actionUrl)
                .header("Cache-Control", "no-store")
                .header("Connection", "close")
                .post(formBody)
                .build()

            client.newCall(postReq).execute().use { resp ->
                val finalUrl = resp.request.url.toString()
                val html = resp.body?.string().orEmpty()

                val parsedStatus = MikrotikParser.parseStatusHtml(html)
                if (parsedStatus != null && parsedStatus.loggedIn) {
                    return@withContext LoginAttemptResult.Success(parsedStatus)
                }

                if (MikrotikParser.isLoginPageHtml(html)) {
                    val errChallenge = MikrotikParser.parseLoginChallenge(html)
                    val errMsg = errChallenge.errorMessage
                        ?: "لم يتم قبول رقم الكرت، يرجى التأكد من الرقم وحالة الشبكة."
                    return@withContext LoginAttemptResult.RouterError(errMsg)
                }

                val aloginConfirmed = MikrotikParser.isAloginPageHtml(html) ||
                    finalUrl.contains("alogin", ignoreCase = true) ||
                    finalUrl.contains("status", ignoreCase = true)

                // RouterOS resets pre-auth conntrack sockets right after login; evict stale pooled sockets
                client.connectionPool.evictAll()

                when (val followUp = fetchStatusWithRetry()) {
                    is StatusFetchResult.Connected -> LoginAttemptResult.Success(followUp.session)
                    is StatusFetchResult.NotLoggedIn -> {
                        val msg = followUp.challenge?.errorMessage
                            ?: "فشل تسجيل الدخول، تحقق من رقم الكرت وحاول مرة أخرى."
                        LoginAttemptResult.RouterError(msg)
                    }
                    is StatusFetchResult.NetworkError -> {
                        if (aloginConfirmed) {
                            // MikroTik explicitly served alogin success; transition cleanly and sync live stats in background
                            LoginAttemptResult.Success(
                                SessionStatus(
                                    loggedIn = true,
                                    isProvisional = true,
                                    username = trimmedCard
                                )
                            )
                        } else {
                            LoginAttemptResult.NetworkError(followUp.message)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            LoginAttemptResult.NetworkError(describeNetworkException(e))
        } catch (e: Exception) {
            LoginAttemptResult.NetworkError("حدث خطأ أثناء تسجيل الدخول: ${e.localizedMessage ?: "غير معروف"}")
        }
    }

    suspend fun silentRenewSpeed(
        cardNumber: String,
        speed: SpeedProfile,
        blockUpdates: Boolean,
        logoutUrl: String
    ): LoginAttemptResult = withContext(Dispatchers.IO) {
        try {
            val secureLogout = MikrotikParser.ensureSecureUrl(logoutUrl, "/logout")
            val logoutBody = FormBody.Builder()
                .add("erase-cookie", "true")
                .build()
            val logoutReq = Request.Builder()
                .url(secureLogout)
                .header("Cache-Control", "no-store")
                .header("Connection", "close")
                .post(logoutBody)
                .build()

            runCatching {
                client.newCall(logoutReq).execute().use { /* complete logout */ }
            }
            client.connectionPool.evictAll()
            delay(350)
            login(cardNumber, speed, blockUpdates)
        } catch (e: IOException) {
            LoginAttemptResult.NetworkError(describeNetworkException(e))
        }
    }

    suspend fun logout(
        logoutUrl: String?,
        currentSession: SessionStatus?
    ): LogoutSummary = withContext(Dispatchers.IO) {
        val secureLogout = MikrotikParser.ensureSecureUrl(logoutUrl, "/logout")
        val body = FormBody.Builder()
            .add("erase-cookie", "true")
            .build()
        val req = Request.Builder()
            .url(secureLogout)
            .header("Cache-Control", "no-store")
            .header("Connection", "close")
            .post(body)
            .build()

        return@withContext try {
            client.newCall(req).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                client.connectionPool.evictAll()
                if (MikrotikParser.isLogoutPageHtml(html)) {
                    MikrotikParser.parseLogoutHtml(html, currentSession)
                } else {
                    LogoutSummary(
                        username = currentSession?.username?.ifBlank { "—" } ?: "—",
                        uptime = currentSession?.uptime ?: "—",
                        timeLeft = currentSession?.sessionTimeLeft ?: "—",
                        bytesIn = currentSession?.bytesIn ?: "—",
                        bytesOut = currentSession?.bytesOut ?: "—"
                    )
                }
            }
        } catch (_: Exception) {
            LogoutSummary(
                username = currentSession?.username?.ifBlank { "—" } ?: "—",
                uptime = currentSession?.uptime ?: "—",
                timeLeft = currentSession?.sessionTimeLeft ?: "—",
                bytesIn = currentSession?.bytesIn ?: "—",
                bytesOut = currentSession?.bytesOut ?: "—"
            )
        }
    }

    private fun fetchLoginChallengeOnly(
        portalUrlOverride: String?,
        httpClient: OkHttpClient = client,
        closeConnection: Boolean = false
    ): LoginChallenge? {
        return (fetchLoginChallengeInternal(portalUrlOverride, httpClient, closeConnection)
            as? LoginChallengeFetchResult.Challenge)?.challenge
    }

    private fun fetchLoginChallengeInternal(
        portalUrlOverride: String?,
        httpClient: OkHttpClient = client,
        closeConnection: Boolean = false
    ): LoginChallengeFetchResult {
        val target = MikrotikParser.ensureSecureUrl(portalUrlOverride, "/login")
        val reqBuilder = Request.Builder()
            .url("$target?_t=${System.currentTimeMillis()}")
            .header("Cache-Control", "no-store")
            .get()
        if (closeConnection) reqBuilder.header("Connection", "close")
        val req = reqBuilder.build()

        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                val finalUrl = resp.request.url.toString()
                val html = resp.body?.string().orEmpty()
                val parsedStatus = MikrotikParser.parseStatusHtml(html)
                if (parsedStatus != null && parsedStatus.loggedIn) {
                    return@use LoginChallengeFetchResult.AlreadyLoggedIn(parsedStatus)
                }
                if (!MikrotikParser.isLoginPageHtml(html) &&
                    (finalUrl.contains("/status", ignoreCase = true) || MikrotikParser.isAloginPageHtml(html))
                ) {
                    return@use LoginChallengeFetchResult.Unavailable
                }
                LoginChallengeFetchResult.Challenge(MikrotikParser.parseLoginChallenge(html))
            }
        }.getOrElse { LoginChallengeFetchResult.Unavailable }
    }

    private fun describeNetworkException(e: IOException): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("timeout") ->
                "تأخر الاتصال بالشبكة. تأكد من اتصالك بشبكة الواي فاي الخاصة بالقناص ثم أعد المحاولة."
            msg.contains("unable to resolve host") || msg.contains("unknownhost") ->
                "تعذر الوصول إلى بوابة شبكة القناص (alqanas.cc.cd). تأكد من اتصالك بشبكة الواي فاي المحلية."
            else ->
                "تعذر الاتصال ببوابة الشبكة، تأكد من اتصالك بشبكة القناص وحاول مرة أخرى."
        }
    }
}
