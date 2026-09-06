package com.samind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.ui.components.PracticeCard
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin

@Composable
fun PracticesScreen(
    onOpenGrounding: () -> Unit,
    onOpenBreathing: () -> Unit,
    onOpenEights: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            // the design's Practices header has no buttons — title only
            SamindTopBar(stringResource(R.string.tab_practices))
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenMargin, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PracticeCard(
                    title = stringResource(R.string.practice_grounding),
                    supporting = stringResource(R.string.practice_grounding_sub),
                    onStart = onOpenGrounding,
                )
                // "Breathing practices" is a category: it opens the practice
                // whose technique is currently selected in settings
                PracticeCard(
                    title = stringResource(R.string.practice_breathing_group),
                    supporting = stringResource(R.string.practice_breathing_sub),
                    onStart = onOpenBreathing,
                )
                Spacer(Modifier.height(110.dp))
            }
        }
    }
}
