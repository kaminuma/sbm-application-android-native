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