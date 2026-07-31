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
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.components.blindAccessibleClickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.DarkImmersiveBg
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.TextPrimaryWhite

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
        "أسماء القراء" to "مثال: الحصري، المنشاوي، العفاسي، عبد الباسط",
        "قائمة السور" to "لفتح فهرس المائة وأربعة عشر سورة"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_command_guide_sheet"),
        color = DarkImmersiveBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دليل الأوامر الصوتية",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AccessibleGold,
                    modifier = Modifier.semantics { contentDescription = "دليل الأوامر الصوتية باللغة العربية" }
                )

                BlindAccessibleIconButton(
                    onClick = onDismiss,
                    onClickLabel = "إغلاق دليل الأوامر الصوتية",
                    onSingleTap = { onAnnounce("إغلاق دليل المساعدة") },
                    modifier = Modifier
                        .height(48.dp)
                        .width(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = AccessibleGold
                    )
                }
            }

            BlindAccessibleButton(
                onClick = onSpeakGuide,
                onClickLabel = "زر قراءة دليل الأوامر الصوتية بصوت ناطق",
                onSingleTap = { onAnnounce("قراءة الدليل صوتياً") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
                    .testTag("speak_guide_button"),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccessibleGold)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = DarkImmersiveBg)
                Spacer(modifier = Modifier.width(8.dp))
                Text("قراءة الدليل صوتياً", color = DarkImmersiveBg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                onClickLabel = "دليل الأمر: $cmd",
                                onClick = { /* No action needed */ },
                                onSingleTap = { onAnnounce("الأمر: $cmd. $example") }
                            )
                            .semantics { contentDescription = "الأمر: $cmd. $example" },
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = DarkImmersiveCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = cmd,
                                style = MaterialTheme.typography.headlineMedium,
                                color = AccessibleGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = example,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimaryWhite
                            )
                        }
                    }
                }
            }
        }
    }
}
