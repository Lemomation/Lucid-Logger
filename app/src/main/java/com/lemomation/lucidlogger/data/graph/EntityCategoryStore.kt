package com.lemomation.lucidlogger.data.graph

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

enum class EntityCategory(val label: String) {
    LOCATION("Locations"),
    DREAM_SIGN("Dream Signs"),
    PERSON("People"),
    EMOTION("Emotions"),
}

class EntityCategoryStore(context: Context) {
    private val file = File(context.filesDir, "dreams/entities.json")
    private val json = Json { prettyPrint = true }
    private val _categories = MutableStateFlow(read())
    val categories: StateFlow<Map<String, EntityCategory>> = _categories.asStateFlow()

    private fun read(): Map<String, EntityCategory> {
        if (!file.exists()) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, String>>(file.readText())
                .mapNotNull { (key, value) ->
                    runCatching { EntityCategory.valueOf(value) }.getOrNull()?.let { key.lowercase(Locale.US) to it }
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    fun categoryFor(name: String): EntityCategory {
        val key = name.lowercase(Locale.US).trim()
        _categories.value[key]?.let { return it }
        return infer(name)
    }

    suspend fun setCategory(name: String, category: EntityCategory) {
        withContext(Dispatchers.IO) {
            val next = _categories.value + (name.lowercase(Locale.US).trim() to category)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(next.mapValues { it.value.name }))
            _categories.value = next
        }
    }

    private fun infer(name: String): EntityCategory {
        val n = name.lowercase(Locale.US)
        return when {
            LOCATION_WORDS.any { n.contains(it) } -> EntityCategory.LOCATION
            PERSON_WORDS.any { n.contains(it) } -> EntityCategory.PERSON
            EMOTION_WORDS.any { n.contains(it) } -> EntityCategory.EMOTION
            else -> EntityCategory.DREAM_SIGN
        }
    }

    private companion object {
        val LOCATION_WORDS = listOf(
            "ocean", "sea", "house", "home", "city", "street", "train", "station",
            "forest", "mountain", "island", "school", "room", "garden", "beach",
            "river", "lake", "tower", "castle", "library", "temple", "cave",
            "subway", "airport", "hotel", "hospital", "office", "park", "desert",
        )
        val PERSON_WORDS = listOf(
            "mother", "father", "mom", "dad", "sister", "brother", "friend", "mentor",
            "teacher", "stranger", "child", "girl", "boy", "woman", "man", "grand",
            "doctor", "guide", "partner", "lover", "family", "crowd",
        )
        val EMOTION_WORDS = listOf(
            "fear", "joy", "anxiety", "calm", "love", "grief", "panic", "wonder",
            "sadness", "anger", "peace", "awe", "loneliness", "hope", "shame",
            "storm", "shadow", "light", "darkness", "bliss", "terror",
        )
    }
}
