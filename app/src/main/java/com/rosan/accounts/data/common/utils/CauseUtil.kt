package com.rosan.accounts.data.common.utils

import android.content.Context
import com.rosan.accounts.R
import com.rosan.accounts.data.common.cause.ShizukuNotWorkException

fun Throwable.help(): String? {
    val context = defaultKoin().get<Context>()
    return when (this) {
        is ShizukuNotWorkException -> context.getString(R.string.shizuku_not_working)
        else -> null
    }
}