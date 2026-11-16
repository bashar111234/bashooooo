package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.PortalConstants
import com.example.notification.NetworkSyncWorker
import com.example.model.ThemeMode
import com.example.ui.HotspotViewModel
import com.example.ui.UiEvent
import com.example.ui.components.NotificationGuideModal
import com.example.ui.components.SavedCardsModal
import com.example.ui.components.SupportModal
import com.example.ui.components.TelegramWallpaper
import com.example.ui.components.WalletVideoModal
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.LogoutScreen
import com.example.ui.screens.StatusScreen
import com.example.ui.theme.AlqanasTheme
import com.example.ui.theme.LocalAlqanasPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NetworkSyncWorker.schedule(applicationContext)
        setContent {
            AlqanasHotspotApp()
        }
    }
}

@Composable
fun AlqanasHotspotApp(
    viewModel: HotspotViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.NavigateTo -> {
                    val current = navController.currentDestination?.route
                    if (current != event.route) {
                        navController.navigate(event.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                is UiEvent.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onPermissionResult(true)
                    }
                }
            }
        }
    }

    LaunchedEffect(state.currentRoute) {
        val current = navController.currentDestination?.route
        if (current != null && current != state.currentRoute) {
            navController.navigate(state.currentRoute) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlqanasTheme(themeMode = state.themeMode) {
            val palette = LocalAlqanasPalette.current

            Box(modifier = Modifier.fillMaxSize()) {
                TelegramWallpaper(modifier = Modifier.fillMaxSize())

                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = palette.accent,
                                contentColor = palette.accentInk,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = PortalConstants.ROUTE_LOGIN
                        ) {
                            composable(PortalConstants.ROUTE_LOGIN) {
                                LoginScreen(
                                    state = state,
                                    onCardInputChange = viewModel::onCardInputChange,
                                    onSelectSpeed = viewModel::onSelectSpeed,
                                    onToggleBlockUpdates = viewModel::onToggleBlockUpdates,
                                    onApplyPlan = viewModel::onApplyPlan,
                                    onSubmitLogin = viewModel::submitLogin,
                                    onSelectTheme = viewModel::onSelectTheme,
                                    onOpenNotifGuide = viewModel::openNotifGuideModal,
                                    onOpenSupport = viewModel::openSupportModal,
                                    onOpenSavedCards = viewModel::openSavedCardsModal,
                                    onOpenWalletVideo = viewModel::openWalletVideo,
                                    onCopiedWalletCode = viewModel::onCopiedWalletCode
                                )
                            }

                            composable(PortalConstants.ROUTE_STATUS) {
                                StatusScreen(
                                    state = state,
                                    onSelectSpeed = viewModel::onSelectSpeed,
                                    onToggleBlockUpdates = viewModel::onToggleBlockUpdates,
                                    onApplySpeedChanges = viewModel::applySpeedAndUpdatesChange,
                                    onRefreshStatus = viewModel::refreshStatusManually,
                                    onSelectTheme = viewModel::onSelectTheme,
                                    onNotificationClick = viewModel::onNotificationHeaderOrActionClick,
                                    onOpenSupport = viewModel::openSupportModal,
                                    onOpenWalletVideo = viewModel::openWalletVideo,
                                    onLogout = viewModel::executeLogout,
                                    onLogoutAndNewCard = viewModel::executeLogoutAndNewCard
                                )
                            }

                            composable(PortalConstants.ROUTE_LOGOUT) {
                                LogoutScreen(
                                    state = state,
                                    onSelectTheme = viewModel::onSelectTheme,
                                    onOpenNotifGuide = viewModel::openNotifGuideModal,
                                    onOpenSupport = viewModel::openSupportModal,
                                    onLoginAgain = viewModel::navigateFromLogoutToLogin
                                )
                            }
                        }
                    }
                }

                if (state.showSupportModal) {
                    SupportModal(onDismiss = viewModel::closeSupportModal)
                }

                if (state.showSavedCardsModal) {
                    SavedCardsModal(
                        savedCards = state.savedCards,
                        onSelectCard = viewModel::onSelectSavedCard,
                        onDismiss = viewModel::closeSavedCardsModal
                    )
                }

                if (state.showNotifGuideModal) {
                    NotificationGuideModal(
                        onEnableNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.onPermissionResult(true)
                            }
                        },
                        onDismiss = viewModel::closeNotifGuideModal
                    )
                }

                state.activeVideoGuide?.let { guide ->
                    WalletVideoModal(
                        guide = guide,
                        onDismiss = viewModel::closeWalletVideo
                    )
                }
            }
        }
    }
}
