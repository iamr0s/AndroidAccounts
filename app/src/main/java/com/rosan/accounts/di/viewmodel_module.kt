package com.rosan.accounts.di

import com.rosan.accounts.ui.page.account_manager.AccountManagerViewModel
import com.rosan.accounts.ui.page.user_manager.UserManagerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        UserManagerViewModel()
    }

    viewModel {
        AccountManagerViewModel(get())
    }
}