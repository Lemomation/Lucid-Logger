package com.lemomation.lucidlogger

import android.app.Application
import android.content.Context
import android.net.Uri
import com.lemomation.lucidlogger.alarm.AlarmScheduler
import com.lemomation.lucidlogger.alarm.MorningReminderScheduler
import com.lemomation.lucidlogger.data.alarms.AlarmRepository
import com.lemomation.lucidlogger.data.export.ExportManager
import com.lemomation.lucidlogger.data.graph.EntityCategoryStore
import com.lemomation.lucidlogger.data.prefs.SettingsRepository
import com.lemomation.lucidlogger.data.store.DreamRepository
import com.lemomation.lucidlogger.data.store.DreamStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val alarms = AlarmRepository(context)
    val store = DreamStore(context)
    val dreams = DreamRepository(store, settings)
    val scheduler = AlarmScheduler(context)
    val entityCategories = EntityCategoryStore(context)
    val exportManager = ExportManager(context, store)

    init {
        dreams.autoExporter = { note ->
            val current = settings.current()
            val uri = current.exportUri
            if (current.autoExport && !uri.isNullOrBlank()) {
                exportManager.exportNote(Uri.parse(uri), note)
            }
        }
        dreams.autoDeleteExporter = { note ->
            val current = settings.current()
            val uri = current.exportUri
            if (current.autoExport && !uri.isNullOrBlank()) {
                exportManager.removeExported(Uri.parse(uri), note)
            }
        }
    }
}

fun Context.appContainer(): AppContainer = (applicationContext as LucidLoggerApp).container

class LucidLoggerApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.dreams.refresh()
            val settings = container.settings.current()
            MorningReminderScheduler(this@LucidLoggerApp).schedule(settings)
            container.scheduler.scheduleAll(container.alarms.snapshot())
        }
    }
}
