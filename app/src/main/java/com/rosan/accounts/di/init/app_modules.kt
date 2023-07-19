package com.rosan.accounts.di.init

import com.rosan.accounts.di.serviceModule
import com.rosan.accounts.di.viewModelModule

val appModules = listOf(
    viewModelModule,
    serviceModule
)