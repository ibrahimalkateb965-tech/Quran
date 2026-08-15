package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import com.example.ui.components.BlindAccessibleButton
import com.example.ui.components.blindAccessibleClickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmCardActive
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

@Composable
fun VoiceCommandGuideSheet(
    onSpeakGuide: () -> Unit,
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    val commands = listOf(
        "تشغيل سورة [اسم السورة]" to "مثال: تشغيل سورة الكهف أو سورة البقرة",
        "الآية [رقم الآية]" to "مثال: الآية 5 أو الانتقال للآية عشرة",
        "توقف / إيقاف" to "لإيقاف التلاوة مؤقتاً",
        "تشغيل / استئناف" to "لمواصلة الاستماع للتلاوة",
        "التالي / السابق" to "للتنقل بين الآيات آية آية",
        "تكرار / حفظ / تركيز" to "لتفعيل تكرار الآية (1، 3، 5، 10 مرات) للحفظ",
        "إشارة مرجعية" to "لحفظ الآية الحالية في المفضلة",
        "أسماء القراء" to "مثال: الحصري، المنشاوي، عبد الرشيد صوفي، عبد الباسط",
        "قائمة السور" to "لفتح فهرس المائة وأربعة عشر سورة"
    )

    AccessibleBottomSheet(
        title = "دليل الأوامر الصوتية",
        contentDescriptionText = "دليل الأوامر الصوتية باللغة العربية",
        onDismiss = onDismiss,
        onAnnounce = onAnnounce,
        showCloseButton = false
    ) {

            BlindAccessibleButton(
                onClick = onSpeakGuide,
                onClickLabel = "زر قراءة دليل الأوامر الصوتية بصوت ناطق",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
                    .testTag("speak_guide_button"),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = WarmCardActive)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = WarmTextLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("قراءة الدليل صوتياً", color = WarmTextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                commands.forEach { (cmd, example) ->
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .blindAccessibleClickable(
                                onClickLabel = "قراءة الدليل صوتياً",
                                onClick = { onAnnounce("هذا التطبيق يدعم الأوامر الصوتية...") }
                            )
                            .semantics { contentDescription = "الأمر: $cmd. $example" },
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = WarmCardLight),
                        border = BorderStroke(1.dp, WarmCardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = cmd,
                                style = MaterialTheme.typography.headlineMedium,
                                color = WarmTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = example,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
