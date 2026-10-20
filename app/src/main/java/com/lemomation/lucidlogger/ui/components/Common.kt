package com.lemomation.lucidlogger.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemomation.lucidlogger.audio.AudioPlayerController
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidBorderStrong
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidOnAmber
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidPrimarySoft
import com.lemomation.lucidlogger.ui.theme.LucidSurface
import com.lemomation.lucidlogger.ui.theme.LucidSurface2
import com.lemomation.lucidlogger.ui.theme.LucidSurface3
import com.lemomation.lucidlogger.ui.theme.LucidSurface4
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import com.lemomation.lucidlogger.util.TextUtils
import kotlinx.coroutines.flow.collect
import java.io.File
import kotlin.math.max

@Composable
fun LucidTopBar(
    title: String = "Lucid Logger",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(LucidCanvas)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = LucidText)
                }
            } else {
                Icon(
                    Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = LucidPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = LucidText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(LucidBorder))
    }
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector,
)

val bottomDestinations = listOf(
    BottomDestination("alarm", "Alarm", Icons.Outlined.Alarm, Icons.Filled.Alarm),
    BottomDestination("journal", "Journal", Icons.Outlined.AutoStories, Icons.Filled.AutoStories),
    BottomDestination("graph", "Graph", Icons.Outlined.Hub, Icons.Filled.Hub),
    BottomDestination("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    BottomDestination("settings", "Settings", Icons.Outlined.Tune, Icons.Filled.Tune),
)

@Composable
fun LucidBottomBar(selectedRoute: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(LucidCanvas)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(LucidBorder))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomDestinations.forEach { destination ->
                val active = destination.route == selectedRoute
                val color by animateColorAsState(
                    targetValue = if (active) LucidAmber else LucidTextVariant,
                    animationSpec = tween(200),
                    label = "navColor",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(destination.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (active) destination.activeIcon else destination.icon,
                        contentDescription = destination.label,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = destination.label,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun LucidCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(LucidSurface)
            .border(BorderStroke(1.dp, LucidBorder), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier,
            )
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionHeader(label: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = LucidAmber, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LucidAmber,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
fun LucidSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val offset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = tween(180),
        label = "switchOffset",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) LucidAmber.copy(alpha = 0.22f) else LucidSurface4,
        animationSpec = tween(180),
        label = "switchTrack",
    )
    Box(
        modifier
            .widthIn(min = 52.dp)
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(trackColor)
                .border(1.dp, if (checked) LucidAmber.copy(alpha = 0.5f) else LucidBorderStrong, CircleShape),
        ) {
            Box(
                Modifier
                    .offset(x = offset, y = 2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) LucidAmber else LucidTextSubtle),
            )
        }
    }
}

@Composable
fun TagChip(text: String, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(LucidSurface3)
            .border(1.dp, LucidBorder, CircleShape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (text.startsWith("#")) text else "#$text",
            style = MaterialTheme.typography.labelSmall,
            color = LucidTextVariant,
        )
    }
}

