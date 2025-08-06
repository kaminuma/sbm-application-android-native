package com.sbm.application.domain.model

data class User(
    val id: String,      // userId → id に変更、型もStringに
    val username: String,
    val email: String
)