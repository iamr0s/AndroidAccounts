package com.rosan.accounts.di

import com.rosan.accounts.data.service.model.ShizukuUserService
import com.rosan.accounts.data.service.repo.UserService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val serviceModule = module {
    single<UserService> {
        ShizukuUserService(androidContext())
    }
}