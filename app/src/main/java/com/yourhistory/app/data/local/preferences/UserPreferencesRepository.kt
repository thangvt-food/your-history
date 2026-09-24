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
    }

    val defaultBankBin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_BANK_BIN]
    }

    suspend fun setDefaultBankBin(bin: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_BANK_BIN] = bin
        }
    }
}
