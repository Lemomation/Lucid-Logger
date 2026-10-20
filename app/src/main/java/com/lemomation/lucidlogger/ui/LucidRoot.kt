package com.lemomation.lucidlogger.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lemomation.lucidlogger.AppContainer
import com.lemomation.lucidlogger.data.model.AppSettings
import com.lemomation.lucidlogger.ui.alarm.AlarmScreen
import com.lemomation.lucidlogger.ui.calendar.CalendarScreen
import com.lemomation.lucidlogger.ui.components.LucidBottomBar
import com.lemomation.lucidlogger.ui.graph.GraphScreen
import com.lemomation.lucidlogger.ui.journal.DreamEditorScreen
import com.lemomation.lucidlogger.ui.journal.JournalScreen
import com.lemomation.lucidlogger.ui.settings.SettingsScreen
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun LucidRoot(container: AppContainer) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val notes by container.dreams.notes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current

    var route by rememberSaveable { mutableStateOf("alarm") }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var autoRecord by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { container.dreams.refresh() }
    }

    DisposableEffect(settings.keepScreenOn) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            if (settings.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose { }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * settings.textScale),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LucidCanvas),
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (route) {
                        "journal" -> JournalScreen(
                            notes = notes,
                            settings = settings,
                            onOpenNote = { note ->
                                editingId = note.id
                                autoRecord = false
                                editorOpen = true
                            },
                            onNewDream = { startRecording ->
                                editingId = null
                                autoRecord = startRecording
                                editorOpen = true
                            },
                            onOpenSettings = { route = "settings" },
                        )

                        "graph" -> GraphScreen(
                            container = container,
                            notes = notes,
                            onOpenNote = { note ->
                                editingId = note.id
                                autoRecord = false
                                editorOpen = true
                            },
                        )

                        "calendar" -> CalendarScreen(
                            notes = notes,
                            settings = settings,
                            onOpenNote = { note ->
                                editingId = note.id
                                autoRecord = false
                                editorOpen = true
                            },
                        )

                        "settings" -> SettingsScreen(
                            container = container,
                            settings = settings,
                            notesCount = notes.size,
                            onOpenAlarm = { route = "alarm" },
                        )

                        else -> AlarmScreen(
                            onOpenSettings = { route = "settings" },
                            onQuickRecord = {
                                route = "journal"
                                editingId = null
                                autoRecord = true
                                editorOpen = true
                            },
                        )
                    }
                }
                LucidBottomBar(
                    selectedRoute = route,
                    onSelect = { selected ->
                        if (!editorOpen) route = selected
                    },
                )
            }

            if (editorOpen) {
                DreamEditorScreen(
                    container = container,
                    existing = editingId?.let { id -> notes.firstOrNull { it.id == id } },
                    autoStartRecording = autoRecord,
                    settings = settings,
                    onClose = {
                        editorOpen = false
                        editingId = null
                        autoRecord = false
                    },
                )
            }
        }
    }
}
