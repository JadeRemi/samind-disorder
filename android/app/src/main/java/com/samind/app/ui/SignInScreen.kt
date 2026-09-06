package com.samind.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.data.Prefs
import com.samind.app.ui.components.ControlHeight
import com.samind.app.ui.components.PillShape
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.SamindGradients

/** Nickname-only onboarding. No account, no password (design). */
@Composable
fun SignInScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(Prefs.displayName(context)) }

    SamindBackground(signIn = true) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = ScreenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Image(
                painterResource(R.drawable.ic_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth().height(40.dp),
            )
            Spacer(Modifier.weight(1f))

            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = Neutral900,
                    textAlign = TextAlign.Center,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ControlHeight)
                    .background(SamindGradients.controlActive, PillShape)
                    .padding(20.dp),
                decorationBox = { field ->
                    if (name.isEmpty()) {
                        Text(
                            stringResource(R.string.signin_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Neutral900.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    field()
                },
            )
            Spacer(Modifier.height(16.dp))
            // disabled until the field has content (design)
            PrimaryButton(
                text = stringResource(R.string.signin_action),
                enabled = name.isNotBlank(),
                onClick = {
                    Prefs.setDisplayName(context, name.trim())
                    onDone()
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
