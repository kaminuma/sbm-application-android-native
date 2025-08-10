package com.sbm.application.data.repository

import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.*
import com.sbm.application.domain.model.*
import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response

class ProxyAIAnalysisRepositoryImplTest {

    private lateinit var apiService: ApiService
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: ProxyAIAnalysisRepositoryImpl

    // テスト用のダミーデータ
    private val testToken = "test-jwt-token"
    private val testUserId = "12345"
    private val testRequest = AIAnalysisRequest(
        startDate = "2024-08-03",
        endDate = "2024-08-10",
        activities = emptyList(),
        moodRecords = emptyList()
    )
    private val testConfig = AIAnalysisConfig(
        analysisFocus = com.sbm.application.domain.model.AnalysisFocus.BALANCED,
        detailLevel = com.sbm.application.domain.model.DetailLevel.STANDARD,
        responseStyle = com.sbm.application.domain.model.ResponseStyle.FRIENDLY
    )

    @Before
    fun setup() {
        apiService = mock()
        authRepository = mock()
        repository = ProxyAIAnalysisRepositoryImpl(apiService, authRepository)
    }

    @Test
    fun `generateInsight_成功レスポンス_正しいAIInsightResponseを返す`() = runTest {
        // Given
        val successResponseDto = AIAnalysisResponseDto(
            success = true,
            error = null,
            data = AIInsightData(
                overallSummary = "テスト用サマリー",
                moodInsights = "テスト用気分分析",
                activityInsights = "テスト用活動分析", 
                recommendations = "推奨事項1\n推奨事項2\n推奨事項3"
            )
        )

        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(authRepository.getStoredUserId()).thenReturn(testUserId)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.success(successResponseDto))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は成功である必要があります", result.isSuccess)
        
        val response = result.getOrNull()
        assertNotNull("レスポンスはnullではない必要があります", response)
        assertTrue("レスポンスは成功である必要があります", response!!.success)
        
        val insight = response.data
        assertNotNull("インサイトデータはnullではない必要があります", insight)
        assertEquals("サマリーが正しく設定されている", "テスト用サマリー", insight!!.summary)
        assertEquals("気分分析が正しく設定されている", "テスト用気分分析", insight.moodAnalysis)
        assertEquals("活動分析が正しく設定されている", "テスト用活動分析", insight.activityAnalysis)
        assertEquals("推奨事項が正しく分割されている", 3, insight.recommendations.size)
        
        // APIが正しいパラメータで呼ばれたことを確認
        verify(apiService).generateAIAnalysis(
            eq("Bearer $testToken"),
            check { request ->
                assertEquals("2024-08-03", request.startDate)
                assertEquals("2024-08-10", request.endDate)
                assertEquals("BALANCED", request.analysisFocus)
                assertEquals("STANDARD", request.detailLevel)
                assertEquals("FRIENDLY", request.responseStyle)
            }
        )
    }

    @Test
    fun `generateInsight_認証トークンなし_適切なエラーを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(null)

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である必要があります", result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertTrue("エラーはAIAnalysisErrorである必要があります", 
                   exception is AIAnalysisError.ApiRequestError)
        assertTrue("エラーメッセージに認証情報が含まれる", 
                   exception!!.message!!.contains("認証情報"))
    }

    @Test
    fun `generateInsight_APIエラーレスポンス_適切なエラーを返す`() = runTest {
        // Given
        val errorResponseDto = AIAnalysisResponseDto(
            success = false,
            error = "データが不足しています",
            data = null
        )

        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.success(errorResponseDto))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は成功である必要があります（エラーレスポンスでも）", result.isSuccess)
        
        val response = result.getOrNull()
        assertNotNull("レスポンスはnullではない必要があります", response)
        assertFalse("レスポンスは失敗である必要があります", response!!.success)
        assertEquals("エラーメッセージが正しく設定されている", "データが不足しています", response.error)
        assertNull("データはnullである必要があります", response.data)
    }

    @Test
    fun `generateInsight_HTTP401エラー_InvalidApiKeyエラーを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(401, mock()))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である必要があります", result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertTrue("エラーはInvalidApiKeyである必要があります", 
                   exception is AIAnalysisError.InvalidApiKey)
    }

    @Test
    fun `generateInsight_HTTP429エラー_RateLimitExceededエラーを返す`() = runTest {
        // Given  
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(429, mock()))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である必要があります", result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertTrue("エラーはRateLimitExceededである必要があります",
                   exception is AIAnalysisError.RateLimitExceeded)
    }

    @Test
    fun `generateInsight_HTTP500エラー_NetworkErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(500, mock()))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である必要があります", result.isFailure)
        
        val exception = result.exceptionOrNull()
        assertTrue("エラーはNetworkErrorである必要があります",
                   exception is AIAnalysisError.NetworkError)
    }

    @Test
    fun `isConfigured_認証トークンあり_trueを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)

        // When
        val result = repository.isConfigured()

        // Then
        assertTrue("設定済みである必要があります", result)
    }

    @Test
    fun `isConfigured_認証トークンなし_falseを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(null)

        // When
        val result = repository.isConfigured()

        // Then
        assertFalse("未設定である必要があります", result)
    }

    @Test
    fun `getConfigurationStatus_認証トークンあり_設定済みステータスを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)

        // When
        val status = repository.getConfigurationStatus()

        // Then
        assertTrue("設定済みステータスである必要があります", status.isConfigured)
        assertEquals("プロバイダーがbackend-apiである", "backend-api", status.provider)
    }

    @Test
    fun `getConfigurationStatus_認証トークンなし_未設定ステータスを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn("")

        // When
        val status = repository.getConfigurationStatus()

        // Then
        assertFalse("未設定ステータスである必要があります", status.isConfigured)
        assertNotNull("エラーメッセージが設定されている", status.errorMessage)
    }

    @Test
    fun `parseRecommendations_改行区切りの推奨事項_正しくリストに分割される`() = runTest {
        // Given
        val recommendations = "・推奨事項1\n2. 推奨事項2\n* 推奨事項3\n推奨事項4"
        val repository = ProxyAIAnalysisRepositoryImpl(apiService, authRepository)

        // Private methodをテストするため、Reflectionを使用
        val method = repository.javaClass.getDeclaredMethod("parseRecommendations", String::class.java)
        method.isAccessible = true

        // When
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(repository, recommendations) as List<String>

        // Then
        assertEquals("4つの推奨事項が分割される", 4, result.size)
        assertEquals("記号が除去されている", "推奨事項1", result[0])
        assertEquals("番号が除去されている", "推奨事項2", result[1])
        assertEquals("アスタリスクが除去されている", "推奨事項3", result[2])
        assertEquals("そのまま保持されている", "推奨事項4", result[3])
    }
}