package com.sbm.application.data.repository

import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.AIAnalysisRequestDto
import com.sbm.application.data.remote.dto.AIAnalysisResponseDto
import com.sbm.application.domain.model.*
import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * ProxyAIAnalysisRepositoryImplのエラーハンドリング専用テストクラス
 * 様々なエラーシナリオをカバーして堅牢性を確認する
 */
class ProxyAIAnalysisErrorHandlingTest {

    private lateinit var apiService: ApiService
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: ProxyAIAnalysisRepositoryImpl

    private val testToken = "test-jwt-token"
    private val testRequest = AIAnalysisRequest(
        startDate = "2024-08-03",
        endDate = "2024-08-10",
        activities = emptyList(),
        moodRecords = emptyList()
    )
    private val testConfig = AIAnalysisConfig()

    @Before
    fun setup() {
        apiService = mock()
        authRepository = mock()
        repository = ProxyAIAnalysisRepositoryImpl(apiService, authRepository)
    }

    // === 認証関連エラー ===

    @Test
    fun `generateInsight_nullToken_ApiRequestErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(null)

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("ApiRequestErrorが返される", exception is AIAnalysisError.ApiRequestError)
        assertTrue("認証エラーメッセージが含まれる", 
                   exception!!.message!!.contains("認証情報"))
    }

    @Test
    fun `generateInsight_emptyToken_ApiRequestErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn("")

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("ApiRequestErrorが返される", exception is AIAnalysisError.ApiRequestError)
    }

    // === HTTPエラーレスポンス ===

    @Test
    fun `generateInsight_HTTP400_ApiRequestErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Bad Request".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(400, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("ApiRequestErrorが返される", exception is AIAnalysisError.ApiRequestError)
        assertTrue("リクエスト形式エラーが含まれる", 
                   exception!!.message!!.contains("リクエスト形式"))
    }

    @Test
    fun `generateInsight_HTTP401_InvalidApiKeyを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(401, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("InvalidApiKeyが返される", exception is AIAnalysisError.InvalidApiKey)
    }

    @Test
    fun `generateInsight_HTTP403_ApiRequestErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Forbidden".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(403, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("ApiRequestErrorが返される", exception is AIAnalysisError.ApiRequestError)
        assertTrue("権限エラーが含まれる", 
                   exception!!.message!!.contains("権限"))
    }

    @Test
    fun `generateInsight_HTTP404_ApiRequestErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Not Found".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(404, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("ApiRequestErrorが返される", exception is AIAnalysisError.ApiRequestError)
        assertTrue("データが見つからないエラーが含まれる", 
                   exception!!.message!!.contains("データが見つかりません"))
    }

    @Test
    fun `generateInsight_HTTP429_RateLimitExceededを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Too Many Requests".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(429, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("RateLimitExceededが返される", exception is AIAnalysisError.RateLimitExceeded)
    }

    @Test
    fun `generateInsight_HTTP500_NetworkErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Internal Server Error".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(500, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("NetworkErrorが返される", exception is AIAnalysisError.NetworkError)
        assertTrue("サーバーエラーメッセージが含まれる",
                   exception!!.message!!.contains("サーバーに一時的な問題"))
    }

    @Test
    fun `generateInsight_HTTP502_NetworkErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Bad Gateway".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(502, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("NetworkErrorが返される", exception is AIAnalysisError.NetworkError)
    }

    @Test
    fun `generateInsight_HTTP503_NetworkErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Service Unavailable".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(503, errorBody))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("NetworkErrorが返される", exception is AIAnalysisError.NetworkError)
    }

    // === ネットワーク例外 ===

    @Test
    fun `generateInsight_UnknownHostException_NetworkErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenThrow(UnknownHostException("Unable to resolve host"))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("NetworkErrorが返される", exception is AIAnalysisError.NetworkError)
        assertTrue("ネットワーク接続エラーメッセージが含まれる",
                   exception!!.message!!.contains("インターネット接続"))
    }

    @Test
    fun `generateInsight_SocketTimeoutException_NetworkErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenThrow(SocketTimeoutException("timeout"))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("NetworkErrorが返される", exception is AIAnalysisError.NetworkError)
        assertTrue("タイムアウトメッセージが含まれる",
                   exception!!.message!!.contains("タイムアウト"))
    }

    @Test
    fun `generateInsight_IOException_NetworkErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenThrow(IOException("Network error"))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("NetworkErrorが返される", exception is AIAnalysisError.NetworkError)
        assertTrue("ネットワークエラーメッセージが含まれる",
                   exception!!.message!!.contains("ネットワークエラー"))
    }

    @Test
    fun `generateInsight_HttpException_適切なエラーを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorResponse = Response.error<AIAnalysisResponseDto>(
            401, 
            "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull())
        )
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenThrow(HttpException(errorResponse))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("InvalidApiKeyが返される", exception is AIAnalysisError.InvalidApiKey)
    }

    // === 予期しない例外 ===

    @Test
    fun `generateInsight_RuntimeException_UnknownErrorを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenThrow(RuntimeException("Unexpected error"))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("適切なエラータイプが返される", exception is AIAnalysisError)
    }

    @Test
    fun `generateInsight_NullPointerException_適切なエラーを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenThrow(NullPointerException("Null pointer"))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("適切なエラータイプが返される", exception is AIAnalysisError)
    }

    // === 不正なレスポンスデータ ===

    @Test
    fun `generateInsight_nullResponseBody_適切なエラーを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.success(null))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
    }

    @Test
    fun `generateInsight_successFalseWithNullData_適切なAIInsightResponseを返す`() = runTest {
        // Given
        val errorResponseDto = AIAnalysisResponseDto(
            success = false,
            error = "データ不足エラー",
            data = null
        )
        
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.success(errorResponseDto))

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は成功である（エラーレスポンスも正常な処理）", result.isSuccess)
        val response = result.getOrNull()
        assertNotNull("レスポンスはnullではない", response)
        assertFalse("レスポンスは失敗を示す", response!!.success)
        assertEquals("エラーメッセージが正しく設定される", "データ不足エラー", response.error)
        assertNull("データはnull", response.data)
    }

    // === 設定・認証状態テスト ===

    @Test
    fun `isConfigured_emptyToken_falseを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn("")

        // When
        val result = repository.isConfigured()

        // Then
        assertFalse("空トークンでは未設定", result)
    }

    @Test
    fun `getConfigurationStatus_emptyToken_未設定ステータスを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn("")

        // When
        val status = repository.getConfigurationStatus()

        // Then
        assertFalse("未設定ステータス", status.isConfigured)
        assertNotNull("エラーメッセージが設定される", status.errorMessage)
        assertTrue("認証情報エラーメッセージ", status.errorMessage!!.contains("認証情報"))
    }

    // === エッジケース ===

    @Test
    fun `generateInsight_未知のHTTPステータスコード_適切なエラーを返す`() = runTest {
        // Given
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        val errorBody = "Unknown Error".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.error(418, errorBody)) // I'm a teapot

        // When
        val result = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("結果は失敗である", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("ApiRequestErrorが返される", exception is AIAnalysisError.ApiRequestError)
        assertTrue("ステータスコードが含まれる", exception!!.message!!.contains("418"))
    }

    @Test
    fun `generateInsight_同時実行_各呼び出しが独立してエラーハンドリングされる`() = runTest {
        // Given - 1つ目の呼び出しは成功、2つ目は失敗
        whenever(authRepository.getStoredToken()).thenReturn(testToken)
        
        val successResponse = AIAnalysisResponseDto(
            success = true,
            error = null,
            data = mock()
        )
        
        whenever(apiService.generateAIAnalysis(any(), any()))
            .thenReturn(Response.success(successResponse))
            .thenThrow(UnknownHostException("Network error"))

        // When
        val result1 = repository.generateInsight(testRequest, testConfig)
        val result2 = repository.generateInsight(testRequest, testConfig)

        // Then
        assertTrue("1つ目は成功", result1.isSuccess)
        assertTrue("2つ目は失敗", result2.isFailure)
    }
}