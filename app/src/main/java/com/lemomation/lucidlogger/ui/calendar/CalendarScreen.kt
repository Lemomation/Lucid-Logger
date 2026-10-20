package com.lemomation.lucidlogger.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.lemomation.lucidlogger.ui.components.LucidCard
import com.lemomation.lucidlogger.ui.components.LucidTopBar
import com.lemomation.lucidlogger.ui.components.MetricCard
import com.lemomation.lucidlogger.ui.components.SectionHeader
import com.lemomation.lucidlogger.ui.components.StatusPill
import com.lemomation.lucidlogger.ui.theme.HeatLevel0
import com.lemomation.lucidlogger.ui.theme.HeatLevel1
import com.lemomation.lucidlogger.ui.theme.HeatLevel2
import com.lemomation.lucidlogger.ui.theme.HeatLevel3
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidSurface
import com.lemomation.lucidlogger.ui.theme.LucidSurface3
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    notes: List<DreamNote>,
    settings: AppSettings,
    onOpenNote: (DreamNote) -> Unit,
) {
    val today = LocalDate.now()
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var month by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var selectedDate by rememberSaveable { mutableStateOf(today.toString()) }
    val yearMonth = remember(year, month) { YearMonth.of(year, month) }

    val monthNotes = remember(notes, yearMonth) {
        notes.filter { YearMonth.from(it.date) == yearMonth }
    }
    val byDay = remember(monthNotes) { monthNotes.groupBy { it.date } }
    val selectedDay = runCatching { LocalDate.parse(selectedDate) }.getOrNull()
    val selectedNotes = selectedDay?.let { byDay[it] }.orEmpty()

    LaunchedEffect(yearMonth) {
        val target = byDay.keys.maxOrNull()
            ?: if (YearMonth.from(today) == yearMonth) today else yearMonth.atDay(1)
        selectedDate = target.toString()
    }

    val nights = byDay.keys.size
    val lucid = monthNotes.count { it.lucid }

    Column(Modifier.fillMaxSize().background(LucidCanvas)) {
        LucidTopBar(title = "Dream Calendar")

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    yearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.US)),
                    style = MaterialTheme.typography.headlineLarge,
                    color = LucidText,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidTextSubtle,
                    letterSpacing = 1.sp,
                )
            }
            if (yearMonth != YearMonth.from(today)) {
                TextButton(onClick = {
                    year = today.year
                    month = today.monthValue
                }) { Text("Today", color = LucidTextVariant) }
            }
            IconButton(onClick = {
                val previous = yearMonth.minusMonths(1)
                year = previous.year
                month = previous.monthValue
            }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month", tint = LucidTextVariant)
            }
            IconButton(onClick = {
                val next = yearMonth.plusMonths(1)
                year = next.year
                month = next.monthValue
            }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month", tint = LucidTextVariant)
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricCard("Nights", nights.toString(), "with dreams", Modifier.weight(1f))
                    MetricCard("Dreams", monthNotes.size.toString(), "this month", Modifier.weight(1f))
                    MetricCard("Lucid", lucid.toString(), "moments", Modifier.weight(1f), accent = LucidAmber)
                }
            }

            item {
                MonthGrid(
                    yearMonth = yearMonth,
                    byDay = byDay,
                    today = today,
                    selected = selectedDay,
                    weekStartsMonday = settings.weekStartsMonday,
                    onSelect = { selectedDate = it.toString() },
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = LucidTextSubtle)
                    listOf(HeatLevel0, HeatLevel1, HeatLevel2, HeatLevel3).forEach { color ->
                        Box(
                            Modifier
                                .size(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                                .border(1.dp, LucidBorder, RoundedCornerShape(3.dp)),
                        )
                    }
                    Text("More", style = MaterialTheme.typography.labelSmall, color = LucidTextSubtle)
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(
                        selectedDay?.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))
                            ?: "Selected night",
                        Icons.Outlined.CalendarMonth,
                        Modifier.weight(1f),
                    )
                    Text(
                        if (selectedNotes.isEmpty()) "1 dream per square" else "${selectedNotes.size} captured",
                        style = MaterialTheme.typography.labelSmall,
                        color = LucidTextSubtle,
                    )
                }
            }

            if (selectedNotes.isEmpty()) {
                item {
                    Text(
                        "No dreams recorded on this night. The square stays dark.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LucidTextVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(selectedNotes, key = { it.id }) { note ->
                    CalendarNoteRow(note, settings, onClick = { onOpenNote(note) })
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    byDay: Map<LocalDate, List<DreamNote>>,
    today: LocalDate,
    selected: LocalDate?,
    weekStartsMonday: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val first = yearMonth.atDay(1)
    val leading = if (weekStartsMonday) first.dayOfWeek.value - 1 else first.dayOfWeek.value % 7
    val total = yearMonth.lengthOfMonth()
    val cells = leading + total
    val rows = (cells + 6) / 7
    val weekLabels = if (weekStartsMonday) listOf("M", "T", "W", "T", "F", "S", "S")
    else listOf("S", "M", "T", "W", "T", "F", "S")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LucidSurface)
            .border(1.dp, LucidBorder, RoundedCornerShape(18.dp))
            .padding(10.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            weekLabels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidTextSubtle,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    val dayNumber = index - leading + 1
                    if (dayNumber in 1..total) {
                        val date = yearMonth.atDay(dayNumber)
                        val count = byDay[date]?.size ?: 0
                        val isToday = date == today
                        val isSelected = date == selected
                        DayCell(
                            day = dayNumber,
                            count = count,
                            isToday = isToday,
                            isSelected = isSelected,
                            hasLucid = byDay[date]?.any { it.lucid } == true,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(date) },
                        )
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    count: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasLucid: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val background = when {
        count <= 0 -> HeatLevel0
        count == 1 -> HeatLevel1
        count == 2 -> HeatLevel2
        else -> HeatLevel3
    }
    Box(
        modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    isSelected -> LucidText
                    isToday -> LucidAmber.copy(alpha = 0.7f)
                    else -> LucidBorder
                },
                shape = RoundedCornerShape(9.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (count >= 2) LucidText else LucidTextVariant,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
        )
        if (hasLucid) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(LucidAmber),
            )
        }
    }
}

@Composable
private fun CalendarNoteRow(note: DreamNote, settings: AppSettings, onClick: () -> Unit) {
    LucidCard(padding = PaddingValues(13.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LucidText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    note.displayTime(settings.use24Hour) +
                        if (note.hasAudio) "  ·  voice memo" else "  ·  written",
                    style = MaterialTheme.typography.labelSmall,
                    color = LucidTextSubtle,
                )
            }
            if (note.hasAudio) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = LucidPrimary, modifier = Modifier.size(16.dp))
            }
            if (note.lucid) {
                Spacer(Modifier.size(8.dp))
                StatusPill("Lucid", LucidAmber)
            }
        }
    }
}
