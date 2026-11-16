package com.example

import com.example.model.QuotaAlertLevel
import com.example.model.SessionStatus
import com.example.util.MikrotikParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MikrotikParserTest {

    @Test
    fun maskCardNumber_hidesDigitsAfterFirstFour() {
        assertEquals("1234****", MikrotikParser.maskCardNumber("12345678"))
        assertEquals("9876", MikrotikParser.maskCardNumber("9876"))
    }

    @Test
    fun translateMikrotikError_translatesKnownErrors() {
        assertEquals(
            "رقم الكرت غير صحيح، يرجى التحقق من الرقم المدخل",
            MikrotikParser.translateMikrotikError("invalid username or password")
        )
        assertEquals(
            "رقم الكرت غير موجود في النظام",
            MikrotikParser.translateMikrotikError("user <12345> not found")
        )
        assertEquals(
            "هذا الكرت مستخدم حالياً في جهاز آخر",
            MikrotikParser.translateMikrotikError("no more sessions are allowed for user <5555>")
        )
    }

    @Test
    fun parseStatusHtml_extractsSessionConfig() {
        val sampleHtml = """
            <script id="mikrotikConfigScript">
              window.STATUS_CONFIG = {
                loggedIn: "yes",
                username: "77889900",
                userProfile: "200R",
                actualProfile: "200R",
                bytesIn: "150.0 MiB",
                bytesOut: "20.0 MiB",
                remainBytes: "200.0 MiB",
                remainBytesRaw: "209715200",
                limitBytesTotalRaw: "838860800",
                bytesInRaw: "157286400",
                bytesOutRaw: "20971520",
                sessionTimeLeft: "2h10m",
                timeLeftSecs: "7800",
                uptime: "1h20m",
                uptimeSecs: "4800",
                linkLogout: "http://alqanas.cc.cd/logout",
                linkLogin: "http://alqanas.cc.cd/login",
                linkStatus: "http://alqanas.cc.cd/status.html"
              };
            </script>
        """.trimIndent()

        val status = MikrotikParser.parseStatusHtml(sampleHtml)
        assertNotNull(status)
        assertTrue(status!!.loggedIn)
        assertEquals("77889900", status.username)
        assertEquals("200R", status.activeProfile)
        assertEquals("https://alqanas.cc.cd/logout", status.linkLogout)
    }

    @Test
    fun analyzeQuota_triggers30And15ThresholdsFromPackageDictionary() {
        // 200R is 800 MiB (838860800 bytes), 8 hours (28800 secs).
        // 200 MiB remaining = 25% -> triggers WARN_30
        val warnSession = SessionStatus(
            loggedIn = true,
            username = "12345678",
            userProfile = "200R",
            remainBytesRaw = (200L * 1024L * 1024L).toString(),
            timeLeftSecs = (4L * 3600L).toString()
        )
        val warnResult = MikrotikParser.analyzeQuota(
            session = warnSession,
            savedMaxBytes = 0.0,
            savedMaxSecs = 0L,
            alert30AlreadySent = false,
            alert15AlreadySent = false
        )
        assertEquals(QuotaAlertLevel.WARN_30, warnResult.alertLevel)
        assertTrue(warnResult.shouldFireNotification30)
        assertFalse(warnResult.shouldFireNotification15)

        // 100 MiB remaining = 12.5% -> triggers CRIT_15
        val critSession = warnSession.copy(
            remainBytesRaw = (100L * 1024L * 1024L).toString()
        )
        val critResult = MikrotikParser.analyzeQuota(
            session = critSession,
            savedMaxBytes = 0.0,
            savedMaxSecs = 0L,
            alert30AlreadySent = true,
            alert15AlreadySent = false
        )
        assertEquals(QuotaAlertLevel.CRIT_15, critResult.alertLevel)
        assertTrue(critResult.shouldFireNotification15)
    }

    @Test
    fun parseApiJson_handlesCaptiveFalseAndTrue() {
        val parsed = MikrotikParser.parseApiJson(
            """{"captive": false, "user-portal-url": "https://alqanas.cc.cd/status.html", "seconds-remaining": 3600}"""
        )
        assertNotNull(parsed)
        assertFalse(parsed!!.captive)
        assertEquals(3600L, parsed.secondsRemaining)

        assertNull(MikrotikParser.parseApiJson("not-json"))
    }

    @Test
    fun isAloginPageHtml_detectsSuccessfulRedirectAndGreeting() {
        val aloginHtml = """
            <html><head><meta http-equiv="refresh" content="2; url=https://alqanas.cc.cd/status.html"></head>
            <body><h1>تم الدخول بنجاح</h1></body></html>
        """.trimIndent()
        assertTrue(MikrotikParser.isAloginPageHtml(aloginHtml))

        val errorLoginHtml = """
            <html><form name="sendin" action="https://alqanas.cc.cd/login"><input name="username"></form></html>
        """.trimIndent()
        assertFalse(MikrotikParser.isAloginPageHtml(errorLoginHtml))
    }

    @Test
    fun analyzeQuota_withEmptyProfileUsesUptimePlusRemainInsteadOf30Days() {
        // Fresh 4h card with empty userProfile & actualProfile: remain = 2h, uptime = 2h -> 50% (NOT divided by 30 days!)
        val session = SessionStatus(
            loggedIn = true,
            username = "15",
            userProfile = "",
            actualProfile = "",
            timeLeftSecs = (2L * 3600L).toString(),
            uptimeSecs = (2L * 3600L).toString()
        )
        val result = MikrotikParser.analyzeQuota(
            session = session,
            savedMaxBytes = 0.0,
            savedMaxSecs = 0L,
            alert30AlreadySent = false,
            alert15AlreadySent = false
        )
        assertNotNull(result.timePct)
        assertEquals(50.0, result.timePct!!, 0.1)
        assertEquals(4L * 3600L, result.updatedMaxSecs)
    }

    @Test
    fun analyzeQuota_handlesExplicitZeroRemainingOnFinitePackage() {
        val depleted = SessionStatus(
            loggedIn = true,
            username = "12345",
            userProfile = "200R",
            remainBytesRaw = "0",
            timeLeftSecs = "0"
        )
        val result = MikrotikParser.analyzeQuota(
            session = depleted,
            savedMaxBytes = 0.0,
            savedMaxSecs = 0L,
            alert30AlreadySent = true,
            alert15AlreadySent = false
        )
        assertNotNull(result.bytesPct)
        assertEquals(0.0, result.bytesPct!!, 0.01)
        assertNotNull(result.timePct)
        assertEquals(0.0, result.timePct!!, 0.01)
        assertEquals(QuotaAlertLevel.CRIT_15, result.alertLevel)
        assertTrue(result.shouldFireNotification15)
    }
}
