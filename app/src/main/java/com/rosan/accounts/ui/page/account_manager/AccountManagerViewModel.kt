package com.rosan.accounts.ui.page.account_manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.accounts.data.common.utils.replace
import com.rosan.accounts.data.service.repo.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AccountManagerViewModel(
    private val userId: Int
) : ViewModel(), KoinComponent {
    private val jobs = mutableMapOf<String, Job>()

    private val userService by inject<UserService>()

    var state by mutableStateOf(AccountManagerViewState())
        private set

    fun dispatch(action: AccountManagerViewAction) {
        when (action) {
            AccountManagerViewAction.Load -> load()
        }
    }

    private fun load() {
        jobs.replace("load") {
            it?.cancel()

            viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    val auths = userService.getAccountAuthenticators(userId)
                    val accounts = userService.getAccounts(userId)
                    state = state.copy(
                        authenticators = auths.map { auth ->
                            AccountManagerViewState.Authenticator(
                                auth = auth,
                                accounts = accounts.filter { auth.type == it.type }
                            )
                        }.filter { it.accounts.isNotEmpty() }
                    )
                    delay(3000)
                }
            }
        }
    }
}