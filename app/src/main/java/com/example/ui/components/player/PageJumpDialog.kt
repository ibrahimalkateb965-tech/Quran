package com.example.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.BlindAccessibleButton
import com.example.ui.theme.WarmAccentTerracotta
import com.example.ui.theme.WarmEarthBg
import com.example.ui.theme.WarmCardBorder
import com.example.ui.theme.WarmCardLight
import com.example.ui.theme.WarmTextLight
import com.example.ui.theme.WarmTextPrimary
import com.example.ui.theme.WarmTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageJumpDialog(
    currentPage: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    onAnnounce: (String) -> Unit
) {
    var pageInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        onAnnounce("نافذة الانتقال برقم الصفحة. أدخل رقم الصفحة من 1 إلى 604")
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val validateAndConfirm: () -> Unit = {
        val normalized = normalizeArabicDigits(pageInput).trim()
        val num = normalized.toIntOrNull()
        if (num == null || num !in 1..604) {
            errorMessage = "رقم الصفحة يجب أن يكون بين 1 و 604"
            onAnnounce("رقم الصفحة غير صحيح. يجب أن يكون بين 1 و 604")
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
            onConfirm(num)
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("page_jump_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = WarmEarthBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WarmCardBorder),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = WarmAccentTerracotta,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "الانتقال إلى صفحة في المصحف",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = WarmAccentTerracotta,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "الصفحة الحالية $currentPage. يمكنك إدخال أي رقم صفحة من 1 إلى 604 للانتقال المباشر إليها.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input Field
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { input ->
                        val normalized = normalizeArabicDigits(input)
                        pageInput = normalized
                        if (errorMessage != null) {
                            errorMessage = null
                        }
                    },
                    placeholder = {
                        Text(
                            text = "رقم الصفحة (1 - 604)",
                            color = WarmTextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = WarmAccentTerracotta
                        )
                    },
                    trailingIcon = {
                        if (pageInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    pageInput = ""
                                    errorMessage = null
                                    onAnnounce("تم مسح رقم الصفحة")
                                },
                                modifier = Modifier.semantics {
                                    this.contentDescription = "مسح رقم الصفحة المدخل"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = WarmTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { validateAndConfirm() }
                    ),
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmAccentTerracotta,
                        unfocusedBorderColor = WarmCardBorder,
                        focusedContainerColor = WarmCardLight,
                        unfocusedContainerColor = WarmCardLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("page_jump_input_field")
                        .semantics {
                            this.contentDescription = "حقل إدخال رقم الصفحة المطلوب الانتقال إليها"
                        }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("page_jump_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    BlindAccessibleButton(
                        onClick = onDismiss,
                        onClickLabel = "إلغاء وإغلاق نافذة الانتقال برقم الصفحة",
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .border(1.dp, WarmCardBorder, RoundedCornerShape(16.dp))
                            .testTag("page_jump_cancel_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmCardLight,
                            contentColor = WarmTextSecondary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            color = WarmTextSecondary
                        )
                    }

                    // Confirm Button
                    BlindAccessibleButton(
                        onClick = { validateAndConfirm() },
                        onClickLabel = "تأكيد الانتقال إلى الصفحة المحددة",
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("page_jump_confirm_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmAccentTerracotta,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "انتقال",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun normalizeArabicDigits(input: String): String {
    return input.map { ch ->
        when (ch) {
            in '٠'..'٩' -> ('0' + (ch - '٠'))
            in '۰'..'۹' -> ('0' + (ch - '۰'))
            else -> ch
        }
    }.filter { it in '0'..'9' }.joinToString("")
}
