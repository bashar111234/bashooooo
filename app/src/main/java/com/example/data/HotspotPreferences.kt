package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SavedCard
import com.example.model.SpeedProfile
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alqanas_prefs")

data class StoredPrefsSnapshot(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastCard: String = "",
    val lastSpeed: SpeedProfile? = null,
    val blockUpdates: Boolean = false,
    val savedCards: List<SavedCard> = emptyList(),
    val notifPromptDismissed: Boolean = false
)

class HotspotPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("alqannas_theme")
        val LAST_CARD = stringPreferencesKey("alqannas_last_card")
        val LAST_SPEED = stringPreferencesKey("alqannas_last_speed")
        val BLOCK_UPDATES = booleanPreferencesKey("alqannas_block_updates")
        val SAVED_CARDS_JSON = stringPreferencesKey("alqannas_cards_history")
        val NOTIF_DISMISSED = booleanPreferencesKey("alqannas_notif_dismissed")
        val LAST_ADMIN_NOTIF_ID = intPreferencesKey("alqannas_last_admin_notif_id")
    }

    val snapshotFlow: Flow<StoredPrefsSnapshot> = context.dataStore.data.map { prefs ->
        StoredPrefsSnapshot(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            lastCard = prefs[Keys.LAST_CARD].orEmpty(),
            lastSpeed = SpeedProfile.fromKey(prefs[Keys.LAST_SPEED]),
            blockUpdates = prefs[Keys.BLOCK_UPDATES] ?: false,
            savedCards = decodeSavedCards(prefs[Keys.SAVED_CARDS_JSON]),
            notifPromptDismissed = prefs[Keys.NOTIF_DISMISSED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun saveLoginSettings(card: String, speed: SpeedProfile, blockUpdates: Boolean, addToHistory: Boolean) {
        val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale("ar", "YE")).format(Date())
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_CARD] = card
            prefs[Keys.LAST_SPEED] = speed.key
            prefs[Keys.BLOCK_UPDATES] = blockUpdates

            if (addToHistory && card.isNotBlank()) {
                val existing = decodeSavedCards(prefs[Keys.SAVED_CARDS_JSON])
                    .filterNot { it.code == card }
                val updated = (listOf(SavedCard(card, speed.key, dateStr)) + existing).take(5)
                prefs[Keys.SAVED_CARDS_JSON] = encodeSavedCards(updated)
            }
        }
    }

    suspend fun saveSpeedPreferences(speed: SpeedProfile, blockUpdates: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SPEED] = speed.key
            prefs[Keys.BLOCK_UPDATES] = blockUpdates
        }
    }

    suspend fun clearLastCard() {
        context.dataStore.edit { it.remove(Keys.LAST_CARD) }
    }

    suspend fun dismissNotifPrompt() {
        context.dataStore.edit { it[Keys.NOTIF_DISMISSED] = true }
    }

    suspend fun getLastAdminNotificationId(): Int {
        return context.dataStore.data.first()[Keys.LAST_ADMIN_NOTIF_ID] ?: 0
    }

    suspend fun setLastAdminNotificationId(id: Int) {
        context.dataStore.edit { it[Keys.LAST_ADMIN_NOTIF_ID] = id }
    }

    suspend fun getCardQuotaState(cardId: String): CardQuotaState {
        val safeId = cardId.ifBlank { "default_card" }
        val prefs = context.dataStore.data.first()
        return CardQuotaState(
            maxBytes = prefs[doublePreferencesKey("qannas_max_bytes_$safeId")] ?: 0.0,
            maxSecs = prefs[longPreferencesKey("qannas_max_secs_$safeId")] ?: 0L,
            alert30Sent = prefs[booleanPreferencesKey("qannas_alert30_$safeId")] ?: false,
            alert15Sent = prefs[booleanPreferencesKey("qannas_alert15_$safeId")] ?: false
        )
    }

    suspend fun updateCardQuotaState(
        cardId: String,
        maxBytes: Double,
        maxSecs: Long,
        alert30Sent: Boolean,
        alert15Sent: Boolean
    ) {
        val safeId = cardId.ifBlank { "default_card" }
        context.dataStore.edit { prefs ->
            if (maxBytes > 0.0) {
                prefs[doublePreferencesKey("qannas_max_bytes_$safeId")] = maxBytes
            }
            if (maxSecs > 0L) {
                prefs[longPreferencesKey("qannas_max_secs_$safeId")] = maxSecs
            }
            prefs[booleanPreferencesKey("qannas_alert30_$safeId")] = alert30Sent
            prefs[booleanPreferencesKey("qannas_alert15_$safeId")] = alert15Sent
        }
    }

    private fun decodeSavedCards(raw: String?): List<SavedCard> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val code = obj.optString("code").trim()
                    if (code.isNotEmpty()) {
                        add(
                            SavedCard(
                                code = code,
                                speedKey = obj.optString("speed").takeIf { it.isNotBlank() },
                                date = obj.optString("date").orEmpty()
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeSavedCards(list: List<SavedCard>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("code", item.code)
                    put("speed", item.speedKey ?: "")
                    put("date", item.date)
                }
            )
        }
        return arr.toString()
    }
}

data class CardQuotaState(
    val maxBytes: Double,
    val maxSecs: Long,
    val alert30Sent: Boolean,
    val alert15Sent: Boolean
)
