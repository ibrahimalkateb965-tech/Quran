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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.accessibility.LocalTalkBackEnabled
import com.example.ui.screens.QuranPlayerScreen
import com.example.ui.theme.QuranBlindTheme
import com.example.ui.viewmodel.QuranViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: QuranViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // The notification warning used to be gated on the microphone permission having been
        // granted. With voice commands gone that coupling is meaningless — background playback
        // matters to every user, so this now warns unconditionally.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            if (!notificationsGranted) {
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
                val isTalkBackEnabled by viewModel.speechManager.isTalkBackEnabledFlow.collectAsState()

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                                if (!viewModel.settingsUiState.value.isContinuousPlayEnabled) {
                                    viewModel.pausePlayback()
                                }
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
                    LocalTalkBackEnabled provides isTalkBackEnabled
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        QuranPlayerScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

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
