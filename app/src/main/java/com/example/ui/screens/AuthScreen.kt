package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.AppStrings.tr
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OledBackground
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    VERIFY_EMAIL
}

@Composable
fun AuthScreen(
    mode: AuthMode,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onModeChange: (AuthMode) -> Unit,
    onLoginSubmit: (email: String, password: String) -> Unit,
    onRegisterSubmit: (email: String, password: String, displayName: String) -> Unit,
    onForgotPasswordSubmit: (email: String, newPassword: String) -> Unit,
    onVerifyEmailSubmit: (code: String) -> Unit,
    onContinueToHome: () -> Unit
) {
    val lang = LocalAppLanguage.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand emblem
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(colors = listOf(AccentGold, AccentOrange))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "MeloGroove",
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = when (mode) {
                    AuthMode.LOGIN -> tr(lang, "ورود به حساب ملوگروو", "Sign In to MeloGroove")
                    AuthMode.REGISTER -> tr(lang, "ساخت حساب کاربری جدید", "Create New Account")
                    AuthMode.FORGOT_PASSWORD -> tr(lang, "بازیابی و تغییر رمز عبور", "Reset Password")
                    AuthMode.VERIFY_EMAIL -> tr(lang, "تأیید ایمیل کاربری", "Verify Your Email")
                },
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tr(
                    lang,
                    "پشتیبان‌گیری از پلی‌لیست‌ها و علاقه‌مندی‌ها (سازگار با اینترنت ایران بدون نیاز به VPN)",
                    "Sync your playlists and favorites (Direct No-VPN compatible)"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BorderHighlight, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeError = localValidationError ?: errorMessage
                    if (!activeError.isNullOrBlank()) {
                        Surface(
                            color = Color(0x33FF5722),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeError,
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentOrange,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (!infoMessage.isNullOrBlank()) {
                        Surface(
                            color = Color(0x3300E676),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = infoMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (mode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {
                                displayName = it
                                localValidationError = null
                            },
                            label = { Text(tr(lang, "نام نمایشی (اختیاری)", "Display Name (Optional)")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = tr(lang, "نام نمایشی", "Display Name"),
                                    tint = AccentGold
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = authTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_display_name_input")
                        )
                    }

                    if (mode != AuthMode.VERIFY_EMAIL) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localValidationError = null
                            },
                            label = { Text(tr(lang, "آدرس ایمیل", "Email Address")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = tr(lang, "ایمیل", "Email"),
                                    tint = AccentGold
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = authTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )
                    }

                    if (mode == AuthMode.LOGIN || mode == AuthMode.REGISTER || mode == AuthMode.FORGOT_PASSWORD) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                localValidationError = null
                            },
                            label = {
                                Text(
                                    if (mode == AuthMode.FORGOT_PASSWORD) {
                                        tr(lang, "رمز عبور جدید (حداقل ۸ کاراکتر)", "New Password (min 8 chars)")
                                    } else {
                                        tr(lang, "رمز عبور (حداقل ۸ کاراکتر)", "Password (min 8 chars)")
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = tr(lang, "رمز عبور", "Password"),
                                    tint = AccentGold
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) {
                                            tr(lang, "مخفی کردن رمز", "Hide Password")
                                        } else {
                                            tr(lang, "نمایش رمز", "Show Password")
                                        },
                                        tint = TextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (mode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
                            ),
                            colors = authTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )
                    }

                    if (mode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                localValidationError = null
                            },
                            label = { Text(tr(lang, "تکرار رمز عبور", "Confirm Password")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = tr(lang, "تکرار رمز عبور", "Confirm Password"),
                                    tint = AccentGold
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            colors = authTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_confirm_password_input")
                        )
                    }

                    if (mode == AuthMode.VERIFY_EMAIL) {
                        OutlinedTextField(
                            value = verifyCode,
                            onValueChange = {
                                verifyCode = it
                                localValidationError = null
                            },
                            label = { Text(tr(lang, "کد تأیید ایمیل", "Verification Code")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            colors = authTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_verify_code_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            localValidationError = null
                            when (mode) {
                                AuthMode.LOGIN -> onLoginSubmit(email, password)
                                AuthMode.REGISTER -> {
                                    if (password != confirmPassword) {
                                        localValidationError = tr(
                                            lang,
                                            "رمز عبور و تکرار آن یکسان نیستند",
                                            "Passwords do not match"
                                        )
                                    } else {
                                        onRegisterSubmit(email, password, displayName)
                                    }
                                }
                                AuthMode.FORGOT_PASSWORD -> onForgotPasswordSubmit(email, password)
                                AuthMode.VERIFY_EMAIL -> onVerifyEmailSubmit(verifyCode)
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when (mode) {
                                    AuthMode.LOGIN -> tr(lang, "ورود به حساب", "Sign In")
                                    AuthMode.REGISTER -> tr(lang, "ثبت‌نام و ورود", "Create Account")
                                    AuthMode.FORGOT_PASSWORD -> tr(lang, "بازنشانی رمز عبور", "Reset Password")
                                    AuthMode.VERIFY_EMAIL -> tr(lang, "تأیید ایمیل", "Verify Email")
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Mode switcher links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mode == AuthMode.LOGIN) {
                            TextButton(
                                onClick = {
                                    localValidationError = null
                                    onModeChange(AuthMode.REGISTER)
                                },
                                modifier = Modifier.testTag("auth_switch_register_btn")
                            ) {
                                Text(tr(lang, "ثبت‌نام حساب جدید", "Create Account"), color = AccentGold)
                            }
                            TextButton(
                                onClick = {
                                    localValidationError = null
                                    onModeChange(AuthMode.FORGOT_PASSWORD)
                                },
                                modifier = Modifier.testTag("auth_switch_forgot_btn")
                            ) {
                                Text(tr(lang, "فراموشی رمز عبور؟", "Forgot Password?"), color = TextSecondary)
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    localValidationError = null
                                    onModeChange(AuthMode.LOGIN)
                                },
                                modifier = Modifier.testTag("auth_switch_login_btn")
                            ) {
                                Text(tr(lang, "بازگشت به صفحه ورود", "Back to Sign In"), color = AccentGold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onContinueToHome,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_continue_home_btn")
            ) {
                Text(
                    text = tr(lang, "بازگشت به محیط برنامه", "Back to Player"),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = AccentGold,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentGold,
    focusedContainerColor = OledSurface,
    unfocusedContainerColor = OledSurface
)
