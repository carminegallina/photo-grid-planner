package com.photogridplanner.ui.media

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.generatedMediaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "photo_grid_generated_media",
)

private val GeneratedMediaKey = stringPreferencesKey("items")
private const val MaxRememberedItems = 500

data class GeneratedMediaRecord(
    val uri: Uri,
    val createdAt: Long,
)

/** Keeps locally-created cuts visible when Android grants only partial media access. */
object GeneratedMediaRegistry {
    suspend fun remember(context: Context, uris: Collection<Uri>) {
        if (uris.isEmpty()) return
        val now = System.currentTimeMillis()
        context.generatedMediaDataStore.edit { preferences ->
            val existing = decode(preferences[GeneratedMediaKey])
            val newRecords = uris.map { uri -> GeneratedMediaRecord(uri = uri, createdAt = now) }
            val records = (newRecords + existing)
                .distinctBy { it.uri }
                .sortedByDescending { it.createdAt }
                .take(MaxRememberedItems)
            preferences[GeneratedMediaKey] = encode(records)
        }
    }

    suspend fun read(context: Context): List<GeneratedMediaRecord> {
        return decode(context.generatedMediaDataStore.data.first()[GeneratedMediaKey])
    }

    private fun encode(records: List<GeneratedMediaRecord>): String {
        return JSONArray().apply {
            records.forEach { record ->
                put(
                    JSONObject()
                        .put("uri", record.uri.toString())
                        .put("createdAt", record.createdAt),
                )
            }
        }.toString()
    }

    private fun decode(raw: String?): List<GeneratedMediaRecord> {
        return runCatching {
            val array = JSONArray(raw.orEmpty())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val value = item.optString("uri")
                    if (value.isNotBlank()) {
                        add(
                            GeneratedMediaRecord(
                                uri = Uri.parse(value),
                                createdAt = item.optLong("createdAt", 0L),
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
}
