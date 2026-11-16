package com.example.ui

import com.example.data.RemoteConfigRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HotspotPreferences
import com.example.data.HotspotRepository
import com.example.data.LoginAttemptResult
import com.example.data.StatusFetchResult
import com.example.data.StoredPrefsSnapshot
import com.example.model.LogoutSummary
import com.example.model.PortalConstants
import com.example.model.QuotaAnalysis
import com.example.model.SavedCard
import com.example.model.SessionStatus
import com.example.model.SpeedProfile
import com.example.model.ThemeMode
import com.example.model.WalletGuide
import com.example.notification.QuotaNotifier
import com.example.util.MikrotikParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HotspotUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cardInput: String = "",
    val selectedSpeed: SpeedProfile? = null,
    val blockUpdates: Boolean = false,
    val hasSelectedPlan: Boolean = false,
    val savedCards: List<SavedCard> = emptyList(),
    val validationAlert: String? = null,
    val routerError: String? = null,
    val isCheckingInitialStatus: Boolean = true,
    val isLoggingIn: Boolean = false,
    val isApplyingSpeed: Boolean = false,
    val isRefreshingStatus: Boolean = false,
    val isLoggingOut: Boolean = false,
    val session: SessionStatus? = null,
    val quotaAnalysis: QuotaAnalysis = QuotaAnalysis(),
    val logoutSummary: LogoutSummary? = null,
    val hasNotificationPermission: Boolean = false,
    val showSupportModal: Boolean = false,
    val showSavedCardsModal: Boolean = false,
    val showNotifGuideModal: Boolean = false,
    val activeVideoGuide: WalletGuide? = null,
    val currentRoute: String = PortalConstants.ROUTE_LOGIN
)

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class NavigateTo(val route: String) : UiEvent
    data object RequestNotificationPermission : UiEvent
}

class HotspotViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = HotspotPreferences(application.applicationContext)
    private val repository = HotspotRepository()
    private val remoteRepo = RemoteConfigRepository()
    private val notifier = QuotaNotifier(application.applicationContext)

    private val _uiState = MutableStateFlow(
        HotspotUiState(hasNotificationPermission = notifier.hasNotificationPermission())
    )
    val uiState: StateFlow<HotspotUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null
    private var prefsInitialized = false

    init {
        viewModelScope.launch {
            prefs.snapshotFlow.collect { snapshot ->
                applyPrefsSnapshot(snapshot)
            }
       viewModelScope.launch {
          while (isActive) {
              remoteRepo.fetchConfig()
              if (notifier.hasNotificationPermission()) {
                  val lastId = prefs.getLastAdminNotificationId()
                  val newNotifs = remoteRepo.fetchNewNotifications(afterId = lastId)
                  if (newNotifs.isNotEmpty()) {
                      var maxId = lastId
                      for (n in newNotifs) {
                          if (n.title.isNotBlank() && n.body.isNotBlank()) {
                              notifier.notifyAdminBroadcast(n.id, n.title, n.body, n.highPriority)
                          }
                          if (n.id > maxId) maxId = n.id
                      }
                      prefs.setLastAdminNotificationId(maxId)
                  }
              }
              delay(10_000L)
          }
      }
        }
        checkInitialSession()
    }

    private fun applyPrefsSnapshot(snapshot: StoredPrefsSnapshot) {
        _uiState.update { current ->
            if (!prefsInitialized) {
                prefsInitialized = true
                current.copy(
                    themeMode = snapshot.themeMode,
                    cardInput = if (current.cardInput.isEmpty()) snapshot.lastCard else current.cardInput,
                    selectedSpeed = current.selectedSpeed ?: snapshot.lastSpeed,
                    blockUpdates = snapshot.blockUpdates,
                    savedCards = snapshot.savedCards
                )
            } else {
                current.copy(
                    themeMode = snapshot.themeMode,
                    savedCards = snapshot.savedCards
                )
            }
        }
    }

    fun refreshNotificationPermissionState() {
        _uiState.update { it.copy(hasNotificationPermission = notifier.hasNotificationPermission()) }
    }

    private fun checkInitialSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingInitialStatus = true) }
            when (val res = repository.fetchStatus(fastProbe = true)) {
                is StatusFetchResult.Connected -> {
                    handleConnectedSession(res.session, navigate = true)
                }
                is StatusFetchResult.NotLoggedIn -> {
                    _uiState.update {
                        it.copy(
                            isCheckingInitialStatus = false,
                            routerError = res.challenge?.errorMessage
                        )
                    }
                }
                is StatusFetchResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isCheckingInitialStatus = false
                        )
                    }
                }
            }
        }
    }

    fun onCardInputChange(value: String) {
        val filtered = value.filter { !it.isWhitespace() }
        _uiState.update { it.copy(cardInput = filtered, validationAlert = null, routerError = null) }
    }

    fun onSelectSpeed(speed: SpeedProfile) {
        _uiState.update { it.copy(selectedSpeed = speed, validationAlert = null) }
    }

    fun onToggleBlockUpdates(block: Boolean) {
        _uiState.update { it.copy(blockUpdates = block) }
    }

    fun onApplyPlan(speed: SpeedProfile, blockUpdates: Boolean) {
        _uiState.update {
            it.copy(
                selectedSpeed = speed,
                blockUpdates = blockUpdates,
                hasSelectedPlan = true,
                validationAlert = null
            )
        }
    }

    fun onSelectSavedCard(card: SavedCard) {
        val speed = SpeedProfile.fromKey(card.speedKey) ?: _uiState.value.selectedSpeed
        _uiState.update {
            it.copy(
                cardInput = card.code,
                selectedSpeed = speed,
                validationAlert = null,
                routerError = null
            )
        }
    }

    fun onSelectTheme(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun openSupportModal() = _uiState.update { it.copy(showSupportModal = true) }
    fun closeSupportModal() = _uiState.update { it.copy(showSupportModal = false) }

    fun openSavedCardsModal() = _uiState.update { it.copy(showSavedCardsModal = true) }
    fun closeSavedCardsModal() = _uiState.update { it.copy(showSavedCardsModal = false) }

    fun openNotifGuideModal() = _uiState.update { it.copy(showNotifGuideModal = true) }
    fun closeNotifGuideModal() {
        _uiState.update { it.copy(showNotifGuideModal = false) }
        viewModelScope.launch { prefs.dismissNotifPrompt() }
    }

    fun openWalletVideo(guide: WalletGuide) = _uiState.update { it.copy(activeVideoGuide = guide) }
    fun closeWalletVideo() = _uiState.update { it.copy(activeVideoGuide = null) }

    fun onNotificationHeaderOrActionClick() {
        refreshNotificationPermissionState()
        if (notifier.hasNotificationPermission()) {
            val state = _uiState.value
            val isActive = state.session != null && state.currentRoute == PortalConstants.ROUTE_STATUS
            notifier.notifyTestOrEnabled(isActive, state.quotaAnalysis.summaryText)
            _events.tryEmit(UiEvent.ShowToast("تم إرسال إشعار تجريبي لشريط النظام ✓"))
        } else {
            _events.tryEmit(UiEvent.RequestNotificationPermission)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        refreshNotificationPermissionState()
        closeNotifGuideModal()
        if (granted) {
            val state = _uiState.value
            val isActive = state.session != null && state.currentRoute == PortalConstants.ROUTE_STATUS
            notifier.notifyTestOrEnabled(isActive, state.quotaAnalysis.summaryText)
            _events.tryEmit(UiEvent.ShowToast("تم تفعيل تنبيهات 30% و 15% بنجاح"))
        } else {
            _events.tryEmit(UiEvent.ShowToast("يرجى السماح بالإشعارات من إعدادات التطبيق لتصلك تنبيهات الرصيد"))
        }
    }

    fun submitLogin() {
        val state = _uiState.value
        if (state.isLoggingIn) return

        val card = state.cardInput.trim()
        if (card.isEmpty()) {
            _uiState.update { it.copy(validationAlert = "الرجاء إدخال رقم الكرت أولاً") }
            return
        }
        val speed = state.selectedSpeed
        if (speed == null) {
            _uiState.update { it.copy(validationAlert = "الرجاء اختيار السرعة المناسبة أولاً!") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, validationAlert = null, routerError = null) }
            prefs.saveLoginSettings(card, speed, state.blockUpdates, addToHistory = true)

            when (val result = repository.login(card, speed, state.blockUpdates)) {
                is LoginAttemptResult.Success -> {
                    _uiState.update { it.copy(isLoggingIn = false) }
                    handleConnectedSession(result.session, navigate = true)
                    _events.tryEmit(UiEvent.ShowToast("تم تسجيل الدخول بنجاح"))
                }
                is LoginAttemptResult.RouterError -> {
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            routerError = result.message
                        )
                    }
                }
                is LoginAttemptResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            validationAlert = result.message
                        )
                    }
                }
            }
        }
    }

    fun refreshStatusManually() {
        if (_uiState.value.isRefreshingStatus) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingStatus = true) }
            when (val res = repository.fetchStatus()) {
                is StatusFetchResult.Connected -> {
                    handleConnectedSession(res.session, navigate = false)
                }
                is StatusFetchResult.NotLoggedIn -> {
                    stopPolling()
                    _uiState.update {
                        it.copy(
                            isRefreshingStatus = false,
                            session = null,
                            routerError = res.challenge?.errorMessage,
                            currentRoute = PortalConstants.ROUTE_LOGIN
                        )
                    }
                    _events.tryEmit(UiEvent.NavigateTo(PortalConstants.ROUTE_LOGIN))
                }
                is StatusFetchResult.NetworkError -> {
                    _uiState.update { it.copy(isRefreshingStatus = false) }
                    _events.tryEmit(UiEvent.ShowToast(res.message))
                }
            }
        }
    }

    fun applySpeedAndUpdatesChange() {
        val state = _uiState.value
        val speed = state.selectedSpeed ?: SpeedProfile.DEFAULT
        if (state.isApplyingSpeed) return

        val card = state.session?.username?.takeIf { it.isNotBlank() } ?: state.cardInput.trim()
        if (card.isBlank()) {
            _events.tryEmit(UiEvent.ShowToast("رقم الكرت غير متوفر لتجديد الجلسة"))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingSpeed = true) }
            prefs.saveSpeedPreferences(speed, state.blockUpdates)

            val logoutUrl = state.session?.linkLogout ?: PortalConstants.LOGOUT_URL
            when (val res = repository.silentRenewSpeed(card, speed, state.blockUpdates, logoutUrl)) {
                is LoginAttemptResult.Success -> {
                    _uiState.update { it.copy(isApplyingSpeed = false) }
                    handleConnectedSession(res.session, navigate = false)
                    _events.tryEmit(UiEvent.ShowToast("تم تحديث سرعتك إلى ${speed.title}"))
                }
                is LoginAttemptResult.RouterError -> {
                    _uiState.update { it.copy(isApplyingSpeed = false) }
                    _events.tryEmit(UiEvent.ShowToast(res.message))
                }
                is LoginAttemptResult.NetworkError -> {
                    _uiState.update { it.copy(isApplyingSpeed = false) }
                    _events.tryEmit(UiEvent.ShowToast(res.message))
                }
            }
        }
    }

    fun executeLogout() {
        val state = _uiState.value
        if (state.isLoggingOut) return
        stopPolling()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            val summary = repository.logout(state.session?.linkLogout, state.session)
            _uiState.update {
                it.copy(
                    isLoggingOut = false,
                    session = null,
                    logoutSummary = summary,
                    currentRoute = PortalConstants.ROUTE_LOGOUT
                )
            }
            _events.tryEmit(UiEvent.NavigateTo(PortalConstants.ROUTE_LOGOUT))
        }
    }

    fun executeLogoutAndNewCard() {
        val state = _uiState.value
        if (state.isLoggingOut) return
        stopPolling()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            repository.logout(state.session?.linkLogout, state.session)
            prefs.clearLastCard()
            _uiState.update {
                it.copy(
                    isLoggingOut = false,
                    session = null,
                    cardInput = "",
                    validationAlert = null,
                    routerError = null,
                    currentRoute = PortalConstants.ROUTE_LOGIN
                )
            }
            _events.tryEmit(UiEvent.NavigateTo(PortalConstants.ROUTE_LOGIN))
        }
    }

    fun navigateFromLogoutToLogin() {
        _uiState.update {
            it.copy(
                validationAlert = null,
                routerError = null,
                currentRoute = PortalConstants.ROUTE_LOGIN
            )
        }
        _events.tryEmit(UiEvent.NavigateTo(PortalConstants.ROUTE_LOGIN))
    }

    fun onCopiedWalletCode(code: String) {
        _events.tryEmit(UiEvent.ShowToast("تم نسخ كود المحفظة: $code"))
    }

    private suspend fun handleConnectedSession(session: SessionStatus, navigate: Boolean) {
        val cardId = session.username.ifBlank { _uiState.value.cardInput.ifBlank { "default_card" } }
        val cardState = prefs.getCardQuotaState(cardId)

        val analysis = MikrotikParser.analyzeQuota(
            session = session,
            savedMaxBytes = cardState.maxBytes,
            savedMaxSecs = cardState.maxSecs,
            alert30AlreadySent = cardState.alert30Sent,
            alert15AlreadySent = cardState.alert15Sent
        )

        prefs.updateCardQuotaState(
            cardId = cardId,
            maxBytes = analysis.updatedMaxBytes,
            maxSecs = analysis.updatedMaxSecs,
            alert30Sent = analysis.alert30Sent,
            alert15Sent = analysis.alert15Sent
        )

        if (analysis.shouldFireNotification15) {
            notifier.notifyQuota15(analysis.summaryText)
        } else if (analysis.shouldFireNotification30) {
            notifier.notifyQuota30(analysis.summaryText)
        }

        _uiState.update { current ->
            current.copy(
                isCheckingInitialStatus = false,
                isRefreshingStatus = session.isProvisional,
                session = session,
                selectedSpeed = current.selectedSpeed ?: SpeedProfile.DEFAULT,
                quotaAnalysis = analysis,
                currentRoute = if (navigate) PortalConstants.ROUTE_STATUS else current.currentRoute
            )
        }

        startPolling()
        if (navigate) {
            _events.tryEmit(UiEvent.NavigateTo(PortalConstants.ROUTE_STATUS))
        }
        if (session.isProvisional) {
            scheduleProvisionalSync()
        }
    }

    private fun scheduleProvisionalSync() {
        viewModelScope.launch {
            val delays = listOf(1200L, 2000L)
            for (waitMs in delays) {
                delay(waitMs)
                when (val res = repository.fetchStatus(fastProbe = false, closeConnection = true)) {
                    is StatusFetchResult.Connected -> {
                        handleConnectedSession(res.session, navigate = false)
                        return@launch
                    }
                    is StatusFetchResult.NotLoggedIn -> {
                        stopPolling()
                        _uiState.update {
                            it.copy(
                                isRefreshingStatus = false,
                                session = null,
                                routerError = res.challenge?.errorMessage,
                                currentRoute = PortalConstants.ROUTE_LOGIN
                            )
                        }
                        _events.tryEmit(UiEvent.NavigateTo(PortalConstants.ROUTE_LOGIN))
                        return@launch
                    }
                    is StatusFetchResult.NetworkError -> Unit
                }
            }
            _uiState.update { it.copy(isRefreshingStatus = false) }
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                when (val res = repository.fetchStatus()) {
                    is StatusFetchResult.Connected -> {
                        handleConnectedSession(res.session, navigate = false)
                    }
                    is StatusFetchResult.NotLoggedIn -> {
                        stopPolling()
                        _uiState.update {
                            it.copy(
                                session = null,
                                routerError = res.challenge?.errorMessage,
                                currentRoute = PortalConstants.ROUTE_LOGIN
                            )
                        }
                        _events.tryEmit(UiEvent.NavigateTo(PortalConstants.ROUTE_LOGIN))
                    }
                    is StatusFetchResult.NetworkError -> {
                        // Keep polling silently on next cycle if Wi-Fi momentarily blips
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
