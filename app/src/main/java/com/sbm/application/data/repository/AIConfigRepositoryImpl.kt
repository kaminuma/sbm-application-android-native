package com.sbm.application.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sbm.application.domain.model.AIAnalysisConfig
import com.sbm.application.domain.model.AnalysisFocus
import com.sbm.application.domain.model.DetailLevel
import com.sbm.application.domain.model.ResponseStyle
import com.sbm.application.domain.repository.AIConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AIConfigRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun getConfig(): AIAnalysisConfig = withContext(Dispatchers.IO) {
        val focusName = prefs.getString(KEY_FOCUS, AnalysisFocus.BALANCED.name)!!
        val detailName = prefs.getString(KEY_DETAIL, DetailLevel.STANDARD.name)!!
        val styleName = prefs.getString(KEY_STYLE, ResponseStyle.FRIENDLY.name)!!

        AIAnalysisConfig(
            analysisFocus = runCatching { AnalysisFocus.valueOf(focusName) }.getOrDefault(AnalysisFocus.BALANCED),
            detailLevel = runCatching { DetailLevel.valueOf(detailName) }.getOrDefault(DetailLevel.STANDARD),
            responseStyle = runCatching { ResponseStyle.valueOf(styleName) }.getOrDefault(ResponseStyle.FRIENDLY)
        )
    }

    override suspend fun saveConfig(config: AIAnalysisConfig) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_FOCUS, config.analysisFocus.name)
            .putString(KEY_DETAIL, config.detailLevel.name)
            .putString(KEY_STYLE, config.responseStyle.name)
            .apply()
    }

    override suspend fun clearConfig() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    override suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        saveConfig(AIAnalysisConfig())
    }

    companion object {
        private const val PREFS_NAME = "ai_config_prefs"
        private const val KEY_FOCUS = "analysis_focus"
        private const val KEY_DETAIL = "detail_level"
        private const val KEY_STYLE = "response_style"
    }
}
