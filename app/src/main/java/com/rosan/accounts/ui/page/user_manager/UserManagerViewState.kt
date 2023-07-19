package com.rosan.accounts.ui.page.user_manager

import com.rosan.accounts.data.service.entity.UserEntity

data class UserManagerViewState(
    val users: List<UserEntity> = emptyList(),
    val cause: Throwable? = null
)