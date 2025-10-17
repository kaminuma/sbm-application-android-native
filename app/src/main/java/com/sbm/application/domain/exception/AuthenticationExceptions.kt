package com.sbm.application.domain.exception

sealed class AuthenticationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AccountLockedException(
    message: String,
    val lockoutTimeRemaining: Long? = null
) : AuthenticationException(message)

class BadCredentialsException(
    message: String,
    val remainingAttempts: Int? = null
) : AuthenticationException(message)

class AuthenticationFailedException(message: String) : AuthenticationException(message)

/**
 * リフレッシュトークンが期限切れまたは無効
 */
class RefreshTokenExpiredException(message: String) : AuthenticationException(message)

/**
 * 一時的なネットワークエラーやサーバーエラー
 */
class TemporaryAuthException(message: String, cause: Throwable? = null) : AuthenticationException(message, cause)