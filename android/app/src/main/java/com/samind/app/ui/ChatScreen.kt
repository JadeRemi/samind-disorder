package com.samind.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.chat.ChatEngine
import com.samind.app.chat.ChatMessage
import com.samind.app.data.Prefs
import com.samind.app.ui.components.CircleIconButton
import com.samind.app.ui.components.EmptyState
import com.samind.app.ui.components.TypingIndicator
import com.samind.app.ui.components.PillShape
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.SamindGradients

private const val SEED_USER = "I'm scared I won't cope"

private val SUGGESTIONS = listOf(
    R.string.chat_chip_1, R.string.chat_chip_2, R.string.chat_chip_3,
    R.string.chat_chip_4, R.string.chat_chip_5, R.string.chat_chip_6,
)

@Composable
fun ChatScreen(designState: String? = null) {
    val context = LocalContext.current
    val engine = remember(context) { ChatEngine(context) }
    // debug states let every chat frame in the design be photographed
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            when (designState) {
                DesignState.MESSAGE -> add(ChatMessage(true, SEED_USER))
                DesignState.REPLY, DesignState.LOADING -> {
                    add(ChatMessage(true, SEED_USER))
                    if (designState == DesignState.REPLY) {
                        add(ChatMessage(false, engine.reply(SEED_USER)))
                    }
                }
                DesignState.SCROLLED -> repeat(6) { index ->
                    add(ChatMessage(index % 2 == 0, SEED_USER))
                    add(ChatMessage(false, engine.reply(SEED_USER)))
                }
            }
        }
    }
    val loading = designState == DesignState.LOADING
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val name = Prefs.displayName(context)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    SamindBackground {
        Column(Modifier.fillMaxSize().imePadding()) {
            SamindTopBar(
                stringResource(R.string.tab_chat),
                onBack = {},
                actionIcon = R.drawable.ic_waveform,
                onAction = {},
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty()) {
                    EmptyState(
                        iconRes = R.drawable.ic_tab_chat,
                        title = if (name.isBlank()) stringResource(R.string.chat_greeting)
                        else stringResource(R.string.chat_greeting_named, name),
                        body = stringResource(R.string.chat_suggestions_title),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = ScreenMargin)
                            // soft alpha fade at both edges (design annotation)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val fade = 28.dp.toPx()
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        (fade / size.height) to Color.Black,
                                        1f - (fade / size.height) to Color.Black,
                                        1f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(messages) { message -> MessageRow(message) }
                        if (loading) item { TypingIndicator() }
                    }
                }
            }

            // chips: horizontal scroll, never wrap; tapping inserts the text
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = ScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SUGGESTIONS.forEach { res ->
                    val text = stringResource(res)
                    Box(
                        Modifier
                            .height(44.dp)
                            .background(SamindGradients.controlActive, PillShape)
                            .clickable { input = text }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text, style = MaterialTheme.typography.titleMedium, color = Neutral900)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Composer(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        messages.add(ChatMessage(true, text))
                        messages.add(ChatMessage(false, engine.reply(text)))
                        input = ""
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Bot messages have no background at all — a hard design constraint. */
@Composable
private fun MessageRow(message: ChatMessage) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (message.fromUser) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Neutral900,
                modifier = Modifier
                    .background(SamindGradients.controlActive, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        } else {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Neutral900,
                modifier = Modifier.padding(end = 24.dp),
            )
        }
    }
}

@Composable
private fun Composer(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .height(68.dp)
            .background(SamindGradients.controlActive, PillShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(start = 12.dp)) {
            if (value.isEmpty()) {
                Text(
                    stringResource(R.string.chat_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Neutral900.copy(alpha = 0.45f),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Neutral900,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CircleIconButton(R.drawable.ic_mic, null, {})
        Spacer(Modifier.size(8.dp))
        // send is enabled only when there is text (design)
        Box(Modifier.alphaIf(value.isNotBlank())) {
            CircleIconButton(R.drawable.ic_send, stringResource(R.string.chat_send), onSend)
        }
    }
}

private fun Modifier.alphaIf(enabled: Boolean) =
    this.graphicsLayer { alpha = if (enabled) 1f else 0.4f }
