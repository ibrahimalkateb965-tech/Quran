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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.announceForAccessibility
import com.example.ui.theme.AccessibleGold
import com.example.ui.theme.DarkImmersiveBg
import com.example.ui.theme.DarkImmersiveCard
import com.example.ui.theme.TextPrimaryWhite

@Composable
fun TrialExpiredScreen(
    isTalkBackEnabled: Boolean = false,
    onAnnounce: (String) -> Unit = {}
) {
    val announceMessage = "تنبيه: انتهت الفترة التجريبية للتطبيق المحددة بـ 7 أيام. يرجى التواصل مع المطور للحصول على النسخة الكاملة."
    val context = LocalContext.current

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
        color = DarkImmersiveBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkImmersiveCard),
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
                        tint = AccessibleGold,
                        modifier = Modifier.height(72.dp).fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "انتهت الفترة التجريبية",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AccessibleGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "عزيزي المستخدم، لقد انتهت الفترة التجريبية المحددة بـ 7 أيام لاستخدام التطبيق.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimaryWhite,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "يرجى التواصل مع المطور لتفعيل وتثبيت النسخة الدائمة والمكتملة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccessibleGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
