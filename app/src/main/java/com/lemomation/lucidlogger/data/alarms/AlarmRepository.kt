package com.lemomation.lucidlogger.data.alarms

import android.content.Context
import com.lemomation.lucidlogger.data.model.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class AlarmRepository(private val context: Context) {
    private val file = File(context.filesDir, "alarms.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val mutex = Mutex()

    private val _alarms = MutableStateFlow(readFromDisk())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    private fun readFromDisk(): List<Alarm> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(Alarm.serializer()), file.readText())
        }.getOrDefault(emptyList())
    }

    fun snapshot(): List<Alarm> = readFromDisk()

    fun get(id: String): Alarm? = snapshot().firstOrNull { it.id == id }

    suspend fun upsert(alarm: Alarm) {
        mutex.withLock {
            val current = _alarms.value.toMutableList()
            val index = current.indexOfFirst { it.id == alarm.id }
            if (index >= 0) current[index] = alarm else current.add(alarm)
            persist(current)
        }
    }

    suspend fun delete(id: String) {
        mutex.withLock {
            persist(_alarms.value.filterNot { it.id == id })
        }
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        mutex.withLock {
            persist(_alarms.value.map { if (it.id == id) it.copy(enabled = enabled) else it })
        }
    }

    suspend fun ensureDefault(): Alarm {
        val existing = _alarms.value
        if (existing.isNotEmpty()) return existing.first()
        val alarm = Alarm.default()
        upsert(alarm)
        return alarm
    }

    suspend fun replaceAll(items: List<Alarm>) {
        mutex.withLock { persist(items) }
    }

    private suspend fun persist(items: List<Alarm>) = withContext(Dispatchers.IO) {
        val text = json.encodeToString(ListSerializer(Alarm.serializer()), items)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        if (file.exists() && !file.delete()) {
            file.writeText(text)
            temp.delete()
        } else if (!temp.renameTo(file)) {
            file.writeText(text)
            temp.delete()
        }
        _alarms.value = items.sortedWith(compareBy({ it.hour }, { it.minute }))
    }
}
