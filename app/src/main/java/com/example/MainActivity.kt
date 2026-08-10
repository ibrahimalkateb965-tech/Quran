package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.security.TrialManager
import com.example.ui.screens.QuranPlayerScreen
import com.example.ui.screens.TrialExpiredScreen
import com.example.ui.theme.QuranBlindTheme
import com.example.ui.viewmodel.QuranViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.view.ViewConfiguration
import com.example.accessibility.PendingBlindActionManager
import com.example.accessibility.LocalPendingBlindAction
import com.example.ui.components.interceptBlindDoubleTap
class MainActivity : ComponentActivity() {

    private val viewModel: QuranViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordAudioGranted) {
            viewModel.announce("تنبيه: صلاحية الميكروفون مطلوبة لتفعيل الأوامر الصوتية.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            if (!notificationsGranted && recordAudioGranted) {
                viewModel.announce("تنبيه: صلاحية الإشعارات مطلوبة لتشغيل الصوت في الخلفية.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        setContent {
            QuranBlindTheme {
                val trialExpired by viewModel.isTrialExpired.collectAsState(initial = null)
                val isTalkBackEnabled by viewModel.speechManager.isTalkBackEnabledFlow.collectAsState()
                val scope = rememberCoroutineScope()
                val pendingActionManager = remember { PendingBlindActionManager(scope) }
                val context = LocalContext.current
                val doubleTapSlop = remember(context) {
                    ViewConfiguration.get(context).scaledDoubleTapSlop.toFloat()
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                                pendingActionManager.clear()
                                viewModel.pausePlayback()
                            }
                            Lifecycle.Event.ON_START -> {
                                viewModel.resumePlayback()
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                CompositionLocalProvider(
                    LocalPendingBlindAction provides pendingActionManager
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .interceptBlindDoubleTap(
                                isTalkBackEnabled = isTalkBackEnabled,
                                doubleTapSlop = doubleTapSlop,
                                onDoubleTap = {
                                    pendingActionManager.execute()
                                    pendingActionManager.clear()
                                },
                                onDragDetected = {
                                    pendingActionManager.clear()
                                }
                            )
                    ) {
                        when (trialExpired) {
                            null -> {
                                // Loading state while checking trial
                            }
                            true -> {
                                TrialExpiredScreen(
                                    isTalkBackEnabled = isTalkBackEnabled,
                                    onAnnounce = { msg -> viewModel.announce(msg) }
                                )
                            }
                            false -> {
                                QuranPlayerScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
