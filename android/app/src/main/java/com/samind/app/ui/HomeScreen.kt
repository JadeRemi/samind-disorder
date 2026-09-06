package com.samind.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.data.Prefs
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.components.SwipeToggle
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary900

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(Prefs.monitoringEnabled(context)) }

    SamindBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = ScreenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(150.dp))

            Image(
                painterResource(R.drawable.ic_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Neutral900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { /* how-it-works sheet */ },
            ) {
                Icon(
                    painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = Primary900,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.how_it_works),
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary900,
                    textDecoration = TextDecoration.Underline,
                )
            }

            Spacer(Modifier.weight(1f))

            SwipeToggle(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    Prefs.setMonitoringEnabled(context, it)
                },
                offLabel = stringResource(R.string.enable_monitoring),
                onLabel = stringResource(R.string.monitoring_on),
            )
            // clearance for the floating nav pill (68 dp + 16 dp margins):
            // the hint used to render behind it
            Spacer(Modifier.height(116.dp))
        }
    }
}
