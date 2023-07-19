package com.rosan.accounts.ui.page.user_manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.accounts.data.common.utils.replace
import com.rosan.accounts.data.service.entity.UserEntity
import com.rosan.accounts.data.service.repo.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserManagerViewModel : ViewModel(), KoinComponent {
    private val jobs = mutableMapOf<String, Job>()

    private val userService by inject<UserService>()

    var state by mutableStateOf(emptyList<UserEntity>())
        private set

    fun dispatch(action: UserManagerViewAction) {
        when (action) {
            UserManagerViewAction.Load -> load()
            is UserManagerViewAction.Remove -> remove(action.user)
        }
    }

    private fun load() {
        jobs.replace("load") {
            it?.cancel()

            viewModelScope.launch(Dispatchers.IO) {
                while (true) {
                    state = userService.getUsers().sortedBy { it.id }
                    delay(1500)
                }
            }
        }
    }

    private fun remove(user: UserEntity) {
        viewModelScope.launch {
            userService.removeUser(user)
        }
    }
}