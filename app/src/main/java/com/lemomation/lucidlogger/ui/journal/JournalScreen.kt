package com.lemomation.lucidlogger.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.data.model.DreamSource
import com.lemomation.lucidlogger.ui.components.EmptyState
import com.lemomation.lucidlogger.ui.components.LucidCard
import com.lemomation.lucidlogger.ui.components.LucidInput
import com.lemomation.lucidlogger.ui.components.LucidTopBar
import com.lemomation.lucidlogger.ui.components.MetricCard
import com.lemomation.lucidlogger.ui.components.SectionHeader
import com.lemomation.lucidlogger.ui.components.StatusPill
import com.lemomation.lucidlogger.ui.components.TagChip
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidOnAmber
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidSurface3
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import com.lemomation.lucidlogger.util.TextUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class JournalFilter(val label: String) {
    ALL("All"),
    LUCID("Lucid"),
    VOICE("Voice"),
    WRITTEN("Written"),
}

@Composable
fun JournalScreen(
    notes: List<DreamNote>,
    settings: AppSettings,
    onOpenNote: (DreamNote) -> Unit,
    onNewDream: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(JournalFilter.ALL.name) }
    val filter = JournalFilter.valueOf(filterName)

    val filtered = remember(notes, query, filter) {
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.body.contains(query, ignoreCase = true) ||
                note.tags.any { it.contains(query, ignoreCase = true) } ||
                note.links.any { it.contains(query, ignoreCase = true) }
            val matchesFilter = when (filter) {
                JournalFilter.ALL -> true
                JournalFilter.LUCID -> note.lucid
                JournalFilter.VOICE -> note.hasAudio
                JournalFilter.WRITTEN -> !note.hasAudio
            }
            matchesQuery && matchesFilter
        }
    }

    val total = notes.size
    val lucidCount = notes.count { it.lucid }
    val lucidPercent = if (total == 0) 0 else (lucidCount * 100) / total
    val streak = remember(notes) { dreamStreak(notes) }

    Box(Modifier.fillMaxSize().background(LucidCanvas)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                LucidTopBar(
                    title = "Dream Journal",
                    actions = {
                        IconButton(onClick = { onNewDream(true) }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Quick voice memo", tint = LucidAmber)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Tune, contentDescription = "Settings", tint = LucidTextVariant)
                        }
                    },
                )
            }

            item {
                LucidInput(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search dreams, tags, dream signs...",
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    JournalFilter.entries.forEach { item ->
                        FilterPill(
                            label = item.label,
                            selected = item == filter,
                            onClick = { filterName = item.name },
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricCard("Dreams", total.toString(), "captured", Modifier.weight(1f))
                    MetricCard("Lucid", "$lucidPercent%", "awareness", Modifier.weight(1f), accent = LucidAmber)
                    MetricCard("Streak", "$streak", if (streak == 1) "night" else "nights", Modifier.weight(1f), accent = LucidPrimary)
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("Recent dreams", Icons.Outlined.AutoStories)
                    Text(
                        "${filtered.size} ${if (filtered.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LucidTextSubtle,
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.WbTwilight,
                        title = if (notes.isEmpty()) "The night is still unwritten" else "No dreams match",
                        message = if (notes.isEmpty()) {
                            "Your alarm will bring you back here. Trace the dream before it dissolves."
                        } else {
                            "Try another search or filter to find a fragment."
                        },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { note ->
                    DreamCard(
                        note = note,
                        use24Hour = settings.use24Hour,
                        onClick = { onOpenNote(note) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { onNewDream(false) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LucidAmber,
            contentColor = LucidOnAmber,
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New dream")
        }
    }
}

@Composable
private fun DreamCard(note: DreamNote, use24Hour: Boolean, onClick: () -> Unit) {
    LucidCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = note.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = LucidTextSubtle,
                letterSpacing = 0.9.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = note.displayTime(use24Hour),
                style = MaterialTheme.typography.labelSmall,
                color = LucidTextVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = note.title,
            style = MaterialTheme.typography.titleLarge,
            color = LucidText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = TextUtils.plainExcerpt(note.body).ifBlank { "Voice only. Add the details in the morning." },
            style = MaterialTheme.typography.bodySmall,
            color = LucidTextVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (note.tags.isNotEmpty() || note.hasAudio || note.lucid) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (note.lucid) StatusPill("Lucid", LucidAmber)
                if (note.hasAudio) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(LucidSurface3)
                            .border(1.dp, LucidBorder, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = LucidPrimary, modifier = Modifier.size(12.dp))
                        Text(
                            TextUtils.formatDuration(note.audioDuration ?: 0),
                            style = MaterialTheme.typography.labelSmall,
                            color = LucidTextVariant,
                        )
                    }
                }
                note.tags.take(4).forEach { TagChip(it) }
            }
        }
        if (note.source == DreamSource.VOICE && !TextUtils.hasWrittenText(note.body)) {
            Spacer(Modifier.height(9.dp))
            StatusPill("Morning note waiting", LucidPrimary)
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) LucidAmber.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, if (selected) LucidAmber.copy(alpha = 0.5f) else LucidBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) LucidAmber else LucidTextVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun dreamStreak(notes: List<DreamNote>): Int {
    val days = notes.map { it.date }.toSet()
    var cursor = LocalDate.now()
    if (cursor !in days) cursor = cursor.minusDays(1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
