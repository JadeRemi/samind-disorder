package com.samind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samind.app.R
import com.samind.app.SamindApp
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun StatsScreen() {
    val dao = remember { SamindApp.instance.database.triggerEvents() }
    val weekAgo = remember { System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7) }

    val total by dao.totalCount().collectAsStateWithLifecycle(0)
    val week by dao.countSince(weekAgo).collectAsStateWithLifecycle(0)
    val recent by dao.recent().collectAsStateWithLifecycle(emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(value = total, label = stringResource(R.string.stats_intercepted), modifier = Modifier.weight(1f))
            StatCard(value = week, label = stringResource(R.string.stats_this_week), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        if (recent.isEmpty()) {
            Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recent) { event ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(event.sourcePackage, style = MaterialTheme.typography.titleSmall)
                            Text(
                                DateFormat.getDateTimeInstance().format(Date(event.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text("$value", style = MaterialTheme.typography.displaySmall)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
