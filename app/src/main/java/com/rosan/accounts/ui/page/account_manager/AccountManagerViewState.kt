package com.rosan.accounts.ui.page.account_manager

import com.rosan.accounts.data.service.entity.AccountAuthenticatorEntity
import com.rosan.accounts.data.service.entity.AccountEntity

data class AccountManagerViewState(
    val authenticators: List<Authenticator> = emptyList()
) {
    data class Authenticator(
        val auth: AccountAuthenticatorEntity,
        val accounts: List<AccountEntity>
    )
}