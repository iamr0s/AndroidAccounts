package com.rosan.accounts.ui.page.user_manager

import com.rosan.accounts.data.service.entity.UserEntity

sealed class UserManagerViewAction {
    object Load : UserManagerViewAction()

    data class Remove(val user: UserEntity) : UserManagerViewAction()
}