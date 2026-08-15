package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.accessibility.announceForAccessibility
import com.example.security.TrialManager
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmEarthBg
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

@Composable
fun TrialExpiredScreen(
    isTalkBackEnabled: Boolean = false,
    onAnnounce: (String) -> Unit = {},
    onUnlockSuccess: () -> Unit = {}
) {
    val announceMessage = "تنبيه: انتهت الفترة التجريبية للتطبيق المحددة بـ 30 يوماً. يرجى التواصل مع المطور للحصول على النسخة الكاملة."
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isTalkBackEnabled) {
            announceForAccessibility(context, announceMessage)
        } else {
            onAnnounce(announceMessage)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = announceMessage
            },
        color = WarmEarthBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WarmCardLight),
                border = BorderStroke(1.dp, WarmCardBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "أيقونة التشفير والإغلاق",
                        tint = WarmAccentTerracotta,
                        modifier = Modifier
                            .height(72.dp)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        showPinDialog = true
                                    }
                                )
                            }
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("إدخال رمز التمديد") {
                                        showPinDialog = true
                                        true
                                    }
                                )
                            }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "انتهت الفترة التجريبية",
                        style = MaterialTheme.typography.headlineLarge,
                        color = WarmAccentTerracotta,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "عزيزي المستخدم، لقد انتهت الفترة التجريبية المحددة بـ 30 يوماً لاستخدام التطبيق.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WarmTextPrimary,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "يرجى التواصل مع المطور لتفعيل وتثبيت النسخة الدائمة والمكتملة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinDialog = false
                    pinText = ""
                    pinError = false
                },
                title = {
                    Text(text = "رمز التمديد", color = WarmTextLight)
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pinText,
                            onValueChange = {
                                pinText = it
                                pinError = false
                            },
                            label = { Text("أدخل الرمز السري") },
                            singleLine = true,
                            isError = pinError
                        )
                        if (pinError) {
                            Text(
                                text = "الرمز غير صحيح أو تم استخدامه مسبقاً",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val manager = TrialManager.getInstance(context)
                        val success = manager.submitUnlockPin(pinText.trim())
                        if (success) {
                            showPinDialog = false
                            pinText = ""
                            onUnlockSuccess()
                        } else {
                            pinError = true
                        }
                    }) {
                        Text("تأكيد")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPinDialog = false
                        pinText = ""
                        pinError = false
                    }) {
                        Text("إلغاء")
                    }
                },
                containerColor = WarmCardActive
            )
        }
    }
}
