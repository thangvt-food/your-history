package com.yourhistory.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_DEFAULT_BANK_BIN = stringPreferencesKey("default_bank_bin")
        val KEY_DEFAULT_CATEGORY_ID = stringPreferencesKey("default_category_id")
        val KEY_MEMO_TAGS = stringPreferencesKey("memo_tags")
        private const val TAGS_SEPARATOR = "|||"
    }

    val defaultBankBin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_BANK_BIN]
    }

    suspend fun setDefaultBankBin(bin: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_BANK_BIN] = bin
        }
    }

    val customMemoTags: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_MEMO_TAGS].orEmpty()
        if (raw.isBlank()) emptyList()
        else raw.split(TAGS_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun addMemoTag(tag: String) {
        val clean = tag.trim()
        if (clean.isEmpty()) return
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_MEMO_TAGS].orEmpty()
                .split(TAGS_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
                .toMutableList()
            if (!current.any { it.equals(clean, ignoreCase = true) }) {
                current.add(clean)
                preferences[KEY_MEMO_TAGS] = current.joinToString(TAGS_SEPARATOR)
            }
        }
    }

    suspend fun removeMemoTag(tag: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_MEMO_TAGS].orEmpty()
                .split(TAGS_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
                .filterNot { it.equals(tag.trim(), ignoreCase = true) }
            preferences[KEY_MEMO_TAGS] = current.joinToString(TAGS_SEPARATOR)
        }
    }
}
