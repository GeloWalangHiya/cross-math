package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.engine.Difficulty
import com.example.engine.EvaluationMode
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.crossMathDataStore: DataStore<Preferences> by preferencesDataStore(name = "crossmath_prefs")

data class UserPreferences(
    val isMuted: Boolean = false,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val evaluationMode: EvaluationMode = EvaluationMode.PEMDAS,
    val allowNegatives: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val completedDailyDate: String = "",
    val totalSolved: Int = 0
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val EVALUATION_MODE = stringPreferencesKey("evaluation_mode")
        val ALLOW_NEGATIVES = booleanPreferencesKey("allow_negatives")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COMPLETED_DAILY_DATE = stringPreferencesKey("completed_daily_date")
        val TOTAL_SOLVED = intPreferencesKey("total_solved")
    }

    val preferencesFlow: Flow<UserPreferences> = context.crossMathDataStore.data.map { prefs ->
        UserPreferences(
            isMuted = prefs[Keys.IS_MUTED] ?: false,
            difficulty = prefs[Keys.DIFFICULTY]?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                ?: Difficulty.MEDIUM,
            evaluationMode = prefs[Keys.EVALUATION_MODE]?.let { runCatching { EvaluationMode.valueOf(it) }.getOrNull() }
                ?: EvaluationMode.PEMDAS,
            allowNegatives = prefs[Keys.ALLOW_NEGATIVES] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            completedDailyDate = prefs[Keys.COMPLETED_DAILY_DATE] ?: "",
            totalSolved = prefs[Keys.TOTAL_SOLVED] ?: 0
        )
    }

    suspend fun setMuted(muted: Boolean) {
        context.crossMathDataStore.edit { it[Keys.IS_MUTED] = muted }
    }

    suspend fun setDifficulty(difficulty: Difficulty) {
        context.crossMathDataStore.edit { it[Keys.DIFFICULTY] = difficulty.name }
    }

    suspend fun setEvaluationMode(mode: EvaluationMode) {
        context.crossMathDataStore.edit { it[Keys.EVALUATION_MODE] = mode.name }
    }

    suspend fun setAllowNegatives(allow: Boolean) {
        context.crossMathDataStore.edit { it[Keys.ALLOW_NEGATIVES] = allow }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.crossMathDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun recordPuzzleSolved(dailyDateCompleted: String?) {
        context.crossMathDataStore.edit { prefs ->
            prefs[Keys.TOTAL_SOLVED] = (prefs[Keys.TOTAL_SOLVED] ?: 0) + 1
            if (!dailyDateCompleted.isNullOrBlank()) {
                prefs[Keys.COMPLETED_DAILY_DATE] = dailyDateCompleted
            }
        }
    }
}
