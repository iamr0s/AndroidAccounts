package com.rosan.accounts.data.common.utils

import org.koin.core.Koin
import org.koin.mp.KoinPlatformTools

fun defaultKoin(): Koin = KoinPlatformTools.defaultContext().get()