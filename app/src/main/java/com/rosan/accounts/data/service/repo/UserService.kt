package com.rosan.accounts.data.service.repo

import com.rosan.accounts.data.service.entity.AccountAuthenticatorEntity
import com.rosan.accounts.data.service.entity.AccountEntity
import com.rosan.accounts.data.service.entity.UserEntity

interface UserService {
    suspend fun removeUser(user: UserEntity): Boolean

    suspend fun removeUser(userId: Int): Boolean

    suspend fun getUsers(): List<UserEntity>

    suspend fun getAccountAuthenticators(userId: Int): List<AccountAuthenticatorEntity>

    suspend fun getAccounts(userId: Int): List<AccountEntity>
}