@Composable
fun StatusPill(text: String, color: Color = LucidAmber, background: Color = color.copy(alpha = 0.12f)) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, color.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun Waveform(
    values: List<Int>,
    progress: Float,
    modifier: Modifier = Modifier,
    playedColor: Color = LucidAmber,
    restColor: Color = LucidTextSubtle,
    onSeek: ((Float) -> Unit)? = null,
) {
    val bars = values.ifEmpty { List(32) { 18 + (it * 13) % 45 } }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .then(
                if (onSeek != null) {
                    Modifier.pointerInput(bars) {
                        detectTapGestures { offset ->
                            onSeek((offset.x / size.width).coerceIn(0f, 1f))
                        }
                    }
                } else Modifier,
            ),
    ) {
        val count = bars.size
        val gap = size.width / (count * 2.2f)
        val barWidth = (size.width - gap * (count - 1)) / count
        bars.forEachIndexed { index, raw ->
            val normalized = max(0.12f, (raw.coerceIn(0, 100)) / 100f)
            val height = size.height * normalized
            val x = index * (barWidth + gap)
            val played = count > 0 && (index + 0.5f) / count <= progress
            drawRoundRect(
                color = if (played) playedColor else restColor,
                topLeft = Offset(x, (size.height - height) / 2f),
                size = Size(barWidth.coerceAtLeast(1.2f), height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
fun AudioNotePlayer(
    note: DreamNote,
    file: File?,
    waveform: List<Int>,
    controller: AudioPlayerController,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()
    val isCurrent = file != null && state.filePath == file.absolutePath
    val progress = if (isCurrent && state.durationMs > 0) {
        state.positionMs.toFloat() / state.durationMs
    } else 0f
    val duration = if (isCurrent && state.durationMs > 0) {
        state.durationMs / 1000
    } else note.audioDuration ?: 0

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LucidCanvas.copy(alpha = 0.55f))
            .border(1.dp, LucidBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LucidPrimary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { if (file != null) controller.toggle(file) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isCurrent && state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isCurrent && state.isPlaying) "Pause" else "Play",
                tint = LucidText,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Waveform(
                values = waveform,
                progress = progress,
                onSeek = if (isCurrent && state.durationMs > 0) { fraction ->
                    controller.seekTo((fraction * state.durationMs).toInt())
                } else null,
            )
            Text(
                text = "Voice memo  ·  ${TextUtils.formatDuration(duration)}",
                style = MaterialTheme.typography.labelSmall,
                color = LucidTextSubtle,
                modifier = Modifier.padding(start = 1.dp, top = 1.dp),
            )
        }
    }
}


@Composable
fun LucidInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = LucidText),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(LucidAmber),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(LucidSurface2)
                    .border(1.dp, LucidBorderStrong, shape)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = textStyle,
                            color = LucidTextSubtle,
                        )
                    }
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    accent: Boolean = false,
) {
    val container = if (accent) LucidAmber else LucidPrimary
    val content = if (accent) LucidOnAmber else LucidText
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = LucidSurface4,
            disabledContentColor = LucidTextSubtle,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LucidSurface2,
            contentColor = LucidTextVariant,
            disabledContainerColor = LucidSurface,
            disabledContentColor = LucidTextSubtle,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    accent: Color = LucidText,
) {
    LucidCard(modifier, padding = PaddingValues(12.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LucidTextSubtle,
            letterSpacing = 0.9.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
        )
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = LucidTextVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(LucidSurface2)
                .border(1.dp, LucidBorderStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = LucidPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = LucidText)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = LucidTextVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun LucidDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LucidBorder))
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = LucidText, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LucidTextSubtle,
                    lineHeight = 16.sp,
                )
            }
        }
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LucidTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = use24Hour,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LucidSurface2,
        title = {
            Text("Set alarm time", color = LucidText, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    containerColor = LucidSurface2,
                    clockDialColor = LucidSurface3,
                    clockDialSelectedContentColor = LucidOnAmber,
                    clockDialUnselectedContentColor = LucidText,
                    selectorColor = LucidAmber,
                    timeSelectorSelectedContainerColor = LucidAmber.copy(alpha = 0.18f),
                    timeSelectorUnselectedContainerColor = LucidSurface3,
                    timeSelectorSelectedContentColor = LucidAmber,
                    timeSelectorUnselectedContentColor = LucidText,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("Set", color = LucidAmber, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = LucidTextVariant) }
        },
    )
}

@Composable
fun ValueStepper(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LucidSurface3)
            .border(1.dp, LucidBorderStrong, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
            Text("−", color = LucidTextVariant, fontSize = 18.sp)
        }
        Text(
            text = "$value$suffix",
            color = LucidAmber,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 38.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
            Text("+", color = LucidTextVariant, fontSize = 18.sp)
        }
    }
}

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = LucidAmber,
    track: Color = LucidSurface4,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(150), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(color),
        )
    }
}
